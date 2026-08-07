package com.yinwu.manager;

import com.yinwu.YinwuForgePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 浓缩材料一览 GUI（两级：类别菜单 → 材料列表，/yinwuforge gui 打开）
 */
public class MaterialGUI {
    private static final int MENU_SIZE = 27;
    private static final int LIST_SIZE = 54;
    private static final int PAGE_SIZE = 45;
    private static final String MENU_TITLE = "Yinwu浓缩材料";

    private static final int SLOT_BACK = 45;
    private static final int SLOT_PREV = 46;
    private static final int SLOT_PAGE_INFO = 49;
    private static final int SLOT_NEXT = 52;

    private final YinwuForgePlugin plugin;
    private final MaterialConfig materialConfig;
    private final Map<UUID, MaterialView> views = new ConcurrentHashMap<>();

    private record MaterialView(String level, String category, int page, Inventory inv) {}

    private record CategoryInfo(String key, String name, String shortName, Material icon, String desc) {}

    /** 5 个类别定义（图标物品 + 名称 + 描述） */
    private static final List<CategoryInfo> CATEGORIES = List.of(
        new CategoryInfo("weapon", "✦ 强化材料 · 武器", "武器", Material.DIAMOND_SWORD, "适用于武器（剑等）"),
        new CategoryInfo("tool", "✦ 强化材料 · 工具", "工具", Material.DIAMOND_PICKAXE, "适用于工具（镐/斧/锹/锄）"),
        new CategoryInfo("armor", "✦ 强化材料 · 盔甲", "盔甲", Material.DIAMOND_CHESTPLATE, "适用于盔甲"),
        new CategoryInfo("adjuster", "✦ 调整材料", "调整", Material.NETHER_STAR, "调整锻造成功率/销毁率"),
        new CategoryInfo("potion", "✦ 药水材料", "药水", Material.POTION, "药水锻造")
    );

    public MaterialGUI(YinwuForgePlugin plugin, MaterialConfig materialConfig) {
        this.plugin = plugin;
        this.materialConfig = materialConfig;
    }

    public void open(Player player) {
        player.getScheduler().run(plugin, (task) -> openMenu(player), null);
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        // 按标题锁定整个材料 GUI（图标不可拿走，含玩家背包格）
        if (!event.getView().getTitle().startsWith(MENU_TITLE)) return;
        event.setCancelled(true);

        MaterialView view = views.get(player.getUniqueId());
        if (view == null || event.getInventory() != view.inv()) return;

        int raw = event.getRawSlot();
        if ("menu".equals(view.level())) {
            // 一级：点击类别槽 → 二级
            for (int i = 0; i < CATEGORIES.size(); i++) {
                if (raw == 10 + i) {
                    openCategory(player, CATEGORIES.get(i).key());
                    return;
                }
            }
        } else {
            // 二级：返回 / 翻页
            if (raw == SLOT_BACK) {
                openMenu(player);
            } else if (raw == SLOT_PREV) {
                openCategoryPage(player, view.category(), view.page() - 1);
            } else if (raw == SLOT_NEXT) {
                openCategoryPage(player, view.category(), view.page() + 1);
            }
        }
    }

    public void removePlayer(Player player) {
        views.remove(player.getUniqueId());
    }

    /** 关窗时清理：仅当关闭的是当前材料 GUI，避免切换界面时误清会话 */
    public void removePlayer(Player player, Inventory inv) {
        MaterialView view = views.get(player.getUniqueId());
        if (view != null && view.inv() == inv) {
            views.remove(player.getUniqueId());
        }
    }

    public void closeAll() {
        for (UUID uuid : views.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) p.closeInventory();
        }
        views.clear();
    }

    // ===== 一级：类别菜单 =====
    private void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, MENU_SIZE, MENU_TITLE);
        for (int i = 0; i < MENU_SIZE; i++) inv.setItem(i, decor());
        for (int i = 0; i < CATEGORIES.size(); i++) {
            inv.setItem(10 + i, categoryItem(CATEGORIES.get(i)));
        }
        views.put(player.getUniqueId(), new MaterialView("menu", null, 0, inv));
        player.openInventory(inv);
    }

    private ItemStack categoryItem(CategoryInfo cat) {
        ItemStack item = new ItemStack(cat.icon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(cat.name());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + cat.desc());
            lore.add(ChatColor.YELLOW + "共 " + categoryMaterials(cat.key()).size() + " 种材料");
            lore.add(ChatColor.GRAY + "点击查看");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ===== 二级：材料列表 =====
    private void openCategory(Player player, String key) {
        openCategoryPage(player, key, 0);
    }

    private void openCategoryPage(Player player, String key, int page) {
        List<MaterialConfig.ConcentratedMat> mats = categoryMaterials(key);
        int totalPages = Math.max(1, (mats.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page, totalPages - 1));

        CategoryInfo cat = categoryInfo(key);
        Inventory inv = Bukkit.createInventory(null, LIST_SIZE, MENU_TITLE + " · " + cat.shortName());

        // 材料平铺（内容区 0-44）
        int start = page * PAGE_SIZE;
        int end = Math.min(mats.size(), start + PAGE_SIZE);
        int slot = 0;
        for (int i = start; i < end; i++) inv.setItem(slot++, createMaterialItem(mats.get(i)));
        for (int i = slot; i < PAGE_SIZE; i++) inv.setItem(i, decor());

        // 底部导航条
        for (int i = PAGE_SIZE; i < LIST_SIZE; i++) inv.setItem(i, decor());
        inv.setItem(SLOT_BACK, navButton(Material.ARROW, ChatColor.YELLOW + "◀ 返回"));
        inv.setItem(SLOT_PAGE_INFO, pageInfo(page, totalPages));
        if (page > 0) inv.setItem(SLOT_PREV, navButton(Material.ARROW, ChatColor.YELLOW + "上一页"));
        if (page < totalPages - 1) inv.setItem(SLOT_NEXT, navButton(Material.ARROW, ChatColor.YELLOW + "下一页"));

        views.put(player.getUniqueId(), new MaterialView("list", key, page, inv));
        player.openInventory(inv);
    }

    // ===== 辅助 =====
    private List<MaterialConfig.ConcentratedMat> categoryMaterials(String key) {
        List<MaterialConfig.ConcentratedMat> all = new ArrayList<>(materialConfig.getAllConcentrated());
        all.sort(Comparator.comparingInt(this::categoryOrder));
        List<MaterialConfig.ConcentratedMat> result = new ArrayList<>();
        for (MaterialConfig.ConcentratedMat cm : all) {
            if (categoryKey(cm).equals(key)) result.add(cm);
        }
        return result;
    }

    private CategoryInfo categoryInfo(String key) {
        for (CategoryInfo cat : CATEGORIES) {
            if (cat.key().equals(key)) return cat;
        }
        return CATEGORIES.get(0);
    }

    private ItemStack createMaterialItem(MaterialConfig.ConcentratedMat cm) {
        ItemStack stack = new ItemStack(cm.material, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(cm.customName);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + cm.lore);
            if (cm.function != null && !cm.function.isEmpty()) {
                lore.add(ChatColor.YELLOW + "功能: " + cm.function);
            }
            lore.add(materialConfig.getCategoryLabel(cm.category));
            lore.add(ChatColor.DARK_GRAY + "ID: " + cm.id);
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack decor() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack navButton(Material material, String name) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack pageInfo(int page, int totalPages) {
        ItemStack item = new ItemStack(Material.NAME_TAG, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "第 " + (page + 1) + " / " + totalPages + " 页");
            meta.setLore(List.of(ChatColor.GRAY + "点击返回/翻页"));
            item.setItemMeta(meta);
        }
        return item;
    }

    // ===== 类别判定（基于 MaterialConfig 动态分组） =====
    private int categoryOrder(MaterialConfig.ConcentratedMat cm) {
        return switch (categoryKey(cm)) {
            case "weapon" -> 0;
            case "tool" -> 1;
            case "armor" -> 2;
            case "adjuster" -> 3;
            default -> 4;
        };
    }

    private String categoryKey(MaterialConfig.ConcentratedMat cm) {
        String equip = materialConfig.getCategoryEquipmentType(cm.category);
        if (equip != null) {
            return switch (equip) {
                case "weapon" -> "weapon";
                case "tool" -> "tool";
                case "armor" -> "armor";
                default -> "adjuster";
            };
        }
        String label = ChatColor.stripColor(materialConfig.getCategoryLabel(cm.category));
        return label.contains("药水") ? "potion" : "adjuster";
    }
}
