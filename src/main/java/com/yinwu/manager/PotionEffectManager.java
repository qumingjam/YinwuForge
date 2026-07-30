package com.yinwu.manager;

import com.yinwu.YinwuForgePlugin;
import com.yinwu.model.PotionEffectData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 药水效果管理器
 */
public class PotionEffectManager {
    private final YinwuForgePlugin plugin;
    private final ConfigManager configManager;

    // 药水效果配置
    private final Map<String, Integer> normalEffects = new ConcurrentHashMap<>();
    private final Map<String, Integer> specialEffects = new ConcurrentHashMap<>();
    private final Map<String, Integer> negativeEffects = new ConcurrentHashMap<>();
    private final Map<String, String> effectChineseNames = new ConcurrentHashMap<>();
    private int maxEffectsPerItem;
    private boolean potionEffectsEnabled;

    public PotionEffectManager(YinwuForgePlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        loadConfig();
    }

    /**
     * 加载药水效果配置
     */
    public void loadConfig() {
        FileConfiguration config = configManager.getRawConfig();

        // 加载药水效果功能开关
        potionEffectsEnabled = config.getBoolean("potion-effects.enabled", true);

        // 加载最大效果数量
        maxEffectsPerItem = config.getInt("potion-effects.max-effects-per-item", 5);

        // 加载药水效果配置
        normalEffects.clear();
        loadEffectsFromConfig(config, "potion-effects.normal-effects", normalEffects);

        specialEffects.clear();
        loadEffectsFromConfig(config, "potion-effects.special-effects", specialEffects);

        negativeEffects.clear();
        loadEffectsFromConfig(config, "potion-effects.negative-effects", negativeEffects);

        // 加载药水效果中文名称映射
        effectChineseNames.clear();
        if (config.isConfigurationSection("potion-effects.effect-names")) {
            for (String key : config.getConfigurationSection("potion-effects.effect-names").getKeys(false)) {
                String englishName = key.toUpperCase();
                String chineseName = config.getString("potion-effects.effect-names." + key);
                if (chineseName != null && !chineseName.isEmpty()) {
                    effectChineseNames.put(englishName, chineseName);
                }
            }
        }
    }

    /**
     * 从配置加载药水效果（通用方法）
     */
    private void loadEffectsFromConfig(FileConfiguration config, String path, Map<String, Integer> targetMap) {
        if (!config.isConfigurationSection(path)) {
            return;
        }

        for (String key : config.getConfigurationSection(path).getKeys(false)) {
            String effectName = key.toUpperCase();
            int maxLevel = config.getInt(path + "." + key);
            if (PotionEffectType.getByName(effectName) != null) {
                targetMap.put(effectName, maxLevel);
            }
        }
    }

    /**
     * 检查药水效果功能是否启用
     */
    public boolean isPotionEffectsEnabled() {
        return potionEffectsEnabled;
    }

    /**
     * 获取最大药水效果数量
     */
    public int getMaxEffectsPerItem() {
        return maxEffectsPerItem;
    }

    /**
     * 获取普通药水效果配置
     */
    public Map<String, Integer> getNormalEffects() {
        return normalEffects;
    }

    /**
     * 获取特殊药水效果配置
     */
    public Map<String, Integer> getSpecialEffects() {
        return specialEffects;
    }

    /**
     * 获取负面药水效果配置
     */
    public Map<String, Integer> getNegativeEffects() {
        return negativeEffects;
    }

    /**
     * 检查某个效果名称是否是负面效果
     */
    public boolean isNegativeEffect(String effectName) {
        return negativeEffects.containsKey(effectName);
    }

    /**
     * 随机选择一个未使用的普通药水效果
     */
    public PotionEffectData getRandomNormalEffect(List<PotionEffectData> currentEffects) {
        List<String> availableEffects = new ArrayList<>();
        for (String effectName : normalEffects.keySet()) {
            boolean exists = currentEffects.stream()
                .anyMatch(e -> e.getEffectName().equals(effectName) && !e.isSpecial());
            if (!exists) {
                availableEffects.add(effectName);
            }
        }

        if (availableEffects.isEmpty()) {
            return null;
        }

        String selected = availableEffects.get(ThreadLocalRandom.current().nextInt(availableEffects.size()));
        return new PotionEffectData(selected, 1, false);
    }

    /**
     * 随机选择一个负面药水效果
     */
    public PotionEffectData getRandomNegativeEffect() {
        if (negativeEffects.isEmpty()) {
            return null;
        }

        List<String> effects = new ArrayList<>(negativeEffects.keySet());
        String selected = effects.get(ThreadLocalRandom.current().nextInt(effects.size()));
        int level = ThreadLocalRandom.current().nextInt(negativeEffects.get(selected)) + 1;
        return new PotionEffectData(selected, level, false, true);
    }

    /**
     * 随机选择一个特殊药水效果
     */
    public PotionEffectData getRandomSpecialEffect(List<PotionEffectData> currentEffects) {
        List<String> availableEffects = new ArrayList<>();
        for (String effectName : specialEffects.keySet()) {
            boolean exists = currentEffects.stream()
                .anyMatch(e -> e.getEffectName().equals(effectName) && e.isSpecial());
            if (!exists) {
                availableEffects.add(effectName);
            }
        }

        if (availableEffects.isEmpty()) {
            return null;
        }

        String selected = availableEffects.get(ThreadLocalRandom.current().nextInt(availableEffects.size()));
        return new PotionEffectData(selected, 1, true);
    }

    /**
     * 检查是否可以升级药水效果
     */
    public boolean canUpgradeEffect(PotionEffectData effect) {
        if (effect.isSpecial()) {
            Integer maxLevel = specialEffects.get(effect.getEffectName());
            return maxLevel != null && effect.getLevel() < maxLevel;
        } else {
            Integer maxLevel = normalEffects.get(effect.getEffectName());
            return maxLevel != null && effect.getLevel() < maxLevel;
        }
    }

    /**
     * 检查所有普通药水效果是否都达到上限
     */
    public boolean allNormalEffectsAtMax(List<PotionEffectData> currentEffects) {
        for (PotionEffectData effect : currentEffects) {
            if (!effect.isSpecial()) {
                Integer maxLevel = normalEffects.get(effect.getEffectName());
                if (maxLevel == null || effect.getLevel() < maxLevel) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 获取药水效果的中文名称
     * @param effectName 英文效果名称
     * @return 中文名称，如果没有映射则返回英文
     */
    public String getChineseName(String effectName) {
        return effectChineseNames.getOrDefault(effectName.toUpperCase(), effectName);
    }

    /**
     * 重新加载配置
     */
    public void reload() {
        loadConfig();
    }
}
