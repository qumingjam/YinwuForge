package com.yinwu.manager;

import com.yinwu.YinwuForgePlugin;
import com.yinwu.model.EquipmentAttributes;
import com.yinwu.model.EquipmentData;
import com.yinwu.model.ForgeResult;
import com.yinwu.model.PotionEffectData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ForgeManager {
    private final YinwuForgePlugin plugin;
    private final ConfigManager configManager;
    private final PotionEffectManager potionEffectManager;
    private final AlloyForgeConfig alloyForgeConfig;
    private final PotionForgeConfig potionForgeConfig;
    private final Random random;
    
    // 白名单配置
    private boolean whitelistEnabled;
    private List<Material> toolWhitelist;
    private List<Material> weaponWhitelist;
    private List<Material> armorWhitelist;
    private List<Material> elytraWhitelist;
    
    // 冷却配置
    private int cooldownSeconds;
    // 使用 ConcurrentHashMap 确保跨玩家区域线程同时写入时的线程安全
    private final Map<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();  // 玩家UUID -> 上次锻造时间戳
    
    private static final String EQUIPMENT_DATA_KEY = "yinwu_equipment_data";  // 装备数据键
    
    // 锻造概率配置键常量
    private static final String CHANCE_FAIL_NO_PENALTY = "fail-no-penalty";
    private static final String CHANCE_EQUIPMENT_DESTROYED = "equipment-destroyed";
    private static final String CHANCE_DOWNGRADE = "downgrade";
    private static final String CHANCE_SUCCESS = "success";
    
    // 属性名称常量
    private static final String ATTR_MAX_DURABILITY = "maxDurability";
    private static final String ATTR_MINING_SPEED = "miningSpeed";
    private static final String ATTR_ARMOR_TOUGHNESS = "armorToughness";
    private static final String ATTR_ARMOR_VALUE = "armorValue";
    private static final String ATTR_ATTACK_SPEED = "attackSpeed";
    private static final String ATTR_BASE_DAMAGE = "baseDamage";
    
    // 消息常量
    private static final String MSG_FORGE_SUCCESS = "锻造成功 - 装备等级提升！";
    private static final String MSG_FORGE_DOWNGRADE = "锻造失败 - 装备等级降低！";
    private static final String MSG_FORGE_DESTROYED = "装备已损毁！";
    private static final String MSG_FORGE_NEGATIVE_EFFECT = "附加负面效果: ";
    private static final String MSG_NO_ATTRIBUTES = "该装备无可提升的属性";
    private static final String MSG_PERFECT = "完美锻造 - 获得极品属性！";
    private static final String MSG_NO_EFFECTS = "该装备无可添加的效果";
    
    // ===== 硬编码武器基础属性映射表 =====
    // 基础伤害贡献值（不含空手1点）
    public static final Map<Material, Double> WEAPON_BASE_DAMAGE = new HashMap<>();
    // 基础攻击速度
    public static final Map<Material, Double> WEAPON_BASE_SPEED = new HashMap<>();
    // 玩家空手基础攻击速度常量
    public static final double BASE_PLAYER_ATTACK_SPEED = 4.0;
    
    // ===== 硬编码盔甲基础属性映射表 =====
    // 基础盔甲值 (ARMOR)
    public static final Map<Material, Double> ARMOR_BASE_VALUES = new HashMap<>();
    // 基础护甲韧性 (ARMOR_TOUGHNESS)
    public static final Map<Material, Double> ARMOR_TOUGHNESS_BASE_VALUES = new HashMap<>();
    // 基础击退抗性 (KNOCKBACK_RESISTANCE) — 仅下界合金系列有
    public static final Map<Material, Double> KNOCKBACK_RESISTANCE_BASE_VALUES = new HashMap<>();
    
    static {
        // 剑类（攻速都是 1.6）
        WEAPON_BASE_DAMAGE.put(Material.WOODEN_SWORD, 3.0);    // 总伤4
        WEAPON_BASE_DAMAGE.put(Material.STONE_SWORD, 4.0);     // 总伤5
        WEAPON_BASE_DAMAGE.put(Material.IRON_SWORD, 5.0);      // 总伤6
        WEAPON_BASE_DAMAGE.put(Material.GOLDEN_SWORD, 3.0);    // 总伤4
        WEAPON_BASE_DAMAGE.put(Material.DIAMOND_SWORD, 6.0);   // 总伤7
        WEAPON_BASE_DAMAGE.put(Material.NETHERITE_SWORD, 7.0); // 总伤8
        for (Material sw : List.of(Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD)) {
            WEAPON_BASE_SPEED.put(sw, 1.6);
        }
        
        // 斧头类
        WEAPON_BASE_DAMAGE.put(Material.WOODEN_AXE, 6.0);     // 总伤7
        WEAPON_BASE_DAMAGE.put(Material.STONE_AXE, 8.0);      // 总伤9
        WEAPON_BASE_DAMAGE.put(Material.IRON_AXE, 8.0);       // 总伤9
        WEAPON_BASE_DAMAGE.put(Material.GOLDEN_AXE, 6.0);     // 总伤7
        WEAPON_BASE_DAMAGE.put(Material.DIAMOND_AXE, 8.0);    // 总伤9
        WEAPON_BASE_DAMAGE.put(Material.NETHERITE_AXE, 9.0);  // 总伤10
        WEAPON_BASE_SPEED.put(Material.WOODEN_AXE, 0.8);
        WEAPON_BASE_SPEED.put(Material.STONE_AXE, 0.8);
        WEAPON_BASE_SPEED.put(Material.IRON_AXE, 0.9);
        WEAPON_BASE_SPEED.put(Material.GOLDEN_AXE, 1.0);
        WEAPON_BASE_SPEED.put(Material.DIAMOND_AXE, 1.0);
        WEAPON_BASE_SPEED.put(Material.NETHERITE_AXE, 1.0);
        
        // 镐类（攻速都是 1.2）
        WEAPON_BASE_DAMAGE.put(Material.WOODEN_PICKAXE, 1.0);   // 总伤2
        WEAPON_BASE_DAMAGE.put(Material.STONE_PICKAXE, 2.0);    // 总伤3
        WEAPON_BASE_DAMAGE.put(Material.IRON_PICKAXE, 3.0);     // 总伤4
        WEAPON_BASE_DAMAGE.put(Material.GOLDEN_PICKAXE, 1.0);   // 总伤2
        WEAPON_BASE_DAMAGE.put(Material.DIAMOND_PICKAXE, 4.0);  // 总伤5
        WEAPON_BASE_DAMAGE.put(Material.NETHERITE_PICKAXE, 5.0);// 总伤6
        for (Material pk : List.of(Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
                Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE)) {
            WEAPON_BASE_SPEED.put(pk, 1.2);
        }
        
        // 铲类（攻速都是 1.0）
        WEAPON_BASE_DAMAGE.put(Material.WOODEN_SHOVEL, 1.5);    // 总伤2.5
        WEAPON_BASE_DAMAGE.put(Material.STONE_SHOVEL, 2.5);     // 总伤3.5
        WEAPON_BASE_DAMAGE.put(Material.IRON_SHOVEL, 3.5);      // 总伤4.5
        WEAPON_BASE_DAMAGE.put(Material.GOLDEN_SHOVEL, 1.5);    // 总伤2.5
        WEAPON_BASE_DAMAGE.put(Material.DIAMOND_SHOVEL, 4.5);   // 总伤5.5
        WEAPON_BASE_DAMAGE.put(Material.NETHERITE_SHOVEL, 5.5); // 总伤6.5
        for (Material sh : List.of(Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
                Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL)) {
            WEAPON_BASE_SPEED.put(sh, 1.0);
        }
        
        // 锄类（伤害都是 1，无基础贡献）
        WEAPON_BASE_DAMAGE.put(Material.WOODEN_HOE, 0.0);
        WEAPON_BASE_DAMAGE.put(Material.STONE_HOE, 0.0);
        WEAPON_BASE_DAMAGE.put(Material.IRON_HOE, 0.0);
        WEAPON_BASE_DAMAGE.put(Material.GOLDEN_HOE, 0.0);
        WEAPON_BASE_DAMAGE.put(Material.DIAMOND_HOE, 0.0);
        WEAPON_BASE_DAMAGE.put(Material.NETHERITE_HOE, 0.0);
        WEAPON_BASE_SPEED.put(Material.WOODEN_HOE, 1.0);
        WEAPON_BASE_SPEED.put(Material.STONE_HOE, 2.0);
        WEAPON_BASE_SPEED.put(Material.IRON_HOE, 3.0);
        WEAPON_BASE_SPEED.put(Material.GOLDEN_HOE, 1.0);
        WEAPON_BASE_SPEED.put(Material.DIAMOND_HOE, 4.0);
        WEAPON_BASE_SPEED.put(Material.NETHERITE_HOE, 4.0);
        
        // 特殊武器
        WEAPON_BASE_DAMAGE.put(Material.TRIDENT, 8.0);  // 总伤害 9
        WEAPON_BASE_SPEED.put(Material.TRIDENT, 1.1);
        WEAPON_BASE_DAMAGE.put(Material.MACE, 5.0);     // 总伤害 6
        WEAPON_BASE_SPEED.put(Material.MACE, 0.6);
        
        // ===== 盔甲基础属性（根据 Minecraft Wiki 写死）=====
        // 头盔
        ARMOR_BASE_VALUES.put(Material.LEATHER_HELMET, 1.0);
        ARMOR_BASE_VALUES.put(Material.CHAINMAIL_HELMET, 2.0);
        ARMOR_BASE_VALUES.put(Material.IRON_HELMET, 2.0);
        ARMOR_BASE_VALUES.put(Material.GOLDEN_HELMET, 2.0);
        ARMOR_BASE_VALUES.put(Material.DIAMOND_HELMET, 3.0);
        ARMOR_BASE_VALUES.put(Material.NETHERITE_HELMET, 3.0);
        ARMOR_BASE_VALUES.put(Material.TURTLE_HELMET, 2.0);
        // 胸甲
        ARMOR_BASE_VALUES.put(Material.LEATHER_CHESTPLATE, 3.0);
        ARMOR_BASE_VALUES.put(Material.CHAINMAIL_CHESTPLATE, 5.0);
        ARMOR_BASE_VALUES.put(Material.IRON_CHESTPLATE, 6.0);
        ARMOR_BASE_VALUES.put(Material.GOLDEN_CHESTPLATE, 5.0);
        ARMOR_BASE_VALUES.put(Material.DIAMOND_CHESTPLATE, 8.0);
        ARMOR_BASE_VALUES.put(Material.NETHERITE_CHESTPLATE, 8.0);
        // 护腿
        ARMOR_BASE_VALUES.put(Material.LEATHER_LEGGINGS, 2.0);
        ARMOR_BASE_VALUES.put(Material.CHAINMAIL_LEGGINGS, 4.0);
        ARMOR_BASE_VALUES.put(Material.IRON_LEGGINGS, 5.0);
        ARMOR_BASE_VALUES.put(Material.GOLDEN_LEGGINGS, 3.0);
        ARMOR_BASE_VALUES.put(Material.DIAMOND_LEGGINGS, 6.0);
        ARMOR_BASE_VALUES.put(Material.NETHERITE_LEGGINGS, 6.0);
        // 靴子
        ARMOR_BASE_VALUES.put(Material.LEATHER_BOOTS, 1.0);
        ARMOR_BASE_VALUES.put(Material.CHAINMAIL_BOOTS, 1.0);
        ARMOR_BASE_VALUES.put(Material.IRON_BOOTS, 2.0);
        ARMOR_BASE_VALUES.put(Material.GOLDEN_BOOTS, 1.0);
        ARMOR_BASE_VALUES.put(Material.DIAMOND_BOOTS, 3.0);
        ARMOR_BASE_VALUES.put(Material.NETHERITE_BOOTS, 3.0);
        
        // 护甲韧性（钻石+2，下界合金+3，其余为0）
        for (Material mat : List.of(Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE,
                Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS)) {
            ARMOR_TOUGHNESS_BASE_VALUES.put(mat, 2.0);
        }
        for (Material mat : List.of(Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
                Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS)) {
            ARMOR_TOUGHNESS_BASE_VALUES.put(mat, 3.0);
        }
        
        // 击退抗性（仅下界合金系列，每件+0.1）
        for (Material mat : List.of(Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
                Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS)) {
            KNOCKBACK_RESISTANCE_BASE_VALUES.put(mat, 0.1);
        }
        
        // ===== 铜盔甲（动态加载，兼容后续 API 版本）=====
        for (String copper : List.of("COPPER_HELMET|2", "COPPER_CHESTPLATE|4", "COPPER_LEGGINGS|3", "COPPER_BOOTS|1")) {
            String[] parts = copper.split("\\|");
            Material mat = Material.matchMaterial(parts[0]);
            if (mat != null) {
                ARMOR_BASE_VALUES.put(mat, Double.parseDouble(parts[1]));
            }
        }
    }
    
    public ForgeManager(YinwuForgePlugin plugin, ConfigManager configManager, AlloyForgeConfig alloyForgeConfig, PotionForgeConfig potionForgeConfig, PotionEffectManager potionEffectManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.potionEffectManager = potionEffectManager;
        this.alloyForgeConfig = alloyForgeConfig;
        this.potionForgeConfig = potionForgeConfig;
        this.random = new Random();
        loadConfig();
    }
    
    /**
     * 加载配置
     */
    private void loadConfig() {
        org.bukkit.configuration.file.FileConfiguration config = configManager.getRawConfig();
        cooldownSeconds = config.getInt("forge.cooldown", 3);
        loadWhitelistConfig();
    }
    
    /**
     * 重新加载配置
     */
    public void reload() {
        loadConfig();
    }
    
    /**
     * 加载白名单配置
     */
    private void loadWhitelistConfig() {
        org.bukkit.configuration.file.FileConfiguration config = configManager.getRawConfig();
        
        whitelistEnabled = config.getBoolean("forge-whitelist.enabled", false);
        
        // 加载工具白名单
        toolWhitelist = new ArrayList<>();
        for (String matName : config.getStringList("forge-whitelist.tools")) {
            try {
                toolWhitelist.add(Material.valueOf(matName.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的工具白名单配置: " + matName);
            }
        }
        
        // 加载武器白名单
        weaponWhitelist = new ArrayList<>();
        for (String matName : config.getStringList("forge-whitelist.weapons")) {
            try {
                weaponWhitelist.add(Material.valueOf(matName.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的武器白名单配置: " + matName);
            }
        }
        
        // 加载盔甲白名单
        armorWhitelist = new ArrayList<>();
        for (String matName : config.getStringList("forge-whitelist.armors")) {
            try {
                armorWhitelist.add(Material.valueOf(matName.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的盔甲白名单配置: " + matName);
            }
        }
        
        // 加载鞘翅白名单
        elytraWhitelist = new ArrayList<>();
        for (String matName : config.getStringList("forge-whitelist.elytras")) {
            try {
                elytraWhitelist.add(Material.valueOf(matName.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的鞘翅白名单配置: " + matName);
            }
        }
    }
    
    /**
     * 执行锻造操作
     */
    public ForgeResult executeForge(Player player, ItemStack equipment, ItemStack material) {
        return executeForge(player, equipment, material, 0.0, 0.0);
    }
    
    /**
     * 执行锻造操作（带加成）
     * @param successBonus 成功率加成(%)
     * @param failReduction 失败率减少(%)
     */
    public ForgeResult executeForge(Player player, ItemStack equipment, ItemStack material, 
                                    double successBonus, double failReduction) {
        if (equipment == null || equipment.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "主手必须持有装备！");
            return null;
        }
        
        if (material == null || material.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "副手必须持有锻造材料（奇点或新矿石）！");
            return null;
        }
        
        // 检查冷却
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long cooldownMillis = cooldownSeconds * 1000L;
        Long lastForgeTime = playerCooldowns.get(playerUUID);
        
        if (lastForgeTime != null && (currentTime - lastForgeTime) < cooldownMillis) {
            long remaining = (cooldownMillis - (currentTime - lastForgeTime));
            double remainingSeconds = remaining / 1000.0;
            player.sendActionBar(ChatColor.RED + "锻造冷却中！剩余 " + String.format("%.1f", remainingSeconds) + " 秒");
            return null;
        }
        
        // 检查装备类型是否支持锻造（仅工具、武器、盔甲、鞘翅）
        EquipmentType equipType = getEquipmentType(equipment);
        if (equipType == EquipmentType.OTHER) {
            player.sendMessage(ChatColor.RED + "该物品无法进行锻造！");
            player.sendMessage(ChatColor.GRAY + "仅支持工具、武器、盔甲和鞘翅");
            return null;
        }
        
        // 检查是否为第二种材料（下界合金锭）
        if (isAlloyMaterial(material)) {
            // 执行合金锻造（属性修改）
            return executeAlloyForge(player, equipment, material, successBonus, failReduction);
        }
        
        // 检查材料是否有效
        if (!isValidForgeMaterial(material)) {
            // 提供更详细的错误信息
            String customName = potionForgeConfig.getCustomName();
            if (customName != null && !customName.isEmpty()) {
                player.sendMessage(ChatColor.RED + "无效的锻造材料！");
                player.sendMessage(ChatColor.GRAY + "药水锻造需要名称为 " + customName + ChatColor.GRAY + " 的下界之星");
                player.sendMessage(ChatColor.GRAY + "或使用矿石、原石等材料");
            } else {
                player.sendMessage(ChatColor.RED + "无效的锻造材料！");
            }
            return null;
        }
        
        // 获取或创建装备数据
        EquipmentData equipmentData = getEquipmentData(equipment);
        
        // 检查是否有特殊效果，如果有则无法锻造
        if (equipmentData.containsSpecialEffect()) {
            player.sendMessage(ChatColor.RED + "该装备已附加特殊效果，无法继续锻造！");
            return null;
        }
        
        // 根据药水锻造概率确定结果（带加成）
        ForgeResult result = determinePotionForgeResult(successBonus, failReduction);
        
        // 应用锻造结果（包含药水效果）
        applyForgeResult(player, equipment, equipmentData, result);
        
        // 锻造完成后，立即应用属性到物品（包括耐久度）
        applyAttributesToItem(equipment);
        
        // 消耗一个材料
        consumeMaterial(player, material);
        
        // 设置冷却
        playerCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        
        return result;
    }
    
    /**
     * 根据配置的概率确定锻造结果（药水锻造）
     */
    private ForgeResult determinePotionForgeResult() {
        return determinePotionForgeResult(0.0, 0.0);
    }
    
    /**
     * 根据配置的概率确定锻造结果（药水锻造，带加成）
     */
    private ForgeResult determinePotionForgeResult(double successBonus, double failReduction) {
        return determineForgeResult(
            configManager.getPotionForgeChance(CHANCE_FAIL_NO_PENALTY),
            configManager.getPotionForgeChance(CHANCE_EQUIPMENT_DESTROYED),
            configManager.getPotionForgeChance(CHANCE_DOWNGRADE),
            configManager.getPotionForgeChance(CHANCE_SUCCESS),
            successBonus,
            failReduction
        );
    }
    
    /**
     * 根据配置的概率确定锻造结果（合金锻造）
     */
    private ForgeResult determineAlloyForgeResult() {
        return determineAlloyForgeResult(0.0, 0.0);
    }
    
    /**
     * 根据配置的概率确定锻造结果（合金锻造，带加成）
     */
    private ForgeResult determineAlloyForgeResult(double successBonus, double failReduction) {
        return determineForgeResult(
            configManager.getAlloyForgeChance(CHANCE_FAIL_NO_PENALTY),
            configManager.getAlloyForgeChance(CHANCE_EQUIPMENT_DESTROYED),
            configManager.getAlloyForgeChance(CHANCE_DOWNGRADE),
            configManager.getAlloyForgeChance(CHANCE_SUCCESS),
            successBonus,
            failReduction
        );
    }
    
    /**
     * 通用的锻造结果判定方法
     */
    private ForgeResult determineForgeResult(double failNoPenalty, double equipmentDestroyed, 
                                            double downgrade, double success) {
        return determineForgeResult(failNoPenalty, equipmentDestroyed, downgrade, success, 0.0, 0.0);
    }
    
    /**
     * 通用的锻造结果判定方法（带加成）
     */
    private ForgeResult determineForgeResult(double failNoPenalty, double equipmentDestroyed, 
                                            double downgrade, double success,
                                            double successBonus, double failReduction) {
        // 失败率降低只分配给装备摧毁和降级，无惩罚失败不参与
        double totalBadFailRate = equipmentDestroyed + downgrade;
        if (totalBadFailRate > 0) {
            // 按比例分配失败率降低
            double destroyRatio = equipmentDestroyed / totalBadFailRate;
            double downgradeRatio = downgrade / totalBadFailRate;
            
            equipmentDestroyed = Math.max(0, equipmentDestroyed - failReduction * destroyRatio);
            downgrade = Math.max(0, downgrade - failReduction * downgradeRatio);
        }
        
        // 应用成功率加成（分配给成功和极品）
        double totalFailRate = failNoPenalty + equipmentDestroyed + downgrade;
        double remaining = 100 - totalFailRate;
        double bonus = Math.min(successBonus, remaining);
        
        // 将加成分配给成功和极品（按原有比例）
        double successRatio = success / (success + 15); // 15 是极品的基础概率
        success += bonus * successRatio;
        // 极品概率自动增加（剩余的加成）
        
        double roll = random.nextDouble() * 100;
        
        if (roll < failNoPenalty) {
            return ForgeResult.FAIL_NO_PENALTY;
        } else if (roll < failNoPenalty + equipmentDestroyed) {
            return ForgeResult.EQUIPMENT_DESTROYED;
        } else if (roll < failNoPenalty + equipmentDestroyed + downgrade) {
            return ForgeResult.DOWNGRADE;
        } else if (roll < failNoPenalty + equipmentDestroyed + downgrade + success) {
            return ForgeResult.SUCCESS;
        } else {
            return ForgeResult.PERFECT;
        }
    }
    
    /**
     * 将锻造结果应用到装备上
     */
    private void applyForgeResult(Player player, ItemStack equipment, EquipmentData equipmentData, ForgeResult result) {
        // 检查药水效果功能是否启用
        boolean potionEffectsEnabled = potionEffectManager.isPotionEffectsEnabled();
        
        switch (result) {
            case FAIL_NO_PENALTY:
                // 无惩罚：消耗材料，无变化，锻造次数不增加
                // 但仍需刷新 Lore 以显示装备属性信息
                player.sendMessage(result.getFullMessage());
                updateEquipmentLore(equipment, equipmentData);
                saveEquipmentData(equipment, equipmentData);
                break;
                
            case EQUIPMENT_DESTROYED:
                if (!potionEffectsEnabled) {
                    // 药水效果功能禁用时，直接摧毁装备
                    player.getInventory().setItemInMainHand(null);
                    player.sendMessage(ChatColor.RED + "锻造失败！");
                    player.sendMessage(ChatColor.RED + "装备已损毁！");
                } else {
                    // 失败：50%概率摧毁装备，50%概率附加负面药水效果
                    if (random.nextBoolean()) {
                        // 50%：摧毁装备
                        player.getInventory().setItemInMainHand(null);
                        player.sendMessage(ChatColor.RED + "锻造失败！");
                        player.sendMessage(ChatColor.RED + "装备已损毁！");
                    } else {
                        // 50%：附加负面药水效果
                        PotionEffectData negativeEffect = potionEffectManager.getRandomNegativeEffect();
                        if (negativeEffect != null) {
                            equipmentData.addPotionEffect(negativeEffect);
                            player.sendMessage(ChatColor.RED + "锻造失败！");
                            String chineseName = potionEffectManager.getChineseName(negativeEffect.getEffectName());
                            player.sendMessage(ChatColor.RED + "附加负面效果: " + chineseName);
                        } else {
                            // 无法获取负面效果时，摧毁装备
                            player.getInventory().setItemInMainHand(null);
                            player.sendMessage(ChatColor.RED + "锻造失败！");
                            player.sendMessage(ChatColor.RED + "装备已损毁！");
                        }
                        updateEquipmentLore(equipment, equipmentData);
                        saveEquipmentData(equipment, equipmentData);
                    }
                }
                break;
                
            case DOWNGRADE:
                if (!potionEffectsEnabled) {
                    // 药水效果功能禁用时，直接摧毁装备
                    player.getInventory().setItemInMainHand(null);
                    player.sendMessage(ChatColor.YELLOW + "锻造失败 - 装备等级降低！");
                    player.sendMessage(ChatColor.RED + "装备已损毁！");
                } else {
                    // 降级：随机选中一条药水效果降级或删除
                    applyDowngradeEffect(player, equipment, equipmentData);
                }
                break;
                
            case SUCCESS:
                // 增加锻造次数（仅成功/极品计入）
                equipmentData.incrementForgeCount();
                
                if (!potionEffectsEnabled) {
                    // 药水效果功能禁用时，只做基础锻造
                    player.sendMessage(result.getFullMessage());
                    updateEquipmentLore(equipment, equipmentData);
                    saveEquipmentData(equipment, equipmentData);
                } else {
                    // 成功：添加或升级药水效果
                    applySuccessEffect(player, equipment, equipmentData);
                }
                break;
                
            case PERFECT:
                // 增加锻造次数
                equipmentData.incrementForgeCount();
                
                if (!potionEffectsEnabled) {
                    // 药水效果功能禁用时，只做基础锻造
                    player.sendMessage(result.getFullMessage());
                    updateEquipmentLore(equipment, equipmentData);
                    saveEquipmentData(equipment, equipmentData);
                } else {
                    // 极品：添加药水效果或升级两个效果
                    applyPerfectEffect(player, equipment, equipmentData);
                }
                break;
        }
    }
    
    /**
     * 应用降级效果
     */
    private void applyDowngradeEffect(Player player, ItemStack equipment, EquipmentData equipmentData) {
        List<PotionEffectData> effects = equipmentData.getPotionEffects();
        
        // 如果没有药水效果，直接摧毁装备
        if (effects.isEmpty()) {
            player.getInventory().setItemInMainHand(null);
            player.sendMessage(ChatColor.YELLOW + "锻造失败 - 装备等级降低！");
            player.sendMessage(ChatColor.RED + "装备已损毁！");
            return;
        }
        
        // 随机选择一个效果降级
        PotionEffectData selected = effects.get(random.nextInt(effects.size()));
        
        if (selected.getLevel() > 1) {
            // 2级降到1级
            selected.decrementLevel();
            player.sendMessage(ChatColor.YELLOW + "锻造失败 - 装备等级降低！");
            String chineseName = potionEffectManager.getChineseName(selected.getEffectName());
            player.sendMessage(ChatColor.GRAY + chineseName + " 降至 " + selected.getLevel() + " 级");
        } else {
            // 1级删除效果
            equipmentData.removePotionEffect(selected);
            player.sendMessage(ChatColor.YELLOW + "锻造失败 - 装备等级降低！");
            String chineseName = potionEffectManager.getChineseName(selected.getEffectName());
            player.sendMessage(ChatColor.GRAY + "移除效果: " + chineseName);
        }
        
        updateEquipmentLore(equipment, equipmentData);
        saveEquipmentData(equipment, equipmentData);
    }
    
    /**
     * 应用成功效果
     */
    private void applySuccessEffect(Player player, ItemStack equipment, EquipmentData equipmentData) {
        List<PotionEffectData> effects = equipmentData.getPotionEffects();
        int maxEffects = potionEffectManager.getMaxEffectsPerItem();
        
        // 过滤出普通效果（非特殊效果）
        List<PotionEffectData> normalEffects = effects.stream()
            .filter(e -> !e.isSpecial())
            .toList();
        
        if (normalEffects.size() < maxEffects) {
            // 普通效果还有空位，添加新效果
            PotionEffectData newEffect = potionEffectManager.getRandomNormalEffect(effects);
            if (newEffect != null) {
                equipmentData.addPotionEffect(newEffect);
                player.sendMessage(ChatColor.GREEN + "锻造成功 - 装备等级提升！");
                String chineseName = potionEffectManager.getChineseName(newEffect.getEffectName());
                player.sendMessage(ChatColor.AQUA + "获得效果: " + chineseName + " I");
            }
        } else {
            // 普通效果已满5个，尝试升级
            if (potionEffectManager.allNormalEffectsAtMax(effects)) {
                // 所有普通效果都满级，添加特殊效果
                PotionEffectData specialEffect = potionEffectManager.getRandomSpecialEffect(effects);
                if (specialEffect != null) {
                    equipmentData.addPotionEffect(specialEffect);
                    equipmentData.setHasSpecialEffect(true);
                    player.sendMessage(ChatColor.GREEN + "锻造成功 - 装备等级提升！");
                    String chineseName = potionEffectManager.getChineseName(specialEffect.getEffectName());
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "获得特殊效果: " + chineseName);
                    player.sendMessage(ChatColor.RED + "该装备已无法继续锻造！");
                }
            } else {
                // 随机选择一个可升级的普通效果
                List<PotionEffectData> upgradable = effects.stream()
                    .filter(e -> !e.isSpecial() && potionEffectManager.canUpgradeEffect(e))
                    .toList();
                
                if (!upgradable.isEmpty()) {
                    PotionEffectData selected = upgradable.get(random.nextInt(upgradable.size()));
                    selected.incrementLevel();
                    player.sendMessage(ChatColor.GREEN + "锻造成功 - 装备等级提升！");
                    String chineseName = potionEffectManager.getChineseName(selected.getEffectName());
                    player.sendMessage(ChatColor.AQUA + chineseName + " 升至 " + selected.getLevel() + " 级");
                } else {
                    // 无法升级，添加特殊效果
                    PotionEffectData specialEffect = potionEffectManager.getRandomSpecialEffect(effects);
                    if (specialEffect != null) {
                        equipmentData.addPotionEffect(specialEffect);
                        equipmentData.setHasSpecialEffect(true);
                        player.sendMessage(ChatColor.GREEN + "锻造成功 - 装备等级提升！");
                        String chineseName = potionEffectManager.getChineseName(specialEffect.getEffectName());
                        player.sendMessage(ChatColor.LIGHT_PURPLE + "获得特殊效果: " + chineseName);
                        player.sendMessage(ChatColor.RED + "该装备已无法继续锻造！");
                    }
                }
            }
        }
        
        updateEquipmentLore(equipment, equipmentData);
        saveEquipmentData(equipment, equipmentData);
    }
    
    /**
     * 应用极品效果
     */
    private void applyPerfectEffect(Player player, ItemStack equipment, EquipmentData equipmentData) {
        List<PotionEffectData> effects = equipmentData.getPotionEffects();
        int maxEffects = potionEffectManager.getMaxEffectsPerItem();
        
        // 过滤出普通效果（非特殊效果）
        List<PotionEffectData> normalEffects = effects.stream()
            .filter(e -> !e.isSpecial())
            .toList();
        
        if (normalEffects.size() < maxEffects) {
            // 普通效果还有空位，添加新效果
            PotionEffectData newEffect = potionEffectManager.getRandomNormalEffect(effects);
            if (newEffect != null) {
                equipmentData.addPotionEffect(newEffect);
                player.sendMessage(ChatColor.AQUA + "完美锻造 - 获得极品属性！");
                String chineseName = potionEffectManager.getChineseName(newEffect.getEffectName());
                player.sendMessage(ChatColor.AQUA + "获得效果: " + chineseName + " I");
            }
        } else {
            // 普通效果已满5个，尝试升级两个
            List<PotionEffectData> upgradable = new java.util.ArrayList<>(effects.stream()
                .filter(e -> !e.isSpecial() && potionEffectManager.canUpgradeEffect(e))
                .toList());
            
            int upgradeCount = 0;
            if (upgradable.size() >= 2) {
                // 随机选择两个升级
                java.util.Collections.shuffle(upgradable);
                upgradable.get(0).incrementLevel();
                upgradable.get(1).incrementLevel();
                upgradeCount = 2;
            } else if (upgradable.size() == 1) {
                upgradable.get(0).incrementLevel();
                upgradeCount = 1;
            }
            
            if (upgradeCount > 0) {
                player.sendMessage(ChatColor.AQUA + "完美锻造 - 获得极品属性！");
                player.sendMessage(ChatColor.AQUA + "升级了 " + upgradeCount + " 个效果");
            } else if (potionEffectManager.allNormalEffectsAtMax(effects)) {
                // 所有普通效果满级，添加2个特殊效果
                int specialCount = 0;
                for (int i = 0; i < 2; i++) {
                    PotionEffectData specialEffect = potionEffectManager.getRandomSpecialEffect(effects);
                    if (specialEffect != null) {
                        equipmentData.addPotionEffect(specialEffect);
                        specialCount++;
                    }
                }
                
                if (specialCount > 0) {
                    equipmentData.setHasSpecialEffect(true);
                    player.sendMessage(ChatColor.AQUA + "完美锻造 - 获得极品属性！");
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "获得 " + specialCount + " 个特殊效果");
                    player.sendMessage(ChatColor.RED + "该装备已无法继续锻造！");
                }
            }
        }
        
        updateEquipmentLore(equipment, equipmentData);
        saveEquipmentData(equipment, equipmentData);
    }
    
    /**
     * 检查材料是否有效用于锻造
     */
    private boolean isValidForgeMaterial(ItemStack material) {
        if (!potionForgeConfig.isEnabled()) {
            return false;
        }
        
        // 检查材质类型
        String potionMaterial = potionForgeConfig.getMaterialType();
        if (!material.getType().name().equals(potionMaterial) &&
            !material.getType().name().contains("_ORE") &&
            material.getType() != Material.RAW_IRON &&
            material.getType() != Material.RAW_GOLD &&
            material.getType() != Material.RAW_COPPER) {
            return false;
        }
        
        // 如果是主要材料（下界之星），检查自定义名称
        if (material.getType().name().equals(potionMaterial)) {
            String customName = potionForgeConfig.getCustomName();
            if (customName != null && !customName.isEmpty()) {
                ItemMeta meta = material.getItemMeta();
                if (meta == null || !meta.hasDisplayName()) {
                    return false;
                }
                // 比较显示名称（支持颜色代码）
                return meta.getDisplayName().equals(customName);
            }
        }
        
        return true;
    }
    
    /**
     * 从副手消耗一个材料
     */
    private void consumeMaterial(Player player, ItemStack material) {
        if (material.getAmount() > 1) {
            material.setAmount(material.getAmount() - 1);
        } else {
            player.getInventory().setItemInOffHand(null);
        }
    }
    
    /**
     * 从 NBT 获取装备数据
     */
    public EquipmentData getEquipmentData(ItemStack equipment) {
        ItemMeta meta = equipment.getItemMeta();
        if (meta == null) {
            return new EquipmentData();
        }
        
        NamespacedKey key = new NamespacedKey(plugin, EQUIPMENT_DATA_KEY);
        String data = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        
        if (data == null || data.isEmpty()) {
            return new EquipmentData();
        }
        
        // 解析数据字符串（格式：level:count:effect|hasSpecial|effect1:level1:special1;effect2:level2:special2）
        String[] parts = data.split("\\|", -1);
        
        EquipmentData equipmentData = new EquipmentData();
        
        // 解析基础数据
        if (parts.length > 0) {
            String[] basic = parts[0].split(":");
            equipmentData.setForgeLevel(basic.length > 0 ? Integer.parseInt(basic[0]) : 0);
            equipmentData.setForgeCount(basic.length > 1 ? Integer.parseInt(basic[1]) : 0);
            if (basic.length > 2 && !basic[2].isEmpty()) {
                equipmentData.setAdditionalEffect(basic[2]);
            }
        }
        
        // 解析 hasSpecial 标志
        if (parts.length > 1 && !parts[1].isEmpty()) {
            equipmentData.setHasSpecialEffect(Boolean.parseBoolean(parts[1]));
        }
        
        // 解析药水效果
        if (parts.length > 2 && !parts[2].isEmpty()) {
            List<PotionEffectData> effects = new ArrayList<>();
            String[] effectStrings = parts[2].split(";");
            for (String effectStr : effectStrings) {
                if (!effectStr.isEmpty()) {
                    String[] effectParts = effectStr.split(":");
                    if (effectParts.length >= 3) {
                        String effectName = effectParts[0];
                        int level = Integer.parseInt(effectParts[1]);
                        boolean isSpecial = Boolean.parseBoolean(effectParts[2]);
                        boolean isNegative;
                        if (effectParts.length >= 4) {
                            isNegative = Boolean.parseBoolean(effectParts[3]);
                        } else {
                            isNegative = potionEffectManager.isNegativeEffect(effectName);
                        }
                        effects.add(new PotionEffectData(effectName, level, isSpecial, isNegative));
                    }
                }
            }
            equipmentData.setPotionEffects(effects);
        }
        
        // 解析装备属性（第4部分）
        if (parts.length > 3 && !parts[3].isEmpty()) {
            EquipmentAttributes attrs = new EquipmentAttributes();
            String[] attrPairs = parts[3].split(";");
            for (String pair : attrPairs) {
                if (!pair.isEmpty()) {
                    String[] kv = pair.split(":");
                    if (kv.length == 2) {
                        String attrKey = kv[0];
                        int value = Integer.parseInt(kv[1]);
                        switch (attrKey) {
                            case "maxDurability":
                                attrs.setMaxDurability(value);
                                break;
                            case "miningSpeed":
                                attrs.setMiningSpeed(value);
                                break;
                            case "armorToughness":
                                attrs.setArmorToughness(value);
                                break;
                            case "armorValue":
                                attrs.setArmorValue(value);
                                break;
                            case "attackSpeed":
                                attrs.setAttackSpeed(value);
                                break;
                            case "baseDamage":
                                attrs.setBaseDamage(value);
                                break;
                        }
                    }
                }
            }
            equipmentData.setAttributes(attrs);
        }
        
        return equipmentData;
    }
    
    /**
     * 将装备数据保存到 NBT
     */
    private void saveEquipmentData(ItemStack equipment, EquipmentData data) {
        ItemMeta meta = equipment.getItemMeta();
        if (meta == null) {
            return;
        }
        
        NamespacedKey key = new NamespacedKey(plugin, EQUIPMENT_DATA_KEY);
        
        // 格式：level:count:effect|hasSpecial|effect1:level1:special1;effect2:level2:special2|attr1:value1;attr2:value2
        StringBuilder dataBuilder = new StringBuilder();
        
        // 基础数据
        dataBuilder.append(data.getForgeLevel())
                   .append(":")
                   .append(data.getForgeCount())
                   .append(":")
                   .append(data.getAdditionalEffect() != null ? data.getAdditionalEffect() : "");
        
        // 是否有特殊效果标志
        dataBuilder.append("|").append(data.hasSpecialEffect());
        
        // 药水效果
        dataBuilder.append("|");
        List<PotionEffectData> effects = data.getPotionEffects();
        for (int i = 0; i < effects.size(); i++) {
            PotionEffectData effect = effects.get(i);
            dataBuilder.append(effect.getEffectName())
                       .append(":")
                       .append(effect.getLevel())
                       .append(":")
                       .append(effect.isSpecial());
            if (i < effects.size() - 1) {
                dataBuilder.append(";");
            }
        }
        
        // 装备属性
        dataBuilder.append("|");
        EquipmentAttributes attrs = data.getAttributes();
        if (attrs != null && attrs.hasAnyAttribute()) {
            boolean first = true;
            if (attrs.getMaxDurability() != null) {
                dataBuilder.append("maxDurability:").append(attrs.getMaxDurability());
                first = false;
            }
            if (attrs.getMiningSpeed() != null) {
                if (!first) dataBuilder.append(";");
                dataBuilder.append("miningSpeed:").append(attrs.getMiningSpeed());
                first = false;
            }
            if (attrs.getArmorToughness() != null) {
                if (!first) dataBuilder.append(";");
                dataBuilder.append("armorToughness:").append(attrs.getArmorToughness());
                first = false;
            }
            if (attrs.getArmorValue() != null) {
                if (!first) dataBuilder.append(";");
                dataBuilder.append("armorValue:").append(attrs.getArmorValue());
                first = false;
            }
            if (attrs.getAttackSpeed() != null) {
                if (!first) dataBuilder.append(";");
                dataBuilder.append("attackSpeed:").append(attrs.getAttackSpeed());
                first = false;
            }
            if (attrs.getBaseDamage() != null) {
                if (!first) dataBuilder.append(";");
                dataBuilder.append("baseDamage:").append(attrs.getBaseDamage());
            }
        }
        
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, dataBuilder.toString());
        
        equipment.setItemMeta(meta);
    }
    
    /**
     * 更新装备 Lore 以显示锻造信息
     */
    private void updateEquipmentLore(ItemStack equipment, EquipmentData data) {
        ItemMeta meta = equipment.getItemMeta();
        if (meta == null) {
            return;
        }
            
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
            
        // 使用分隔符定位，删除整个 YinwuForge 区域
        // 找到第一个和第二个分隔符的位置（一对分隔符）
        String separator = ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━";
        int firstSeparatorIndex = -1;
        int secondSeparatorIndex = -1;
            
        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).equals(separator)) {
                if (firstSeparatorIndex == -1) {
                    firstSeparatorIndex = i;
                } else if (secondSeparatorIndex == -1) {
                    secondSeparatorIndex = i;
                    break; // 找到一对分隔符就停止
                }
            }
        }
            
        // 如果找到了分隔符对，删除它们之间的所有内容（包括分隔符本身）
        if (firstSeparatorIndex != -1 && secondSeparatorIndex != -1) {
            lore.subList(firstSeparatorIndex, secondSeparatorIndex + 1).clear();
        } else if (firstSeparatorIndex != -1) {
            // 如果只有一个分隔符（异常情况），删除从该位置到末尾的所有内容
            lore.subList(firstSeparatorIndex, lore.size()).clear();
        }
            
        // 添加锻造信息部分
        lore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━");
        lore.add(ChatColor.YELLOW + "锻造次数: " + ChatColor.WHITE + data.getForgeCount());
        if (data.getAdditionalEffect() != null) {
            lore.add(ChatColor.AQUA + "附加效果: " + ChatColor.WHITE + data.getAdditionalEffect());
        }
            
        // 添加药水效果部分
        List<PotionEffectData> effects = data.getPotionEffects();
        if (!effects.isEmpty()) {
            lore.add(ChatColor.LIGHT_PURPLE + "药水效果:");
            for (PotionEffectData effect : effects) {
                String levelText = "I".repeat(effect.getLevel());
                ChatColor color;
                if (effect.isSpecial()) {
                    color = ChatColor.DARK_PURPLE;
                } else if (effect.isNegative()) {
                    color = ChatColor.RED;
                } else {
                    color = ChatColor.GREEN;
                }
                String chineseName = potionEffectManager.getChineseName(effect.getEffectName());
                lore.add(color + "  " + chineseName + " " + levelText);
            }
                
            if (data.hasSpecialEffect()) {
                lore.add(ChatColor.RED + "⚠ 无法继续锻造");
            }
        }
            
        // 添加装备属性部分
        EquipmentAttributes attrs = data.getAttributes();
        
        // 先判断类型
        Material itemType = equipment.getType();
        boolean hasForgeData = attrs != null && attrs.hasAnyAttribute();
        
        // 只有有锻造属性加成才显示装备属性区块
        // 武器/盔甲/鞘翅的原版属性提示由 MC 客户端自动显示
        if (hasForgeData) {
            lore.add(ChatColor.WHITE + "装备属性:");
            if (attrs != null) {
                addAttributeToLore(lore, "最大耐久", attrs.getMaxDurability());
                addAttributeToLore(lore, "挖掘速度", attrs.getMiningSpeed());
            }
        }
            
        lore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━");
            
        meta.setLore(lore);
        equipment.setItemMeta(meta);
    }
    
    /**
     * 将属性添加到 Lore（辅助方法）
     */
    private void addAttributeToLore(List<String> lore, String name, Integer value) {
        if (value != null) {
            String sign = value > 0 ? "+" : "";
            lore.add(ChatColor.GREEN + "  " + name + ": " + sign + value);
        }
    }
    
    /**
     * 执行合金锻造（属性修改）
     */
    private ForgeResult executeAlloyForge(Player player, ItemStack equipment, ItemStack material) {
        return executeAlloyForge(player, equipment, material, 0.0, 0.0);
    }
    
    private ForgeResult executeAlloyForge(Player player, ItemStack equipment, ItemStack material, 
                                         double successBonus, double failReduction) {
        // 检查装备类型是否支持合金锻造
        EquipmentType equipType = getEquipmentType(equipment);
        if (equipType == EquipmentType.OTHER) {
            player.sendMessage(ChatColor.RED + "该物品无法进行合金锻造！");
            player.sendMessage(ChatColor.GRAY + "仅支持工具、武器、盔甲和鞘翅");
            return null;
        }
        
        // 获取或创建装备数据
        EquipmentData equipmentData = getEquipmentData(equipment);
        
        // 根据合金锻造概率确定结果（带加成）
        ForgeResult result = determineAlloyForgeResult(successBonus, failReduction);
        
        // 应用合金锻造结果
        applyAlloyForgeResult(player, equipment, equipmentData, result);
        
        // 消耗一个材料
        consumeMaterial(player, material);
        
        // 设置冷却
        playerCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        
        return result;
    }
    
    /**
     * 检查是否为合金材料（下界合金锭）
     */
    private boolean isAlloyMaterial(ItemStack material) {
        if (!alloyForgeConfig.isEnabled()) {
            return false;
        }
        
        // 检查材质类型
        String alloyMaterial = alloyForgeConfig.getMaterialType();
        if (!material.getType().name().equals(alloyMaterial)) {
            return false;
        }
        
        // 如果配置了自定义名称，则检查显示名称
        String customName = alloyForgeConfig.getCustomName();
        if (customName != null && !customName.isEmpty()) {
            ItemMeta meta = material.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) {
                return false;
            }
            // 比较显示名称（支持颜色代码）
            return meta.getDisplayName().equals(customName);
        }
        
        return true;
    }
    
    /**
     * 应用合金锻造结果
     */
    private void applyAlloyForgeResult(Player player, ItemStack equipment, EquipmentData equipmentData, ForgeResult result) {
        EquipmentAttributes attributes = equipmentData.getAttributes();
        
        // 判断装备类型
        EquipmentType equipType = getEquipmentType(equipment);
        
        switch (result) {
            case FAIL_NO_PENALTY:
                // 无惩罚：不修改属性
                player.sendMessage(result.getFullMessage());
                player.sendMessage(ChatColor.GRAY + "装备属性未改变");
                updateEquipmentLore(equipment, equipmentData);
                applyAttributesToItem(equipment, attributes);
                saveEquipmentData(equipment, equipmentData);
                break;
                
            case EQUIPMENT_DESTROYED:
                // 失败：摧毁装备
                player.getInventory().setItemInMainHand(null);
                player.sendMessage(result.getFullMessage());
                player.sendMessage(ChatColor.RED + "装备已损毁！");
                break;
                
            case DOWNGRADE:
                // 降级：减少属性值
                applyAlloyDowngrade(player, equipment, equipmentData, attributes, equipType);
                break;
                
            case SUCCESS:
                // 成功：增加属性值（计入锻造次数）
                equipmentData.incrementForgeCount();
                applyAlloySuccess(player, equipment, equipmentData, attributes, equipType);
                break;
                
            case PERFECT:
                // 极品：大幅增加属性值（计入锻造次数）
                equipmentData.incrementForgeCount();
                applyAlloyPerfect(player, equipment, equipmentData, attributes, equipType);
                break;
        }
    }
    
    /**
     * 获取装备类型
     */
    private EquipmentType getEquipmentType(ItemStack equipment) {
        Material type = equipment.getType();
        
        // 如果启用了白名单模式，使用白名单判断
        if (whitelistEnabled) {
            if (type == Material.ELYTRA) {
                return EquipmentType.ELYTRA;
            }
            if (toolWhitelist.contains(type)) {
                return EquipmentType.TOOL;
            }
            if (weaponWhitelist.contains(type)) {
                return EquipmentType.WEAPON;
            }
            if (armorWhitelist.contains(type)) {
                return EquipmentType.ARMOR;
            }
            return EquipmentType.OTHER;
        }
        
        // 默认分类逻辑
        // 鞘翅
        if (type == Material.ELYTRA) {
            return EquipmentType.ELYTRA;
        }
        
        // 工具类
        if (type.name().endsWith("_PICKAXE") || type.name().endsWith("_AXE") || 
            type.name().endsWith("_SHOVEL") || type.name().endsWith("_HOE")) {
            return EquipmentType.TOOL;
        }
        
        // 武器类
        if (type.name().endsWith("_SWORD") || type == Material.BOW || 
            type == Material.CROSSBOW || type == Material.TRIDENT) {
            return EquipmentType.WEAPON;
        }
        
        // 盔甲类
        if (type.name().endsWith("_HELMET") || type.name().endsWith("_CHESTPLATE") || 
            type.name().endsWith("_LEGGINGS") || type.name().endsWith("_BOOTS")) {
            return EquipmentType.ARMOR;
        }
        
        return EquipmentType.OTHER;
    }
    
    /**
     * 应用合金降级效果
     */
    private void applyAlloyDowngrade(Player player, ItemStack equipment, EquipmentData equipmentData, 
                                     EquipmentAttributes attributes, EquipmentType equipType) {
        // 随机选择一个属性减少
        List<String> applicableAttrs = getApplicableAttributes(equipType);
        if (applicableAttrs.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "锻造失败 - 装备等级降低！");
            player.sendMessage(ChatColor.GRAY + "该装备无可降低的属性");
            applyAttributesToItem(equipment, attributes);
            updateEquipmentLore(equipment, equipmentData);
            saveEquipmentData(equipment, equipmentData);
            return;
        }
        
        String selectedAttr = applicableAttrs.get(random.nextInt(applicableAttrs.size()));
        int reduction = alloyForgeConfig.getRandomDowngradeValue(random);
        modifyAttribute(player, attributes, selectedAttr, -reduction, true);
        
        updateEquipmentLore(equipment, equipmentData);
        applyAttributesToItem(equipment, attributes);
        saveEquipmentData(equipment, equipmentData);
    }
    
    /**
     * 应用合金成功效果
     */
    private void applyAlloySuccess(Player player, ItemStack equipment, EquipmentData equipmentData,
                                   EquipmentAttributes attributes, EquipmentType equipType) {
        List<String> applicableAttrs = getApplicableAttributes(equipType);
        if (applicableAttrs.isEmpty()) {
            player.sendMessage(ChatColor.GREEN + "锻造成功 - 装备等级提升！");
            player.sendMessage(ChatColor.GRAY + "该装备无可提升的属性");
            updateEquipmentLore(equipment, equipmentData);
            applyAttributesToItem(equipment, attributes);
            saveEquipmentData(equipment, equipmentData);
            return;
        }
        
        String selectedAttr = applicableAttrs.get(random.nextInt(applicableAttrs.size()));
        int increase = alloyForgeConfig.getRandomSuccessValue(random);
        modifyAttribute(player, attributes, selectedAttr, increase, false);
        
        updateEquipmentLore(equipment, equipmentData);
        applyAttributesToItem(equipment, attributes);
        saveEquipmentData(equipment, equipmentData);
    }
    
    /**
     * 应用合金极品效果
     */
    private void applyAlloyPerfect(Player player, ItemStack equipment, EquipmentData equipmentData,
                                   EquipmentAttributes attributes, EquipmentType equipType) {
        List<String> applicableAttrs = getApplicableAttributes(equipType);
        if (applicableAttrs.isEmpty()) {
            player.sendMessage(ChatColor.AQUA + "完美锻造 - 获得极品属性！");
            player.sendMessage(ChatColor.GRAY + "该装备无可提升的属性");
            updateEquipmentLore(equipment, equipmentData);
            applyAttributesToItem(equipment, attributes);
            saveEquipmentData(equipment, equipmentData);
            return;
        }
        
        java.util.Collections.shuffle(applicableAttrs);
        int count = Math.min(2, applicableAttrs.size());
        
        player.sendMessage(ChatColor.AQUA + "完美锻造 - 获得极品属性！");
        
        for (int i = 0; i < count; i++) {
            String attr = applicableAttrs.get(i);
            int increase = alloyForgeConfig.getRandomPerfectValue(random);
            modifyAttribute(player, attributes, attr, increase, false, false);
        }
        
        updateEquipmentLore(equipment, equipmentData);
        applyAttributesToItem(equipment, attributes);
        saveEquipmentData(equipment, equipmentData);
    }
    
    /**
     * 修改装备属性（通用方法）
     * @param player 玩家
     * @param attributes 属性对象
     * @param attrName 属性名称
     * @param value 修改值（正数增加，负数减少）
     * @param isDowngrade 是否是降级操作
     */
    private void modifyAttribute(Player player, EquipmentAttributes attributes, String attrName, 
                                int value, boolean isDowngrade) {
        modifyAttribute(player, attributes, attrName, value, isDowngrade, true);
    }
    
    /**
     * 修改装备属性（通用方法）
     * @param player 玩家
     * @param attributes 属性对象
     * @param attrName 属性名称
     * @param value 修改值（正数增加，负数减少）
     * @param isDowngrade 是否是降级操作
     * @param sendHeader 是否发送标题消息
     */
    private void modifyAttribute(Player player, EquipmentAttributes attributes, String attrName, 
                                int value, boolean isDowngrade, boolean sendHeader) {
        String displayName = alloyForgeConfig.getAttributeName(attrName);
        String prefix = isDowngrade ? "-" : "+";
        ChatColor valueColor = isDowngrade ? ChatColor.RED : ChatColor.AQUA;
        
        if (sendHeader) {
            String header = isDowngrade 
                ? ChatColor.YELLOW + "锻造失败 - 装备等级降低！"
                : ChatColor.GREEN + "锻造成功 - 装备等级提升！";
            player.sendMessage(header);
        }
        
        switch (attrName) {
            case ATTR_MAX_DURABILITY:
                attributes.setMaxDurability(attributes.getMaxDurabilityOrDefault() + value);
                break;
            case ATTR_MINING_SPEED:
                attributes.setMiningSpeed(attributes.getMiningSpeedOrDefault() + value);
                break;
            case ATTR_ARMOR_TOUGHNESS:
                attributes.setArmorToughness(attributes.getArmorToughnessOrDefault() + value);
                break;
            case ATTR_ARMOR_VALUE:
                attributes.setArmorValue(attributes.getArmorValueOrDefault() + value);
                break;
            case ATTR_ATTACK_SPEED:
                attributes.setAttackSpeed(attributes.getAttackSpeedOrDefault() + value);
                break;
            case ATTR_BASE_DAMAGE:
                attributes.setBaseDamage(attributes.getBaseDamageOrDefault() + value);
                break;
        }
        
        player.sendMessage(valueColor + displayName + " " + prefix + Math.abs(value));
    }
    
    /**
     * 获取适用于该装备类型的属性列表
     */
    private List<String> getApplicableAttributes(EquipmentType equipType) {
        String typeName;
        switch (equipType) {
            case TOOL:
                typeName = "tool";
                break;
            case WEAPON:
                typeName = "weapon";
                break;
            case ARMOR:
                typeName = "armor";
                break;
            case ELYTRA:
                typeName = "elytra";
                break;
            default:
                // 其他类型返回所有属性
                return new ArrayList<>(Arrays.asList(
                    "maxDurability", "miningSpeed", "armorToughness",
                    "armorValue", "attackSpeed", "baseDamage"
                ));
        }
        
        return alloyForgeConfig.getAttributesForType(typeName);
    }
    
    /**
     * 装备类型枚举
     */
    private enum EquipmentType {
        TOOL,      // 工具
        WEAPON,    // 武器
        ARMOR,     // 盔甲
        ELYTRA,    // 鞘翅
        OTHER      // 其他
    }
    
    /**
     * 立即应用属性到物品（锻造后调用）
     */
    private void applyAttributesToItem(ItemStack equipment) {
        if (configManager.getBoolean("debug")) plugin.getLogger().info("[锻造后应用属性] 开始处理物品: " + equipment.getType());
        
        if (equipment == null || equipment.getType().isAir()) {
            if (configManager.getBoolean("debug")) plugin.getLogger().info("[锻造后应用属性] 物品为空或空气");
            return;
        }
        
        EquipmentData equipmentData = getEquipmentData(equipment);
        applyAttributesToItem(equipment, equipmentData.getAttributes());
    }
    
    /**
     * 应用锻造属性到物品（硬编码基础值 + 锻造加成一次性设置）
     * <p>
     * 对于硬编码表中的武器（剑、斧、三叉戟、重锤），始终设置伤害/攻速修饰符，
     * 即使当前锻造没有加成这些属性，也设置基础值，确保 Lore 始终正确显示。
     */
    private void applyAttributesToItem(ItemStack equipment, EquipmentAttributes attributes) {
        if (equipment == null || equipment.getType().isAir()) {
            return;
        }
        
        ItemMeta meta = equipment.getItemMeta();
        if (meta == null) return;
        
        Material material = equipment.getType();
        
        // 获取当前已有修饰符
        Multimap<Attribute, AttributeModifier> allModifiers = meta.getAttributeModifiers();
        if (allModifiers == null) {
            allModifiers = ArrayListMultimap.create();
        } else {
            allModifiers = ArrayListMultimap.create(allModifiers);
        }
        
        // 移除所有旧的 yinwu 自定义修饰符
        List<Map.Entry<Attribute, AttributeModifier>> toRemove = new ArrayList<>();
        for (Map.Entry<Attribute, AttributeModifier> entry : allModifiers.entries()) {
            if ("yinwu".equals(entry.getValue().getKey().getNamespace())) {
                toRemove.add(entry);
            }
        }
        for (Map.Entry<Attribute, AttributeModifier> entry : toRemove) {
            allModifiers.remove(entry.getKey(), entry.getValue());
        }
        
        // ===== 始终设置武器伤害/攻速（基础值 + 锻造加成，如无加成为0）=====
        // ATTACK_DAMAGE：修饰符值 = 武器基础伤害贡献 + 锻造加成
        // 最终伤害 = 空手1 + 修饰符值
        Double baseDamage = WEAPON_BASE_DAMAGE.get(material);
        if (baseDamage != null) {
            double forgeBonus = (attributes != null && attributes.getBaseDamage() != null) ? attributes.getBaseDamage() : 0;
            allModifiers.put(Attribute.ATTACK_DAMAGE, new AttributeModifier(
                new NamespacedKey("yinwu", "yinwu_damage"),
                baseDamage + forgeBonus,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND
            ));
        }
        
        // ATTACK_SPEED：修饰符值 = (基础攻速 - 4.0) + 锻造加成 * 0.2
        // 最终攻速 = 4.0 + 修饰符值
        Double baseSpeed = WEAPON_BASE_SPEED.get(material);
        if (baseSpeed != null) {
            double forgeBonus = (attributes != null && attributes.getAttackSpeed() != null) ? attributes.getAttackSpeed() * 0.2 : 0;
            allModifiers.put(Attribute.ATTACK_SPEED, new AttributeModifier(
                new NamespacedKey("yinwu", "yinwu_attack_speed"),
                (baseSpeed - BASE_PLAYER_ATTACK_SPEED) + forgeBonus,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND
            ));
        }
        
        // ===== 盔甲：始终设置总盔甲值/韧性（基础值 + 锻造加成，如无加成为0）=====
        if (isArmorType(material)) {
            EquipmentSlotGroup slotGroup = getArmorSlotGroup(material);
            Double baseArmor = ARMOR_BASE_VALUES.get(material);
            if (baseArmor == null) baseArmor = 0.0;
            double forgeArmor = (attributes != null && attributes.getArmorValue() != null) ? attributes.getArmorValue() : 0;
            allModifiers.put(Attribute.ARMOR, new AttributeModifier(
                new NamespacedKey("yinwu", "yinwu_armor"),
                baseArmor + forgeArmor,
                AttributeModifier.Operation.ADD_NUMBER,
                slotGroup
            ));
            
            Double baseToughness = ARMOR_TOUGHNESS_BASE_VALUES.get(material);
            if (baseToughness == null) baseToughness = 0.0;
            double forgeToughness = (attributes != null && attributes.getArmorToughness() != null) ? attributes.getArmorToughness() : 0;
            allModifiers.put(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(
                new NamespacedKey("yinwu", "yinwu_armor_toughness"),
                baseToughness + forgeToughness,
                AttributeModifier.Operation.ADD_NUMBER,
                slotGroup
            ));
            
            // 击退抗性（仅下界合金系列有基础值）
            Double baseKB = KNOCKBACK_RESISTANCE_BASE_VALUES.get(material);
            if (baseKB != null && baseKB > 0) {
                allModifiers.put(Attribute.KNOCKBACK_RESISTANCE, new AttributeModifier(
                    new NamespacedKey("yinwu", "yinwu_knockback_resistance"),
                    baseKB,
                    AttributeModifier.Operation.ADD_NUMBER,
                    slotGroup
                ));
            }
        }
        
        // ===== 锻造可选的属性加成（仅在锻造roll到时设置）=====
        if (attributes != null) {
            // 挖掘速度（工具专用）
            if (attributes.getMiningSpeed() != null && attributes.getMiningSpeed() != 0) {
                allModifiers.put(Attribute.BLOCK_BREAK_SPEED, new AttributeModifier(
                    new NamespacedKey("yinwu", "yinwu_mining_speed"),
                    attributes.getMiningSpeed().doubleValue() * 0.2,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1,
                    EquipmentSlotGroup.MAINHAND
                ));
            }
        }
        
        // 一次性设置完整修饰符集合
        meta.setAttributeModifiers(allModifiers);
        
        // 处理耐久度
        if (attributes != null && attributes.getMaxDurability() != null && attributes.getMaxDurability() != 0) {
            if (meta instanceof Damageable damageable) {
                int baseMaxDurability = getDefaultMaxDurability(equipment);
                if (baseMaxDurability > 0) {
                    damageable.setMaxDamage(baseMaxDurability + (attributes.getMaxDurability() * 150));
                    damageable.setDamage(0);
                }
            }
        }
        
        equipment.setItemMeta(meta);
    }
    

    /**
     * 获取武器的默认基础伤害值（不包括空手伤害）
     * @param material 物品材质
     * @return 默认基础伤害值
     */
    private double getDefaultAttackDamage(Material material) {
        return switch (material) {
            case NETHERITE_SWORD -> 8.0;
            case DIAMOND_SWORD -> 7.0;
            case IRON_SWORD -> 6.0;
            case STONE_SWORD -> 5.0;
            case WOODEN_SWORD -> 4.0;
            case GOLDEN_SWORD -> 4.0;
            case NETHERITE_AXE -> 10.0;
            case DIAMOND_AXE -> 9.0;
            case IRON_AXE -> 7.0;
            case STONE_AXE -> 6.0;
            case WOODEN_AXE -> 5.0;
            case GOLDEN_AXE -> 5.0;
            case TRIDENT -> 9.0;
            default -> 1.0;
        };
    }
    
    /**
     * 获取物品类型的默认最大耐久值
     * @param item 物品栈
     * @return 默认最大耐久值
     */
    private int getDefaultMaxDurability(ItemStack item) {
        Material material = item.getType();
        ItemStack defaultItem = new ItemStack(material);
        ItemMeta defaultMeta = defaultItem.getItemMeta();
        
        if (defaultMeta instanceof Damageable defaultDamageable && defaultDamageable.hasMaxDamage()) {
            return defaultDamageable.getMaxDamage();
        }
        
        return switch (material) {
            case NETHERITE_PICKAXE, NETHERITE_AXE, NETHERITE_SWORD, NETHERITE_SHOVEL, NETHERITE_HOE,
                 NETHERITE_HELMET, NETHERITE_CHESTPLATE, NETHERITE_LEGGINGS, NETHERITE_BOOTS -> 2031;
            case DIAMOND_PICKAXE, DIAMOND_AXE, DIAMOND_SWORD, DIAMOND_SHOVEL, DIAMOND_HOE,
                 DIAMOND_HELMET, DIAMOND_CHESTPLATE, DIAMOND_LEGGINGS, DIAMOND_BOOTS -> 1561;
            case IRON_PICKAXE, IRON_AXE, IRON_SWORD, IRON_SHOVEL, IRON_HOE,
                 IRON_HELMET, IRON_CHESTPLATE, IRON_LEGGINGS, IRON_BOOTS -> 250;
            case STONE_PICKAXE, STONE_AXE, STONE_SWORD, STONE_SHOVEL, STONE_HOE -> 131;
            case WOODEN_PICKAXE, WOODEN_AXE, WOODEN_SWORD, WOODEN_SHOVEL, WOODEN_HOE,
                 LEATHER_HELMET, LEATHER_CHESTPLATE, LEATHER_LEGGINGS, LEATHER_BOOTS -> 60;
            case GOLDEN_PICKAXE, GOLDEN_AXE, GOLDEN_SWORD, GOLDEN_SHOVEL, GOLDEN_HOE,
                 GOLDEN_HELMET, GOLDEN_CHESTPLATE, GOLDEN_LEGGINGS, GOLDEN_BOOTS -> 33;
            default -> 1561;
        };
    }
    
    /**
     * 判断是否为盔甲类型
     */
    private boolean isArmorType(Material material) {
        if (material == Material.ELYTRA) return true;
        String name = material.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || 
               name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }
    
    /**
     * 从全新同类型物品读取默认基础属性值
     */
    private double getBaseAttributeValue(Material material, Attribute attribute) {
        ItemStack defaultItem = new ItemStack(material);
        ItemMeta defaultMeta = defaultItem.getItemMeta();
        if (defaultMeta != null) {
            Collection<AttributeModifier> mods = defaultMeta.getAttributeModifiers(attribute);
            if (mods != null) {
                double total = 0;
                for (AttributeModifier mod : mods) {
                    total += mod.getAmount();
                }
                return total;
            }
        }
        return 0;
    }
    
    /**
     * 根据盔甲材质返回对应的 EquipmentSlotGroup
     */
    public static EquipmentSlotGroup getArmorSlotGroup(Material material) {
        String name = material.name();
        if (name.endsWith("_HELMET") || material == Material.TURTLE_HELMET) return EquipmentSlotGroup.HEAD;
        if (name.endsWith("_CHESTPLATE")) return EquipmentSlotGroup.CHEST;
        if (name.endsWith("_LEGGINGS")) return EquipmentSlotGroup.LEGS;
        if (name.endsWith("_BOOTS")) return EquipmentSlotGroup.FEET;
        if (material == Material.ELYTRA) return EquipmentSlotGroup.CHEST;
        return EquipmentSlotGroup.ANY;
    }
    
    /**
     * 格式化 double 值（去掉无意义的小数零）
     */
    private String formatDouble(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format("%.1f", value);
    }
    
    /**
     * 根据盔甲部位返回对应的原版显示文本
     */
    private String getArmorSlotDisplay(Material material) {
        String name = material.name();
        if (name.endsWith("_HELMET")) return "戴在头上时";
        if (name.endsWith("_CHESTPLATE")) return "穿在身上时";
        if (name.endsWith("_LEGGINGS")) return "穿在腿上时";
        if (name.endsWith("_BOOTS")) return "穿在脚上时";
        return null;
    }

    /**
     * 执行分类锻造（GUI锻造入口，根据浓缩材料的分类）
     */
    public ForgeResult executeCategoryForge(Player player, ItemStack equipment, ItemStack strengthMat,
                                            ItemStack adjusterMat, String adjusterCategory,
                                            double netSuccessBonus, double netDestroyReduction) {
        if (equipment == null || equipment.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "没有要锻造的装备！");
            return null;
        }

        // 检查装备类型是否支持锻造
        EquipmentType equipType = getEquipmentType(equipment);
        if (equipType == EquipmentType.OTHER) {
            player.sendMessage(ChatColor.RED + "该物品无法进行锻造！");
            return null;
        }

        // 获取或创建装备数据
        EquipmentData equipmentData = getEquipmentData(equipment);

        // 确定锻造结果（使用合金锻造概率表）
        ForgeResult result = determineAdjustedAlloyForgeResult(netSuccessBonus, netDestroyReduction);

        // 应用合金锻造结果
        applyCategoryForgeResult(player, equipment, equipmentData, result);

        // 锻造完成后，立即应用属性到物品
        applyAttributesToItem(equipment);

        // 设置冷却
        playerCooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        return result;
    }

    /**
     * 带调整的概率判定（用于核心锻造）
     * 所有概率自动钳制在 0%~100%，溢出时按比例归一化
     */
    private ForgeResult determineAdjustedAlloyForgeResult(double netSuccessBonus, double netDestroyReduction) {
        double failNoPenalty = configManager.getAlloyForgeChance(CHANCE_FAIL_NO_PENALTY);
        double equipmentDestroyed = configManager.getAlloyForgeChance(CHANCE_EQUIPMENT_DESTROYED);
        double downgrade = configManager.getAlloyForgeChance(CHANCE_DOWNGRADE);
        double success = configManager.getAlloyForgeChance(CHANCE_SUCCESS);
        double perfect = configManager.getAlloyForgeChance("perfect");

        // 钳制基础值到 [0, 100]
        failNoPenalty = clamp(failNoPenalty, 0, 100);
        equipmentDestroyed = clamp(equipmentDestroyed, 0, 100);
        downgrade = clamp(downgrade, 0, 100);
        success = clamp(success, 0, 100);
        perfect = clamp(perfect, 0, 100);

        // 归一化基础值，保证加起来等于 100
        double[] baseProbs = normalizeProbs(new double[]{failNoPenalty, equipmentDestroyed, downgrade, success, perfect});
        failNoPenalty = baseProbs[0];
        equipmentDestroyed = baseProbs[1];
        downgrade = baseProbs[2];
        success = baseProbs[3];
        perfect = baseProbs[4];

        // 1) 应用净成功加成：从失败率中抽取，分配给 success + perfect
        if (netSuccessBonus != 0) {
            double grabFrom = failNoPenalty + equipmentDestroyed + downgrade;
            if (grabFrom > 0) {
                double actualBonus = clamp(netSuccessBonus, -grabFrom, grabFrom);
                // 按比例从失败率中扣除
                double ratio = actualBonus / grabFrom;
                failNoPenalty += failNoPenalty * ratio;
                equipmentDestroyed += equipmentDestroyed * ratio;
                downgrade += downgrade * ratio;
                // 加给 success 和 perfect（按原始比例）
                double successPool = success + perfect;
                if (successPool > 0) {
                    success += actualBonus * (success / successPool);
                    perfect += actualBonus * (perfect / successPool);
                } else {
                    success += actualBonus * 0.7;
                    perfect += actualBonus * 0.3;
                }
            }
        }

        // 2) 应用净损坏调整（正数=减少损坏，负数=增加损坏）
        if (netDestroyReduction != 0) {
            double totalBad = equipmentDestroyed + downgrade;
            if (totalBad > 0) {
                double adjustment = clamp(netDestroyReduction, -totalBad, totalBad);
                double destroyRatio = equipmentDestroyed / totalBad;
                double downgradeRatio = downgrade / totalBad;
                equipmentDestroyed -= adjustment * destroyRatio;
                downgrade -= adjustment * downgradeRatio;
                // 损坏减少的部分加到无惩罚上
                failNoPenalty += adjustment;
            }
        }

        // 3) 最终钳制 + 归一化，保证 5 个概率加起来 = 100
        double[] finalProbs = normalizeProbs(new double[]{failNoPenalty, equipmentDestroyed, downgrade, success, perfect});
        failNoPenalty = finalProbs[0];
        equipmentDestroyed = finalProbs[1];
        downgrade = finalProbs[2];
        success = finalProbs[3];
        perfect = finalProbs[4];

        double roll = random.nextDouble() * 100;

        if (roll < failNoPenalty) {
            return ForgeResult.FAIL_NO_PENALTY;
        } else if (roll < failNoPenalty + equipmentDestroyed) {
            return ForgeResult.EQUIPMENT_DESTROYED;
        } else if (roll < failNoPenalty + equipmentDestroyed + downgrade) {
            return ForgeResult.DOWNGRADE;
        } else if (roll < failNoPenalty + equipmentDestroyed + downgrade + success) {
            return ForgeResult.SUCCESS;
        } else {
            return ForgeResult.PERFECT;
        }
    }

    /**
     * 钳制值在 [min, max] 范围内
     */
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 将一组概率按比例归一化到总和 100
     * 所有值先钳制到 [0, 100]，然后再缩放
     */
    private double[] normalizeProbs(double[] probs) {
        double[] clamped = new double[probs.length];
        double sum = 0;
        for (int i = 0; i < probs.length; i++) {
            clamped[i] = clamp(probs[i], 0, 100);
            sum += clamped[i];
        }
        if (sum <= 0) {
            clamped[clamped.length - 1] = 100;
            return clamped;
        }
        double[] result = new double[probs.length];
        for (int i = 0; i < probs.length; i++) {
            result[i] = clamped[i] / sum * 100;
        }
        // 处理浮点误差，确保总和严格等于 100
        double resultSum = 0;
        for (double v : result) resultSum += v;
        double diff = 100 - resultSum;
        if (Math.abs(diff) > 0.001) {
            result[result.length - 1] += diff;
        }
        return result;
    }

    /**
     * 应用分类锻造结果
     */
    private void applyCategoryForgeResult(Player player, ItemStack equipment, EquipmentData equipmentData,
                                          ForgeResult result) {
        EquipmentAttributes attributes = equipmentData.getAttributes();
        EquipmentType equipType = getEquipmentType(equipment);

        switch (result) {
            case FAIL_NO_PENALTY:
                player.sendMessage(result.getFullMessage());
                player.sendMessage(ChatColor.GRAY + "装备属性未改变");
                updateEquipmentLore(equipment, equipmentData);
                applyAttributesToItem(equipment, attributes);
                saveEquipmentData(equipment, equipmentData);
                break;

            case EQUIPMENT_DESTROYED:
                player.sendMessage(result.getFullMessage());
                player.sendMessage(ChatColor.RED + "装备已损毁！");
                equipment.setAmount(0);
                break;

            case DOWNGRADE:
                applyAlloyDowngrade(player, equipment, equipmentData, attributes, equipType);
                break;

            case SUCCESS:
                // 成功（计入锻造次数）
                equipmentData.incrementForgeCount();
                applyAlloySuccess(player, equipment, equipmentData, attributes, equipType);
                break;

            case PERFECT:
                // 极品（计入锻造次数）
                equipmentData.incrementForgeCount();
                applyAlloyPerfect(player, equipment, equipmentData, attributes, equipType);
                break;
        }
    }

    /**
     * 检查装备类型是否匹配核心预期类型
     */
    public boolean isEquipmentTypeMatch(ItemStack equipment, String expectedType) {
        if (equipment == null || equipment.getType() == Material.AIR) return false;
        EquipmentType type = getEquipmentType(equipment);

        return switch (expectedType) {
            case "armor" -> type == EquipmentType.ARMOR;
            case "weapon" -> type == EquipmentType.WEAPON;
            case "tool" -> type == EquipmentType.TOOL;
            case "elytra" -> type == EquipmentType.ELYTRA;
            default -> false;
        };
    }

    /**
     * 获取装备的当前锻造次数
     */
    public int getEquipmentForgeCount(ItemStack equipment) {
        EquipmentData data = getEquipmentData(equipment);
        return data.getForgeCount();
    }

    /**
     * 设置玩家冷却（公开方法，供GUI使用）
     */
    public void setCooldown(Player player) {
        playerCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
}