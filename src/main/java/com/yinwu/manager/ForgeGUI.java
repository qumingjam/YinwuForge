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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ForgeGUI {
    private static final int GUI_SIZE = 27;

    private final YinwuForgePlugin plugin;
    private final MaterialConfig materialConfig;
    private final ForgeManager forgeManager;
    private final AltarManager altarManager;
    private final ConfigManager configManager;

    private final Map<UUID, Inventory> openGUIs = new ConcurrentHashMap<>();

    private final List<ItemStack> equipPreviews = new CopyOnWriteArrayList<>();
    private final List<ItemStack> corePreviews = new CopyOnWriteArrayList<>();
    private final List<ItemStack> adjusterPreviews = new CopyOnWriteArrayList<>();
    private int rotationIndex;

    private static final int PREVIEW_SLOT_EQUIP = 9;
    private static final int PREVIEW_SLOT_CORE = 10;
    private static final int PREVIEW_SLOT_ADJUSTER = 11;
    private static final int STATUS_SLOT = 16;

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
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GRAY + "（样本）");
                item.setItemMeta(meta);
            }
            equipPreviews.add(item);
        }

        corePreviews.clear();
        for (MaterialConfig.ConcentratedMat cm : materialConfig.getAllConcentrated()) {
            ItemStack sample = cm.createItem(1, materialConfig.getCategoryLabel(cm.category));
            if (materialConfig.isStrengthMaterial(sample)) {
                ItemMeta meta = sample.getItemMeta();
                if (meta != null) {
                    String name = meta.getDisplayName();
                    meta.setDisplayName(name + ChatColor.GRAY + " （样本）");
                    List<String> lore = meta.getLore();
                    if (lore == null) lore = new ArrayList<>();
                    lore.add(ChatColor.DARK_GRAY + "仅用作预览展示");
                    meta.setLore(lore);
                    sample.setItemMeta(meta);
                }
                corePreviews.add(sample);
            }
        }

        adjusterPreviews.clear();
        for (MaterialConfig.ConcentratedMat cm : materialConfig.getAllConcentrated()) {
            ItemStack sample = cm.createItem(1, materialConfig.getCategoryLabel(cm.category));
            if (materialConfig.isAdjusterMaterial(sample)) {
                ItemMeta meta = sample.getItemMeta();
                if (meta != null) {
                    String name = meta.getDisplayName();
                    meta.setDisplayName(name + ChatColor.GRAY + " （样本）");
                    List<String> lore = meta.getLore();
                    if (lore == null) lore = new ArrayList<>();
                    lore.add(ChatColor.DARK_GRAY + "仅用作预览展示");
                    meta.setLore(lore);
                    sample.setItemMeta(meta);
                }
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
        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> {
            for (UUID uuid : openGUIs.keySet()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
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
        setNeutralStatus(inv);

        updateRateDisplay(inv, null, 0, 0, 0);
    }

    private void setNeutralStatus(Inventory inv) {
        inv.setItem(STATUS_SLOT, createItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "提示信息"));
    }

    /** 在状态槽 + action bar 显示错误，聊天栏提示在 GUI 界面不显眼 */
    private void showError(Player player, Inventory inv, String... lines) {
        player.sendActionBar(ChatColor.RED + lines[0]);
        ItemStack status = createItem(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "⚠ " + lines[0]);
        if (lines.length > 1) {
            ItemMeta meta = status.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                for (int i = 1; i < lines.length; i++) {
                    lore.add(ChatColor.GRAY + lines[i]);
                }
                meta.setLore(lore);
                status.setItemMeta(meta);
            }
        }
        inv.setItem(STATUS_SLOT, status);
    }

    /** 锻造结果也显示在状态槽 + action bar */
    private void showResult(Player player, Inventory inv, ForgeResult result) {
        player.sendActionBar(result.getFullMessage());
        inv.setItem(STATUS_SLOT, createItem(Material.EXPERIENCE_BOTTLE, result.getFullMessage()));
    }

    /** 三个输入槽任一放错 → 整个 GUI 填充物变屏障方块，标注错误位置 */
    private void refreshValidationState(Player player, Inventory inv) {
        int equipSlot = materialConfig.getSlotEquipment();
        int coreSlot = materialConfig.getSlotCore();
        int adjusterSlot = materialConfig.getSlotAdjuster();
        int forgeSlot = materialConfig.getSlotForge();

        String error = null;
        ItemStack equip = inv.getItem(equipSlot);
        if (equip != null && equip.getType() != Material.AIR && !forgeManager.isForgeable(equip)) {
            error = "第1格：该物品无法锻造";
        }
        if (error == null) {
            ItemStack core = inv.getItem(coreSlot);
            if (core != null && core.getType() != Material.AIR
                && (!materialConfig.isConcentratedMaterial(core)
                    || (!materialConfig.isStrengthMaterial(core) && !materialConfig.isPotionMaterial(core)))) {
                error = "第2格：不是强化/药水材料";
            }
        }
        if (error == null) {
            ItemStack adj = inv.getItem(adjusterSlot);
            if (adj != null && adj.getType() != Material.AIR
                && (!materialConfig.isConcentratedMaterial(adj) || !materialConfig.isAdjusterMaterial(adj))) {
                error = "第3格：不是调整材料";
            }
        }

        if (error != null) {
            showError(player, inv, error, "物品放错，请更换后再锻造");
            ItemStack barrier = createItem(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + error);
            for (int i = 0; i < GUI_SIZE; i++) {
                if (i == equipSlot || i == coreSlot || i == adjusterSlot || i == forgeSlot
                    || i == PREVIEW_SLOT_EQUIP || i == PREVIEW_SLOT_CORE || i == PREVIEW_SLOT_ADJUSTER
                    || i == STATUS_SLOT || i == 4) {
                    continue;
                }
                inv.setItem(i, barrier);
            }
        } else {
            setNeutralStatus(inv);
            ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
            for (int i = 0; i < GUI_SIZE; i++) {
                if (i == equipSlot || i == coreSlot || i == adjusterSlot || i == forgeSlot
                    || i == PREVIEW_SLOT_EQUIP || i == PREVIEW_SLOT_CORE || i == PREVIEW_SLOT_ADJUSTER
                    || i == STATUS_SLOT || i == 4) {
                    continue;
                }
                inv.setItem(i, border);
            }
        }
    }

    private void updateRateDisplay(Inventory inv, String adjusterCategory) {
        updateRateDisplay(inv, adjusterCategory, 0.0, 0.0, 0.0);
    }

    /** 检测玩家主副手是否持有共振附魔盾牌 */
    private int getResonateLevel(Player player) {
        var enchant = forgeManager.getEnchantAPI();
        if (enchant == null) return 0;
        var inv = player.getInventory();
        for (var item : new ItemStack[]{inv.getItemInMainHand(), inv.getItemInOffHand()}) {
            if (item != null && !item.getType().isAir() && item.getType() == Material.SHIELD) {
                if (enchant.getEnchantmentLevel(item, "resonate") > 0) return 1;
            }
        }
        return 0;
    }

    /**
     * 获取 resonate 附魔的锻造加成：每级 +3% 成功率
     */
    static double resonateForgeBonus(int level) {
        return level * 3.0;
    }

    private void updateRateDisplay(Inventory inv, String adjusterCategory, double altarSuccessBonus,
                                    double altarFailReduction, double resonateBonus) {
        boolean potionForge = false;
        ItemStack core = inv.getItem(materialConfig.getSlotCore());
        if (core != null && core.getType() != Material.AIR) {
            potionForge = materialConfig.isPotionMaterial(core);
        }
        double baseFail = potionForge ? configManager.getPotionForgeChance("fail-no-penalty") : configManager.getAlloyForgeChance("fail-no-penalty");
        double baseDestroy = potionForge ? configManager.getPotionForgeChance("equipment-destroyed") : configManager.getAlloyForgeChance("equipment-destroyed");
        double baseDowngrade = potionForge ? configManager.getPotionForgeChance("downgrade") : configManager.getAlloyForgeChance("downgrade");
        double baseSuccess = potionForge ? configManager.getPotionForgeChance("success") : configManager.getAlloyForgeChance("success");
        double basePerfect = potionForge ? configManager.getPotionForgeChance("perfect") : configManager.getAlloyForgeChance("perfect");

        double netSuccessBonus = 0;
        double netDestroyReduction = 0;

        if (adjusterCategory != null) {
            MaterialConfig.CategoryAdjuster ca = materialConfig.getCategoryAdjuster(adjusterCategory);
            if (ca != null) {
                netSuccessBonus += ca.successBonus - ca.successPenalty;
                netDestroyReduction += ca.destroyReduction - ca.destroyIncrease;
            }
        }

        netSuccessBonus += altarSuccessBonus;
        netDestroyReduction += altarFailReduction;

        double[] probs = ForgeManager.calculateAdjustedProbs(
            baseFail, baseDestroy, baseDowngrade, baseSuccess, basePerfect,
            netSuccessBonus, netDestroyReduction);

        int failInt = (int) Math.round(probs[0]);
        int destroyInt = (int) Math.round(probs[1]);
        int downgradeInt = (int) Math.round(probs[2]);
        int successInt = (int) Math.round(probs[3]);
        int perfectInt = (int) Math.round(probs[4]);

        List<String> lore = new ArrayList<>();
        if (potionForge) {
            lore.add(ChatColor.LIGHT_PURPLE + "药水锻造：附加药水效果");
        }
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
        if (resonateBonus > 0) {
            lore.add(ChatColor.AQUA + "✦ 共振附魔: 成功率 +" + String.format("%.0f", resonateBonus) + "%");
        }

        ItemStack rateItem = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = rateItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + (potionForge ? "药水锻造概率" : "锻造概率"));
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
                    refreshValidationState(player, inv);
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

                refreshValidationState(player, inv);
                refreshRateDisplay(player);
                return;
            }

            event.setCancelled(true);
        } else {
            ClickType click = event.getClick();
            if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
                rotationIndex++;
                // shift 点击的物品转移在事件后才完成，延迟一拍再校验
                player.getScheduler().runDelayed(plugin, (task) -> {
                    refreshRateDisplay(player);
                    refreshValidationState(player, inv);
                }, null, 1L);
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

        // 共振附魔加成：每级 +3% 成功率
        int resonateLevel = getResonateLevel(player);
        double resonateBonus = resonateLevel > 0 ? resonateForgeBonus(resonateLevel) : 0;

        updateRateDisplay(inv, category, altarSuccess, altarFail, resonateBonus);
    }

    private void performForge(Player player) {
        Inventory inv = openGUIs.get(player.getUniqueId());
        if (inv == null) return;

        ItemStack equipment = inv.getItem(materialConfig.getSlotEquipment());
        ItemStack strengthMat = inv.getItem(materialConfig.getSlotCore());
        ItemStack adjusterMat = inv.getItem(materialConfig.getSlotAdjuster());

        if (equipment == null || equipment.getType() == Material.AIR) {
            showError(player, inv, "请在第1格放入要锻造的装备！");
            return;
        }

        // 灾厄强化后的物品禁止锻造
        if (isDisasterEnhanced(equipment)) {
            showError(player, inv, "灾厄强化后的物品无法锻造！");
            return;
        }

        if (strengthMat == null || strengthMat.getType() == Material.AIR) {
            showError(player, inv, "请在第2格放入强化材料！", "矿物→盔甲 | 亡灵→武器 | 农牧→工具");
            return;
        }

        if (!materialConfig.isConcentratedMaterial(strengthMat)) {
            showError(player, inv, "第2格必须放入浓缩材料！", "强化材料或药水锻造材料");
            return;
        }
        boolean potionForge = materialConfig.isPotionMaterial(strengthMat);
        if (!potionForge && !materialConfig.isStrengthMaterial(strengthMat)) {
            showError(player, inv, "第2格必须放入强化材料！", "强化材料：矿物/亡灵/农牧系列");
            return;
        }

        String category = materialConfig.getCategory(strengthMat);
        String expectedEquipType = materialConfig.getCategoryEquipmentType(category);

        if (!potionForge && !forgeManager.isEquipmentTypeMatch(equipment, expectedEquipType)) {
            String typeName = switch (expectedEquipType) {
                case "armor" -> "盔甲";
                case "weapon" -> "武器";
                case "tool" -> "工具";
                default -> expectedEquipType;
            };
            showError(player, inv, "该材料只能用于强化" + typeName + "！");
            return;
        }

        double adjustSuccessBonus = 0;
        double adjustDestroyBonus = 0;
        double adjustDestroyReduction = 0;
        double adjustSuccessPenalty = 0;
        String adjusterCategory = null;

        if (adjusterMat != null && adjusterMat.getType() != Material.AIR) {
            if (!materialConfig.isConcentratedMaterial(adjusterMat) || !materialConfig.isAdjusterMaterial(adjusterMat)) {
                showError(player, inv, "第3格必须放入概率调整材料！", "调整材料：炼狱/末地/挑战系列");
                return;
            }

            String adjCategory = materialConfig.getCategory(adjusterMat);
            MaterialConfig.CategoryAdjuster ca = materialConfig.getCategoryAdjuster(adjCategory);
            if (ca != null) {
                if ("challenge".equals(adjCategory)) {
                    int forgeCount = forgeManager.getEquipmentForgeCount(equipment);
                    if (forgeCount < ca.minLevel) {
                        showError(player, inv, "挑战类材料需要锻造次数 ≥ " + ca.minLevel + "！", "当前锻造次数: " + forgeCount);
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

        // 共振附魔加成：每级 +3% 成功率
        int resonateLevel = getResonateLevel(player);
        if (resonateLevel > 0) {
            netSuccessBonus += resonateForgeBonus(resonateLevel);
            player.sendMessage(ChatColor.AQUA + "✦ 共振附魔触发：锻造成功率 +" +
                String.format("%.0f", resonateForgeBonus(resonateLevel)) + "%");
        }

        final double finalSuccessBonus = netSuccessBonus;
        final double finalDestroyReduction = netDestroyReduction;

        player.getScheduler().run(plugin, (task) -> {
            ForgeResult result = potionForge
                ? forgeManager.executePotionForge(player, equipmentClone, finalSuccessBonus, finalDestroyReduction)
                : forgeManager.executeCategoryForge(player, equipmentClone, null,
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
            if (result != null) {
                showResult(player, inv, result);
            }
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

    /** 检查物品是否已被灾厄强化，此类物品禁止锻造和附魔 */
    private boolean isDisasterEnhanced(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        var key = org.bukkit.NamespacedKey.fromString("yinwuraid:disaster_enhanced");
        return key != null && item.getItemMeta().getPersistentDataContainer()
            .has(key, org.bukkit.persistence.PersistentDataType.BYTE);
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
