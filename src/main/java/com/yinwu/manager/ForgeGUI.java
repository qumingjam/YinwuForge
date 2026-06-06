package com.yinwu.manager;

import com.yinwu.YinwuForgePlugin;
import com.yinwu.model.ForgeResult;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ForgeGUI {
    private static final int GUI_SIZE = 27;

    private final YinwuForgePlugin plugin;
    private final MaterialConfig materialConfig;
    private final ForgeManager forgeManager;
    private final AltarManager altarManager;
    private final ConfigManager configManager;

    private final Map<UUID, Inventory> openGUIs = new HashMap<>();

    public ForgeGUI(YinwuForgePlugin plugin, MaterialConfig materialConfig,
                    ForgeManager forgeManager, AltarManager altarManager,
                    ConfigManager configManager) {
        this.plugin = plugin;
        this.materialConfig = materialConfig;
        this.forgeManager = forgeManager;
        this.altarManager = altarManager;
        this.configManager = configManager;
    }

    public void openForgeGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, materialConfig.getGuiTitle());
        initGUI(inv);
        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), inv);
    }

    private void initGUI(Inventory inv) {
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        ItemStack info = createItem(Material.BOOK, ChatColor.YELLOW + "锻造指南",
            ChatColor.GRAY + "第1格: 放入要锻造的装备",
            ChatColor.GRAY + "第2格: 放入强化材料（矿物→盔甲 / 亡灵→武器 / 农牧→工具）",
            ChatColor.GRAY + "第3格: 放入概率调整材料（可选：炼狱/末地/挑战）",
            ChatColor.GRAY + "中间按钮: 点击开始锻造");

        for (int i = 0; i < GUI_SIZE; i++) {
            if (i == materialConfig.getSlotEquipment()) continue;
            if (i == materialConfig.getSlotCore()) continue;
            if (i == materialConfig.getSlotAdjuster()) continue;
            if (i == materialConfig.getSlotForge()) {
                inv.setItem(i, createForgeButton());
                continue;
            }
            inv.setItem(i, border);
        }

        inv.setItem(4, info);
    }

    private ItemStack createForgeButton() {
        ItemStack button = new ItemStack(Material.ANVIL);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "⛏ 开始锻造");
            meta.setLore(Collections.singletonList(ChatColor.GRAY + "点击开始锻造"));
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openGUIs.containsKey(player.getUniqueId())) return;
        if (!event.getView().getTitle().equals(materialConfig.getGuiTitle())) return;

        int slot = event.getRawSlot();
        if (slot >= 0 && slot < GUI_SIZE) {
            int equipSlot = materialConfig.getSlotEquipment();
            int coreSlot = materialConfig.getSlotCore();
            int adjusterSlot = materialConfig.getSlotAdjuster();
            int forgeSlot = materialConfig.getSlotForge();

            if (slot == forgeSlot) {
                event.setCancelled(true);
                performForge(player);
                return;
            }

            if (slot == equipSlot || slot == coreSlot || slot == adjusterSlot) {
                if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR
                    && event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                    event.setCancelled(true);
                    return;
                }
                return;
            }

            event.setCancelled(true);
        }
    }

    private void performForge(Player player) {
        Inventory inv = openGUIs.get(player.getUniqueId());
        if (inv == null) return;

        ItemStack equipment = inv.getItem(materialConfig.getSlotEquipment());
        ItemStack strengthMat = inv.getItem(materialConfig.getSlotCore());
        ItemStack adjusterMat = inv.getItem(materialConfig.getSlotAdjuster());

        if (equipment == null || equipment.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "请在第1格放入要锻造的装备！");
            return;
        }

        if (strengthMat == null || strengthMat.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "请在第2格放入强化材料！");
            player.sendMessage(ChatColor.GRAY + "矿物→盔甲 | 亡灵→武器 | 农牧→工具");
            return;
        }

        // 第2格必须放入强化类材料（mineral/undead/farming）
        if (!materialConfig.isConcentratedMaterial(strengthMat) || !materialConfig.isStrengthMaterial(strengthMat)) {
            player.sendMessage(ChatColor.RED + "第2格必须放入强化材料（矿物/亡灵/农牧系列）！");
            return;
        }

        String category = materialConfig.getCategory(strengthMat);
        String expectedEquipType = materialConfig.getCategoryEquipmentType(category);

        if (!forgeManager.isEquipmentTypeMatch(equipment, expectedEquipType)) {
            String typeName = switch (expectedEquipType) {
                case "armor" -> "盔甲";
                case "weapon" -> "武器";
                case "tool" -> "工具";
                default -> expectedEquipType;
            };
            player.sendMessage(ChatColor.RED + "该材料只能用于强化" + typeName + "！");
            return;
        }

        // 处理第3格概率调整材料（可选）
        double adjustSuccessBonus = 0;
        double adjustDestroyBonus = 0;
        double adjustDestroyReduction = 0;
        double adjustSuccessPenalty = 0;
        String adjusterCategory = null;

        if (adjusterMat != null && adjusterMat.getType() != Material.AIR) {
            if (!materialConfig.isConcentratedMaterial(adjusterMat) || !materialConfig.isAdjusterMaterial(adjusterMat)) {
                player.sendMessage(ChatColor.RED + "第3格必须放入概率调整材料（炼狱/末地/挑战系列）！");
                return;
            }

            String adjCategory = materialConfig.getCategory(adjusterMat);
            MaterialConfig.CategoryAdjuster ca = materialConfig.getCategoryAdjuster(adjCategory);
            if (ca != null) {
                if ("challenge".equals(adjCategory)) {
                    int forgeCount = forgeManager.getEquipmentForgeCount(equipment);
                    if (forgeCount < ca.minLevel) {
                        player.sendMessage(ChatColor.RED + "挑战类材料需要锻造次数 ≥ " + ca.minLevel + " 才能使用！");
                        player.sendMessage(ChatColor.GRAY + "当前锻造次数: " + forgeCount);
                        return;
                    }
                }
                adjusterCategory = adjCategory;
                adjustSuccessBonus = ca.successBonus;
                adjustDestroyBonus = ca.destroyIncrease;
                adjustDestroyReduction = ca.destroyReduction;
                adjustSuccessPenalty = ca.successPenalty;
            }
        }
        final String finalAdjCategory = adjusterCategory;

        inv.setItem(materialConfig.getSlotEquipment(), null);
        inv.setItem(materialConfig.getSlotCore(), null);
        inv.setItem(materialConfig.getSlotAdjuster(), null);
        refreshGUI(inv);

        double netSuccessBonus = adjustSuccessBonus - adjustSuccessPenalty;
        double netDestroyReduction = adjustDestroyReduction - adjustDestroyBonus;

        if (altarManager != null) {
            netSuccessBonus += altarManager.getPlayerSuccessBonus(player);
            netDestroyReduction += altarManager.getPlayerFailReduction(player);
        }

        final double finalSuccessBonus = netSuccessBonus;
        final double finalDestroyReduction = netDestroyReduction;

        player.getScheduler().run(plugin, (task) -> {
            ForgeResult result = forgeManager.executeCategoryForge(player, equipment, strengthMat,
                adjusterMat, finalAdjCategory, finalSuccessBonus, finalDestroyReduction);

            if (result != null && result != ForgeResult.EQUIPMENT_DESTROYED) {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(equipment);
                if (!leftover.isEmpty()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), equipment);
                }
            }

            closeGUI(player);
            forgeManager.setCooldown(player);
        }, null);
    }

    private void refreshGUI(Inventory inv) {
        initGUI(inv);
    }

    public void closeGUI(Player player) {
        Inventory inv = openGUIs.remove(player.getUniqueId());
        if (inv != null) {
            returnItems(player, inv.getItem(materialConfig.getSlotEquipment()));
            returnItems(player, inv.getItem(materialConfig.getSlotCore()));
            returnItems(player, inv.getItem(materialConfig.getSlotAdjuster()));
            player.closeInventory();
        }
    }

    private void returnItems(Player player, ItemStack item) {
        if (item != null && item.getType() != Material.AIR) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            if (!leftover.isEmpty()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }
    }

    public boolean isOpenGUI(Player player) {
        return openGUIs.containsKey(player.getUniqueId());
    }

    public void removePlayer(Player player) {
        closeGUI(player);
    }
}
