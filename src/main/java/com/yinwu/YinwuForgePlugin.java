package com.yinwu;

import com.yinwu.manager.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class YinwuForgePlugin extends JavaPlugin {

    private ConfigManager configManager;
    private PotionEffectManager potionEffectManager;
    private PotionForgeConfig potionForgeConfig;
    private AlloyForgeConfig alloyForgeConfig;
    private MaterialConfig materialConfig;
    private ForgeManager forgeManager;
    private AltarManager altarManager;
    private ForgeGUI forgeGUI;
    private CommandHandler commandHandler;
    private EventListener eventListener;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        potionEffectManager = new PotionEffectManager(this, configManager);
        potionForgeConfig = new PotionForgeConfig(this, configManager);
        alloyForgeConfig = new AlloyForgeConfig(this, configManager);
        materialConfig = new MaterialConfig(this, configManager);
        forgeManager = new ForgeManager(this, configManager, alloyForgeConfig, potionEffectManager);
        altarManager = new AltarManager(this, configManager, forgeManager);
        forgeGUI = new ForgeGUI(this, materialConfig, forgeManager, altarManager, configManager);
        altarManager.setForgeGUI(forgeGUI);
        commandHandler = new CommandHandler(this, configManager, forgeManager, potionEffectManager, altarManager, materialConfig);
        eventListener = new EventListener(this, configManager, forgeManager, potionEffectManager, altarManager);
        eventListener.setForgeGUI(forgeGUI);

        getCommand("yinwu").setExecutor(commandHandler);
        getCommand("yinwu").setTabCompleter(commandHandler);

        if (configManager.getBoolean("debug")) {
            getLogger().info("YinwuForge 已启用！");
            getLogger().info("运行在 Folia: " + isFolia());
            getLogger().info("已加载 " + potionEffectManager.getNormalEffects().size() + " 种普通药水效果");
            getLogger().info("已加载 " + potionEffectManager.getSpecialEffects().size() + " 种特殊药水效果");
            getLogger().info("已加载 " + potionEffectManager.getNegativeEffects().size() + " 种负面药水效果");
            getLogger().info("已加载 " + materialConfig.getAllConcentrated().size() + " 种浓缩材料");
        }
    }

    @Override
    public void onDisable() {
        if (forgeGUI != null) {
            forgeGUI.closeAllGUIs();
        }
        if (configManager.getBoolean("debug")) {
            getLogger().info("YinwuForge 已禁用。");
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ForgeManager getForgeManager() {
        return forgeManager;
    }

    public PotionForgeConfig getPotionForgeConfig() {
        return potionForgeConfig;
    }

    public AlloyForgeConfig getAlloyForgeConfig() {
        return alloyForgeConfig;
    }

    public PotionEffectManager getPotionEffectManager() {
        return potionEffectManager;
    }

    public AltarManager getAltarManager() {
        return altarManager;
    }

    public MaterialConfig getMaterialConfig() {
        return materialConfig;
    }

    public ForgeGUI getForgeGUI() {
        return forgeGUI;
    }

    public boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
