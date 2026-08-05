package com.yinwu;

import com.yinwu.api.ForgeAPIImpl;
import com.yinwu.manager.AlloyForgeConfig;
import com.yinwu.manager.AltarManager;
import com.yinwu.manager.CommandHandler;
import com.yinwu.manager.ConfigManager;
import com.yinwu.manager.EventListener;
import com.yinwu.manager.ForgeGUI;
import com.yinwu.manager.ForgeManager;
import com.yinwu.manager.MaterialConfig;
import com.yinwu.manager.PotionEffectManager;
import com.yinwu.manager.PotionForgeConfig;
import net.yinwu.lib.api.ForgeAPI;
import net.yinwu.lib.plugin.YinwuPlugin;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.ServicePriority;

public final class YinwuForgePlugin extends YinwuPlugin {

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
    public String name() {
        return "YinwuForge";
    }

    @Override
    public void enable() {
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

        getCommand("yf").setExecutor(commandHandler);
        getCommand("yf").setTabCompleter(commandHandler);

        // 注册 ForgeAPI 服务（供其他 Yinwu 插件调用）
        Bukkit.getServicesManager().register(ForgeAPI.class, new ForgeAPIImpl(this, forgeManager), this, ServicePriority.Normal);
        debug("ForgeAPI 已注册");

        // 尝试链接 YinwuEnchant —— 锻造时可能添加自定义附魔
        tryEnchantLink();

        if (configManager.getBoolean("debug")) {
            getLogger().info("已加载 " + potionEffectManager.getNormalEffects().size() + " 种普通药水效果");
            getLogger().info("已加载 " + potionEffectManager.getSpecialEffects().size() + " 种特殊药水效果");
            getLogger().info("已加载 " + potionEffectManager.getNegativeEffects().size() + " 种负面药水效果");
            getLogger().info("已加载 " + materialConfig.getAllConcentrated().size() + " 种浓缩材料");
        }
    }

    @Override
    public void disable() {
        Bukkit.getGlobalRegionScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);
        if (forgeGUI != null) {
            forgeGUI.closeAllGUIs();
        }
        if (forgeManager != null) {
            forgeManager.clearCooldowns();
        }
    }

    /** 尝试链接 Enchant 插件 */
    private void tryEnchantLink() {
        var enchant = Bukkit.getServicesManager().load(net.yinwu.lib.api.EnchantAPI.class);
        if (enchant != null) {
            getLogger().info("§a✓ 检测到 YinwuEnchant —— 已启用锻造附魔联动");
            forgeManager.setEnchantAPI(enchant);
        } else {
            fine("未检测到 YinwuEnchant（可选依赖）");
        }
    }

    public ConfigManager getConfigManager() { return configManager; }
    public ForgeManager getForgeManager() { return forgeManager; }
    public PotionForgeConfig getPotionForgeConfig() { return potionForgeConfig; }
    public AlloyForgeConfig getAlloyForgeConfig() { return alloyForgeConfig; }
    public PotionEffectManager getPotionEffectManager() { return potionEffectManager; }
    public AltarManager getAltarManager() { return altarManager; }
    public MaterialConfig getMaterialConfig() { return materialConfig; }
    public ForgeGUI getForgeGUI() { return forgeGUI; }
}
