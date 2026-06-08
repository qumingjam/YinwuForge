package com.yinwu.manager;

import com.yinwu.YinwuForgePlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final YinwuForgePlugin plugin;
    private FileConfiguration config;  // 配置文件
    
    private final Map<String, Object> settings = new HashMap<>();  // 设置映射
    private final Map<String, Double> potionForgeChances = new HashMap<>();  // 药水锻造概率映射
    private final Map<String, Double> alloyForgeChances = new HashMap<>();  // 合金锻造概率映射
    
    public ConfigManager(YinwuForgePlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        settings.clear();
        potionForgeChances.clear();
        alloyForgeChances.clear();
        
        // 加载调试模式
        settings.put("debug", config.getBoolean("debug", false));
        
        // 加载药水锻造概率（第一种材料 - 下界之星）
        potionForgeChances.put("fail-no-penalty", config.getDouble("potion-forge-chances.fail-no-penalty", 20.0));
        potionForgeChances.put("equipment-destroyed", config.getDouble("potion-forge-chances.equipment-destroyed", 15.0));
        potionForgeChances.put("downgrade", config.getDouble("potion-forge-chances.downgrade", 15.0));
        potionForgeChances.put("success", config.getDouble("potion-forge-chances.success", 35.0));
        potionForgeChances.put("perfect", config.getDouble("potion-forge-chances.perfect", 15.0));
        
        // 加载合金锻造概率（第二种材料 - 下界合金锭）
        alloyForgeChances.put("fail-no-penalty", config.getDouble("alloy-forge-chances.fail-no-penalty", 25.0));
        alloyForgeChances.put("equipment-destroyed", config.getDouble("alloy-forge-chances.equipment-destroyed", 10.0));
        alloyForgeChances.put("downgrade", config.getDouble("alloy-forge-chances.downgrade", 15.0));
        alloyForgeChances.put("success", config.getDouble("alloy-forge-chances.success", 35.0));
        alloyForgeChances.put("perfect", config.getDouble("alloy-forge-chances.perfect", 15.0));
        
        // 加载Boss设置
        settings.put("boss.spawn-chance", config.getDouble("boss.spawn-chance", 0.05));
        settings.put("boss.loot-multiplier", config.getDouble("boss.loot-multiplier", 1.5));
        
        // 加载交易设置
        settings.put("trade.singularity-price", config.getInt("trade.singularity-price", 10));
        
        // 加载调度器设置
        settings.put("scheduler.default-delay", config.getInt("scheduler.default-delay", 20));
        settings.put("scheduler.default-interval", config.getInt("scheduler.default-interval", 60));
        
        validateConfig();
    }
    
    private void validateConfig() {
        // 验证药水锻造概率总和约为100
        double potionTotalChance = potionForgeChances.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(potionTotalChance - 100.0) > 0.01) {
            plugin.getLogger().warning("药水锻造概率总和为 " + potionTotalChance + "，期望值为 100.0");
        }
        
        // 验证合金锻造概率总和约为100
        double alloyTotalChance = alloyForgeChances.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(alloyTotalChance - 100.0) > 0.01) {
            plugin.getLogger().warning("合金锻造概率总和为 " + alloyTotalChance + "，期望值为 100.0");
        }
        
        // 验证调度器设置
        int delay = (Integer) settings.get("scheduler.default-delay");
        if (delay < 1) {
            plugin.getLogger().warning("scheduler.default-delay 必须 >= 1，使用默认值 20");
            settings.put("scheduler.default-delay", 20);
        }
        
        int interval = (Integer) settings.get("scheduler.default-interval");
        if (interval < 1) {
            plugin.getLogger().warning("scheduler.default-interval 必须 >= 1，使用默认值 60");
            settings.put("scheduler.default-interval", 60);
        }
    }
    
    /**
     * 获取药水锻造概率（第一种材料）
     */
    public double getPotionForgeChance(String resultType) {
        return potionForgeChances.getOrDefault(resultType, 0.0);
    }
    
    /**
     * 获取合金锻造概率（第二种材料）
     */
    public double getAlloyForgeChance(String resultType) {
        return alloyForgeChances.getOrDefault(resultType, 0.0);
    }
    
    public int getInt(String key) {
        if (settings.containsKey(key)) {
            return (Integer) settings.get(key);
        }
        return config.getInt(key, 10);
    }

    public double getDouble(String key) {
        if (settings.containsKey(key)) {
            return (Double) settings.get(key);
        }
        return config.getDouble(key, 1.0);
    }

    public String getString(String key) {
        if (settings.containsKey(key)) {
            Object value = settings.get(key);
            return value != null ? value.toString() : "";
        }
        return config.getString(key, "");
    }

    public boolean getBoolean(String key) {
        if (settings.containsKey(key)) {
            return (Boolean) settings.get(key);
        }
        return config.getBoolean(key, true);
    }

    public long getLong(String key) {
        if (settings.containsKey(key)) {
            Object value = settings.get(key);
            if (value instanceof Integer) {
                return ((Integer) value).longValue();
            }
            return (Long) value;
        }
        return config.getLong(key, 2000L);
    }
    
    public FileConfiguration getRawConfig() {
        return config;
    }
    
    public void reload() {
        loadConfig();
    }
}
