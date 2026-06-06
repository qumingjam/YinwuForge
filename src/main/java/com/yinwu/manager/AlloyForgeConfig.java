package com.yinwu.manager;

import com.yinwu.YinwuForgePlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

/**
 * 合金锻造配置管理器
 */
public class AlloyForgeConfig {
    private final YinwuForgePlugin plugin;
    private final ConfigManager configManager;
    
    // 基础配置
    private boolean enabled;
    private String materialType;
    private String customName;  // 自定义显示名称
    
    // 属性修改范围
    private int downgradeMin;
    private int downgradeMax;
    private int successMin;
    private int successMax;
    private int perfectMin;
    private int perfectMax;
    
    // 装备类型与属性映射
    private final Map<String, List<String>> equipmentTypeAttributes = new HashMap<>();
    
    // 属性名称映射
    private final Map<String, String> attributeNames = new HashMap<>();
    
    public AlloyForgeConfig(YinwuForgePlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        loadConfig();
    }
    
    /**
     * 加载配置
     */
    public void loadConfig() {
        FileConfiguration config = configManager.getRawConfig();
        
        // 加载基础配置
        enabled = config.getBoolean("alloy-forge.enabled", true);
        materialType = config.getString("alloy-forge.material", "NETHERITE_INGOT");
        customName = config.getString("alloy-forge.custom-name", "");
        
        // 加载属性修改范围
        downgradeMin = config.getInt("alloy-forge.attributes.downgrade-range.min", 1);
        downgradeMax = config.getInt("alloy-forge.attributes.downgrade-range.max", 3);
        successMin = config.getInt("alloy-forge.attributes.success-range.min", 1);
        successMax = config.getInt("alloy-forge.attributes.success-range.max", 3);
        perfectMin = config.getInt("alloy-forge.attributes.perfect-range.min", 2);
        perfectMax = config.getInt("alloy-forge.attributes.perfect-range.max", 5);
        
        // 加载装备类型与属性映射
        equipmentTypeAttributes.clear();
        if (config.isConfigurationSection("alloy-forge.equipment-types")) {
            for (String type : config.getConfigurationSection("alloy-forge.equipment-types").getKeys(false)) {
                List<String> attrs = config.getStringList("alloy-forge.equipment-types." + type);
                equipmentTypeAttributes.put(type.toLowerCase(), attrs);
            }
        }
        
        // 加载属性名称映射
        attributeNames.clear();
        if (config.isConfigurationSection("alloy-forge.attribute-names")) {
            for (String key : config.getConfigurationSection("alloy-forge.attribute-names").getKeys(false)) {
                String name = config.getString("alloy-forge.attribute-names." + key);
                attributeNames.put(key, name != null ? name : key);
            }
        }
    }
    
    /**
     * 检查是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 获取材料类型
     */
    public String getMaterialType() {
        return materialType;
    }
    
    /**
     * 获取自定义显示名称（如果为空则不检查）
     */
    public String getCustomName() {
        return customName;
    }
    
    /**
     * 获取降级属性减少范围（随机值）
     */
    public int getRandomDowngradeValue(Random random) {
        return getRandomValue(random, downgradeMin, downgradeMax);
    }
    
    /**
     * 获取成功属性增加范围（随机值）
     */
    public int getRandomSuccessValue(Random random) {
        return getRandomValue(random, successMin, successMax);
    }
    
    /**
     * 获取极品属性增加范围（随机值）
     */
    public int getRandomPerfectValue(Random random) {
        return getRandomValue(random, perfectMin, perfectMax);
    }
    
    /**
     * 生成指定范围内的随机值（通用方法）
     */
    private int getRandomValue(Random random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }
    
    /**
     * 获取装备类型对应的属性列表
     */
    public List<String> getAttributesForType(String equipmentType) {
        return equipmentTypeAttributes.getOrDefault(equipmentType.toLowerCase(), new ArrayList<>());
    }
    
    /**
     * 获取属性的显示名称
     */
    public String getAttributeName(String attributeKey) {
        return attributeNames.getOrDefault(attributeKey, attributeKey);
    }
    
    /**
     * 重新加载配置
     */
    public void reload() {
        loadConfig();
    }
}
