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

import org.bukkit.event.inventory.ClickType;
import java.util.*;

public class ForgeGUI {
    private static final int GUI_SIZE = 27;

    private final YinwuForgePlugin plugin;
    private final MaterialConfig materialConfig;
    private final ForgeManager forgeManager;
    private final AltarManager altarManager;
    private final ConfigManager configManager;

    private final Map<UUID, Inventory> openGUIs = new HashMap<>();

    private final List<ItemStack> equipPreviews = new ArrayList<>();
    private final List<ItemStack> corePreviews = new ArrayList<>();
    private final List<ItemStack> adjusterPreviews = new ArrayList<>();
    private int rotationIndex;

    private static final int PREVIEW_SLOT_EQUIP = 9;
    private static final int PREVIEW_SLOT_CORE = 10;
    private static final int PREVIEW_SLOT_ADJUSTER = 11;

    public ForgeGUI(YinwuForgePlugin plugin, MaterialConfig materialConfig,
                    ForgeManager forgeManager, AltarManager altarManager,
                    ConfigManager configManager) {
        this.plugin = plugin;
        this.materialConfig = materialConfig;
        this.forgeManager = forgeManager;
        this.altarManager = altarManager;
        this.configManager = configManager;
        buildPreviewLists();
        startPreviewRotation();
    }

    private void buildPreviewLists() {
        equipPreviews.clear();
        for (Material mat : new Material[]{
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
            Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
            Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
            Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL,
            Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE,
            Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE,
            Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET,
            Material.GOLDEN_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET,
            Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE,
            Material.GOLDEN_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE,
            Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS,
            Material.GOLDEN_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS,
            Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS,
            Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS,
            Material.ELYTRA, Material.TRIDENT, Material.MACE,
            Material.BOW, Material.CROSSBOW
        }) {
            ItemStack item = new ItemStack(mat);
            equipPreviews.add(item);
        }

        corePreviews.clear();
        for (MaterialConfig.ConcentratedMat cm : materialConfig.getAllConcentrated()) {
            ItemStack sample = cm.createItem(1);
            if (materialConfig.isStrengthMaterial(sample)) {
                corePreviews.add(sample);
            }
        }

        adjusterPreviews.clear();
        for (MaterialConfig.ConcentratedMat cm : materialConfig.getAllConcentrated()) {
            ItemStack sample = cm.createItem(1);
            if (materialConfig.isAdjusterMaterial(sample)) {
                adjusterPreviews.add(sample);
            }
        }
    }

    private void setPreviewItem(Inventory inv, int slot, List<ItemStack> previews) {
        if (previews.isEmpty()) return;
        int idx = (rotationIndex + slot * 7) % previews.size();
        inv.setItem(slot, previews.get(idx).clone());
    }

    private void refreshPlayerPreview(Player player) {
        rotationIndex++;
        Inventory inv = openGUIs.get(player.getUniqueId());
        if (inv == null) return;
        int equipSlot = materialConfig.getSlotEquipment();
        int coreSlot = materialConfig.getSlotCore();
        int adjusterSlot = materialConfig.getSlotAdjuster();
        ItemStack equip = inv.getItem(equipSlot);
        ItemStack core = inv.getItem(coreSlot);
        ItemStack adjuster = inv.getItem(adjusterSlot);
        if (equip == null || equip.getType() == Material.AIR) {
            setPreviewItem(inv, PREVIEW_SLOT_EQUIP, equipPreviews);
        }
        if (core == null || core.getType() == Material.AIR) {
            setPreviewItem(inv, PREVIEW_SLOT_CORE, corePreviews);
        }
        if (adjuster == null || adjuster.getType() == Material.AIR) {
            setPreviewItem(inv, PREVIEW_SLOT_ADJUSTER, adjusterPreviews);
        }
        refreshRateDisplay(player);
    }

    private void startPreviewRotation() {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (openGUIs.containsKey(p.getUniqueId())) {
                    p.getScheduler().run(plugin, (t) -> refreshPlayerPreview(p), null);
                }
            }
        }, 20L, 80L);
    }

    public void openForgeGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, materialConfig.getGuiTitle());
        initGUI(inv);
        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), inv);
    }

    private void initGUI(Inventory inv) {
        rotationIndex++;
        int equipSlot = materialConfig.getSlotEquipment();
        int coreSlot = materialConfig.getSlotCore();
        int adjusterSlot = materialConfig.getSlotAdjuster();
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < GUI_SIZE; i++) {
            if (i == equipSlot || i == coreSlot || i == adjusterSlot) {
                continue;
            }
            inv.setItem(i, border);
        }

        setPreviewItem(inv, PREVIEW_SLOT_EQUIP, equipPreviews);
        setPreviewItem(inv, PREVIEW_SLOT_CORE, corePreviews);
        setPreviewItem(inv, PREVIEW_SLOT_ADJUSTER, adjusterPreviews);

        inv.setItem(materialConfig.getSlotForge(), createForgeButton());

        updateRateDisplay(inv, null);
    }

    private void updateRateDisplay(Inventory inv, String adjusterCategory) {
        updateRateDisplay(inv, adjusterCategory, 0, 0);
    }

    private void updateRateDisplay(Inventory inv, String adjusterCategory, double altarSuccessBonus, double altarFailReduction) {
        double baseFail = configManager.getAlloyForgeChance("fail-no-penalty");
        double baseDestroy = configManager.getAlloyForgeChance("equipment-destroyed");
        double baseDowngrade = configManager.getAlloyForgeChance("downgrade");
        double baseSuccess = configManager.getAlloyForgeChance("success");
        double basePerfect = configManager.getAlloyForgeChance("perfect");

        double success = baseSuccess;
        double perfect = basePerfect;
        double failNoPenalty = baseFail;
        double destroy = baseDestroy;
        double downgrade = baseDowngrade;

        if (adjusterCategory != null) {
            MaterialConfig.CategoryAdjuster ca = materialConfig.getCategoryAdjuster(adjusterCategory);
            if (ca != null) {
                double netSuccess = ca.successBonus - ca.successPenalty;
                double netDestroy = ca.destroyReduction - ca.destroyIncrease;

                double grabFrom = failNoPenalty + destroy + downgrade;
                if (grabFrom > 0 && netSuccess != 0) {
                    double actual = Math.min(Math.abs(netSuccess), grabFrom) * (netSuccess > 0 ? 1 : -1);
                    double ratio = actual / grabFrom;
                    failNoPenalty += failNoPenalty * ratio;
                    destroy += destroy * ratio;
                    downgrade += downgrade * ratio;
                    double sp = success + perfect;
                    if (sp > 0) {
                        success += actual * (success / sp);
                        perfect += actual * (perfect / sp);
                    } else {
                        success += actual * 0.7;
                        perfect += actual * 0.3;
                    }
                }

                double totalBad = destroy + downgrade;
                if (totalBad > 0 && netDestroy != 0) {
                    double adj = Math.min(Math.abs(netDestroy), totalBad) * (netDestroy > 0 ? 1 : -1);
                    double dr = destroy / totalBad;
                    double dnr = downgrade / totalBad;
                    destroy -= adj * dr;
                    downgrade -= adj * dnr;
                    failNoPenalty += adj;
                }
            }
        }

        // 应用祭坛加成（与 performForge 中的逻辑一致）
        if (altarSuccessBonus != 0) {
            double grabFrom = failNoPenalty + destroy + downgrade;
            if (grabFrom > 0) {
                double actual = Math.min(Math.abs(altarSuccessBonus), grabFrom) * (altarSuccessBonus > 0 ? 1 : -1);
                double ratio = actual / grabFrom;
                failNoPenalty += failNoPenalty * ratio;
                destroy += destroy * ratio;
                downgrade += downgrade * ratio;
                double sp = success + perfect;
                if (sp > 0) {
                    success += actual * (success / sp);
                    perfect += actual * (perfect / sp);
                } else {
                    success += actual * 0.7;
                    perfect += actual * 0.3;
                }
            }
        }
        if (altarFailReduction != 0) {
            double totalBad = destroy + downgrade;
            if (totalBad > 0) {
                double adj = Math.min(Math.abs(altarFailReduction), totalBad) * (altarFailReduction > 0 ? 1 : -1);
                double dr = destroy / totalBad;
                double dnr = downgrade / totalBad;
                destroy -= adj * dr;
                downgrade -= adj * dnr;
                failNoPenalty += adj;
            }
        }

        double[] clamped = new double[]{failNoPenalty, destroy, downgrade, success, perfect};
        for (int i = 0; i < clamped.length; i++) {
            clamped[i] = Math.max(0, Math.min(100, clamped[i]));
        }
        double sum = 0;
        for (double v : clamped) sum += v;
        if (sum > 0) {
            for (int i = 0; i < clamped.length; i++) {
                clamped[i] = clamped[i] / sum * 100;
            }
        }

        int successInt = (int) Math.round(clamped[3]);
        int perfectInt = (int) Math.round(clamped[4]);
        int failInt = (int) Math.round(clamped[0]);
        int destroyInt = (int) Math.round(clamped[1]);
        int downgradeInt = (int) Math.round(clamped[2]);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GREEN + "成功: " + successInt + "%  |  极品: " + perfectInt + "%");
        lore.add(ChatColor.RED + "无惩罚: " + failInt + "%  |  摧毁: " + destroyInt + "%  |  降级: " + downgradeInt + "%");
        if (adjusterCategory != null) {
            String adjName = switch (adjusterCategory) {
                case "nether" -> "炼狱";
                case "end" -> "末地";
                case "challenge" -> "挑战";
                default -> adjusterCategory;
            };
            lore.add(ChatColor.LIGHT_PURPLE + "调整核心: " + adjName);
        }

        ItemStack rateItem = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = rateItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "锻造概率");
            meta.setLore(lore);
            rateItem.setItemMeta(meta);
        }

        inv.setItem(4, rateItem);
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
        Inventory inv = openGUIs.get(player.getUniqueId());
        if (inv == null) return;

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
                event.setCancelled(true);
                rotationIndex++;
                ItemStack slotItem = event.getCurrentItem();
                ItemStack cursorItem = event.getCursor();
                ClickType click = event.getClick();

                boolean slotHasItem = slotItem != null && slotItem.getType() != Material.AIR;
                boolean cursorHasItem = cursorItem != null && cursorItem.getType() != Material.AIR;

                if (slotHasItem && cursorHasItem) {
                    inv.setItem(slot, cursorItem.clone());
                    event.getView().setCursor(slotItem.clone());
                    refreshRateDisplay(player);
                    return;
                }

                if (slotHasItem) {
                    if (click == ClickType.RIGHT) {
                        int take = (slotItem.getAmount() + 1) / 2;
                        ItemStack taken = slotItem.clone();
                        taken.setAmount(take);
                        slotItem.setAmount(slotItem.getAmount() - take);
                        if (slotItem.getAmount() <= 0) {
                            inv.setItem(slot, null);
                        } else {
                            inv.setItem(slot, slotItem);
                        }
                        event.getView().setCursor(taken);
                    } else if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
                        Map<Integer, ItemStack> leftover = player.getInventory().addItem(slotItem.clone());
                        for (ItemStack left : leftover.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), left);
                        }
                        inv.setItem(slot, null);
                    } else {
                        event.getView().setCursor(slotItem.clone());
                        inv.setItem(slot, null);
                    }
                } else if (cursorHasItem) {
                    if (click == ClickType.RIGHT) {
                        ItemStack one = cursorItem.clone();
                        one.setAmount(1);
                        inv.setItem(slot, one);
                        cursorItem.setAmount(cursorItem.getAmount() - 1);
                        event.getView().setCursor(cursorItem.getAmount() > 0 ? cursorItem : null);
                    } else {
                        inv.setItem(slot, cursorItem.clone());
                        event.getView().setCursor(null);
                    }
                }

                refreshRateDisplay(player);
                return;
            }

            event.setCancelled(true);
        } else {
            ClickType click = event.getClick();
            if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null && clicked.getType() != Material.AIR) {
                    int equipSlot = materialConfig.getSlotEquipment();
                    int coreSlot = materialConfig.getSlotCore();
                    int adjusterSlot = materialConfig.getSlotAdjuster();
                    for (int target : new int[]{equipSlot, coreSlot, adjusterSlot}) {
                        ItemStack existing = inv.getItem(target);
                        if (existing == null || existing.getType() == Material.AIR) {
                            event.setCancelled(true);
                            rotationIndex++;
                            inv.setItem(target, clicked.clone());
                            int raw = event.getRawSlot();
                            if (raw >= GUI_SIZE) {
                                int pSlot = raw - GUI_SIZE;
                                if (pSlot < player.getInventory().getSize()) {
                                    player.getInventory().setItem(pSlot, null);
                                }
                            }
                            player.getScheduler().runDelayed(plugin, (task) -> {
                                refreshRateDisplay(player);
                            }, null, 1L);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void refreshRateDisplay(Player player) {
        Inventory inv = openGUIs.get(player.getUniqueId());
        if (inv == null) return;

        int equipSlot = materialConfig.getSlotEquipment();
        int coreSlot = materialConfig.getSlotCore();
        int adjusterSlot = materialConfig.getSlotAdjuster();
        ItemStack equip = inv.getItem(equipSlot);
        ItemStack core = inv.getItem(coreSlot);
        ItemStack adj = inv.getItem(adjusterSlot);
        if (equip == null || equip.getType() == Material.AIR) {
            setPreviewItem(inv, PREVIEW_SLOT_EQUIP, equipPreviews);
        }
        if (core == null || core.getType() == Material.AIR) {
            setPreviewItem(inv, PREVIEW_SLOT_CORE, corePreviews);
        }
        if (adj == null || adj.getType() == Material.AIR) {
            setPreviewItem(inv, PREVIEW_SLOT_ADJUSTER, adjusterPreviews);
        }

        ItemStack adjuster = inv.getItem(materialConfig.getSlotAdjuster());
        String category = null;
        if (adjuster != null && adjuster.getType() != Material.AIR) {
            String cat = materialConfig.getCategory(adjuster);
            if (cat != null && materialConfig.getCategoryAdjuster(cat) != null) {
                category = cat;
            }
        }

        double altarSuccess = 0;
        double altarFail = 0;
        if (altarManager != null) {
            altarSuccess = altarManager.getPlayerSuccessBonus(player);
            altarFail = altarManager.getPlayerFailReduction(player);
        }
        updateRateDisplay(inv, category, altarSuccess, altarFail);
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

        ItemStack equipmentClone = equipment.clone();
        inv.setItem(materialConfig.getSlotEquipment(), null);
        inv.setItem(materialConfig.getSlotCore(), consumeOne(strengthMat));
        inv.setItem(materialConfig.getSlotAdjuster(), adjusterMat != null && adjusterMat.getType() != Material.AIR ? consumeOne(adjusterMat) : null);
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
            ForgeResult result = forgeManager.executeCategoryForge(player, equipmentClone, null,
                adjusterMat, finalAdjCategory, finalSuccessBonus, finalDestroyReduction);

            if (result != null && altarManager != null) {
                altarManager.playForgeEffects(player, result);
            }

            if (result != ForgeResult.EQUIPMENT_DESTROYED) {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(equipmentClone);
                for (ItemStack left : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), left);
                }
            }

            refreshGUI(inv);
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
            for (ItemStack left : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
        }
    }

    private ItemStack consumeOne(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        if (item.getAmount() <= 1) return null;
        item.setAmount(item.getAmount() - 1);
        return item;
    }

    public boolean isOpenGUI(Player player) {
        return openGUIs.containsKey(player.getUniqueId());
    }

    public void removePlayer(Player player) {
        closeGUI(player);
    }

    public void closeAllGUIs() {
        for (Map.Entry<UUID, Inventory> entry : openGUIs.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                Inventory inv = entry.getValue();
                returnItems(player, inv.getItem(materialConfig.getSlotEquipment()));
                returnItems(player, inv.getItem(materialConfig.getSlotCore()));
                returnItems(player, inv.getItem(materialConfig.getSlotAdjuster()));
            }
        }
        openGUIs.clear();
    }
}
