package com.yinwu.manager;

import com.yinwu.YinwuForgePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MaterialConfig {
    private final YinwuForgePlugin plugin;
    private final ConfigManager configManager;

    private final Map<String, ConcentratedMat> concentratedMaterials = new ConcurrentHashMap<>();
    private final Map<String, String> categoryEquipmentMap = new ConcurrentHashMap<>();
    private final Map<String, CategoryAdjuster> adjusterConfigs = new ConcurrentHashMap<>();

    private String guiTitle;
    private int slotEquipment;
    private int slotCore;
    private int slotAdjuster;
    private int slotForge;

    public static class ConcentratedMat {
        public final String id;
        public final Material material;
        public final String customName;
        public final String lore;
        public final String category;
        public final String function;

        public ConcentratedMat(String id, Material material, String customName, String lore, String category, String function) {
            this.id = id;
            this.material = material;
            this.customName = customName;
            this.lore = lore;
            this.category = category;
            this.function = function;
        }

        public boolean matches(ItemStack item) {
            if (item == null || item.getType() != material) return false;
            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) return false;
            return meta.getDisplayName().equals(customName);
        }

        public ItemStack createItem(int amount) {
            ItemStack stack = new ItemStack(material, amount);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(customName);
                List<String> loreList = new ArrayList<>();
                loreList.add(ChatColor.GRAY + lore);
                meta.setLore(loreList);
                stack.setItemMeta(meta);
            }
            return stack;
        }
    }

    public static class CategoryAdjuster {
        public final double successBonus;
        public final double destroyIncrease;
        public final double failReduction;
        public final double successPenalty;
        public final double destroyReduction;
        public final int minLevel;

        public CategoryAdjuster(double successBonus, double destroyIncrease,
                                double failReduction, double successPenalty,
                                double destroyReduction, int minLevel) {
            this.successBonus = successBonus;
            this.destroyIncrease = destroyIncrease;
            this.failReduction = failReduction;
            this.successPenalty = successPenalty;
            this.destroyReduction = destroyReduction;
            this.minLevel = minLevel;
        }
    }

    public MaterialConfig(YinwuForgePlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        loadConfig();
    }

    public void loadConfig() {
        concentratedMaterials.clear();
        categoryEquipmentMap.clear();
        adjusterConfigs.clear();

        // === 从 material.yml 加载材料定义 ===
        File materialFile = new File(plugin.getDataFolder(), "material.yml");
        if (!materialFile.exists()) {
            plugin.saveResource("material.yml", false);
        }
        FileConfiguration matConfig = YamlConfiguration.loadConfiguration(materialFile);

        ConfigurationSection matSection = matConfig.getConfigurationSection("materials");
        if (matSection != null) {
            for (String key : matSection.getKeys(false)) {
                String matStr = matSection.getString(key + ".material");
                String name = matSection.getString(key + ".name");
                String lore = matSection.getString(key + ".lore", "");
                String category = matSection.getString(key + ".category", "");
                String function = matSection.getString(key + ".function", "");
                if (matStr != null && name != null && !category.isEmpty()) {
                    Material mat = Material.matchMaterial(matStr.toUpperCase());
                    if (mat != null) {
                        concentratedMaterials.put(key, new ConcentratedMat(key, mat, name, lore, category, function));
                    }
                }
            }
        }

        // === 从 config.yml 加载其他配置 ===
        FileConfiguration config = configManager.getRawConfig();

        ConfigurationSection equipMap = config.getConfigurationSection("category-equipment-map");
        if (equipMap != null) {
            for (String key : equipMap.getKeys(false)) {
                categoryEquipmentMap.put(key, equipMap.getString(key));
            }
        }

        ConfigurationSection adjusterSection = config.getConfigurationSection("adjuster-categories");
        if (adjusterSection != null) {
            for (String key : adjusterSection.getKeys(false)) {
                double successBonus = adjusterSection.getDouble(key + ".success-bonus", 0);
                double destroyIncrease = adjusterSection.getDouble(key + ".destroy-increase", 0);
                double failReduction = adjusterSection.getDouble(key + ".fail-reduction", 0);
                double successPenalty = adjusterSection.getDouble(key + ".success-penalty", 0);
                double destroyReduction = adjusterSection.getDouble(key + ".destroy-reduction", 0);
                int minLevel = adjusterSection.getInt(key + ".min-level", 0);
                adjusterConfigs.put(key, new CategoryAdjuster(successBonus, destroyIncrease,
                    failReduction, successPenalty, destroyReduction, minLevel));
            }
        }

        ConfigurationSection guiSection = config.getConfigurationSection("forge-gui");
        if (guiSection != null) {
            guiTitle = guiSection.getString("title", "§8Yinwu锻造");
            slotEquipment = guiSection.getInt("slot-equipment", 0);
            slotCore = guiSection.getInt("slot-core", 1);
            slotAdjuster = guiSection.getInt("slot-adjuster", 2);
            slotForge = guiSection.getInt("slot-forge", 13);
        }
    }

    public boolean isConcentratedMaterial(ItemStack item) {
        return getConcentratedMat(item) != null;
    }

    public ConcentratedMat getConcentratedMat(ItemStack item) {
        for (ConcentratedMat cm : concentratedMaterials.values()) {
            if (cm.matches(item)) return cm;
        }
        return null;
    }

    public String getConcentratedMatId(ItemStack item) {
        ConcentratedMat cm = getConcentratedMat(item);
        return cm != null ? cm.id : null;
    }

    public String getCategory(ItemStack item) {
        ConcentratedMat cm = getConcentratedMat(item);
        return cm != null ? cm.category : null;
    }

    public boolean isStrengthMaterial(ItemStack item) {
        String category = getCategory(item);
        return category != null && categoryEquipmentMap.containsKey(category);
    }

    public boolean isAdjusterMaterial(ItemStack item) {
        String category = getCategory(item);
        return category != null && adjusterConfigs.containsKey(category);
    }

    public boolean isPotionMaterial(ItemStack item) {
        String category = getCategory(item);
        return "potion".equals(category);
    }

    public String getCategoryEquipmentType(String category) {
        return categoryEquipmentMap.get(category);
    }

    public CategoryAdjuster getCategoryAdjuster(String category) {
        return adjusterConfigs.get(category);
    }

    public ItemStack createItem(String id, int amount) {
        ConcentratedMat cm = concentratedMaterials.get(id);
        if (cm == null) return null;
        return cm.createItem(amount);
    }

    public Collection<ConcentratedMat> getAllConcentrated() {
        return concentratedMaterials.values();
    }

    public Map<String, String> getCategoryEquipmentMap() {
        return categoryEquipmentMap;
    }

    public String getGuiTitle() {
        return guiTitle;
    }

    public int getSlotEquipment() {
        return slotEquipment;
    }

    public int getSlotCore() {
        return slotCore;
    }

    public int getSlotAdjuster() {
        return slotAdjuster;
    }

    public int getSlotForge() {
        return slotForge;
    }

    public void reload() {
        loadConfig();
    }
}
