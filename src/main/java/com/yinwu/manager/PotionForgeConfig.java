package com.yinwu.manager;

import com.yinwu.YinwuForgePlugin;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 药水锻造配置管理器
 */
public class PotionForgeConfig {
    private final YinwuForgePlugin plugin;
    private final ConfigManager configManager;
    
    // 基础配置
    private boolean enabled;
    private String materialType;
    private String customName;  // 自定义显示名称
    
    public PotionForgeConfig(YinwuForgePlugin plugin, ConfigManager configManager) {
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
        enabled = config.getBoolean("potion-forge.enabled", true);
        materialType = config.getString("potion-forge.material", "NETHER_STAR");
        customName = config.getString("potion-forge.custom-name", "");
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
     * 重新加载配置
     */
    public void reload() {
        loadConfig();
    }
}
