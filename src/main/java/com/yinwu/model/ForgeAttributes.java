package com.yinwu.model;

import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlotGroup;
import java.util.List;
import java.util.Map;

/**
 * 锻造属性常量定义
 * <p>
 * 集中管理属性键、转换系数、装备槽位映射，避免 ForgeManager 中散落的魔幻字符串/数字。
 */
public final class ForgeAttributes {

    private ForgeAttributes() {}

    // ===== 属性键常量 =====
    public static final String ATTR_MAX_DURABILITY   = "maxDurability";
    public static final String ATTR_MINING_SPEED     = "miningSpeed";
    public static final String ATTR_ARMOR_TOUGHNESS  = "armorToughness";
    public static final String ATTR_ARMOR_VALUE      = "armorValue";
    public static final String ATTR_ATTACK_SPEED     = "attackSpeed";
    public static final String ATTR_BASE_DAMAGE      = "baseDamage";

    // ===== 转换系数（锻造点数 → 实际属性值） =====
    public static final double FACTOR_BASE_DAMAGE     = 0.4;
    public static final double FACTOR_ATTACK_SPEED    = 0.02;
    public static final double FACTOR_ARMOR_VALUE     = 0.15;
    public static final double FACTOR_ARMOR_TOUGHNESS = 0.05;
    public static final double FACTOR_MINING_SPEED    = 0.04;
    /** 每点锻造耐久加成 = 150 基础耐久 */
    public static final int    DURABILITY_PER_POINT   = 150;

    /** 默认系数映射（配置加载时的后备值） */
    public static final Map<String, Double> DEFAULT_FACTORS = Map.of(
        ATTR_BASE_DAMAGE, FACTOR_BASE_DAMAGE,
        ATTR_ATTACK_SPEED, FACTOR_ATTACK_SPEED,
        ATTR_ARMOR_VALUE, FACTOR_ARMOR_VALUE,
        ATTR_ARMOR_TOUGHNESS, FACTOR_ARMOR_TOUGHNESS,
        ATTR_MINING_SPEED, FACTOR_MINING_SPEED
    );

    // ===== 装备槽位 =====
    /** 武器 / 工具的固定主手槽位 */
    public static final EquipmentSlotGroup MAINHAND_SLOT = EquipmentSlotGroup.MAINHAND;

    // ===== 属性分类列表 =====
    /** 全部六项属性 */
    public static final List<String> ALL_ATTRIBUTES = List.of(
        ATTR_MAX_DURABILITY, ATTR_MINING_SPEED, ATTR_ARMOR_TOUGHNESS,
        ATTR_ARMOR_VALUE, ATTR_ATTACK_SPEED, ATTR_BASE_DAMAGE
    );

    /** 工具适用属性 */
    public static final List<String> TOOL_ATTRIBUTES   = List.of(ATTR_MAX_DURABILITY, ATTR_MINING_SPEED);
    /** 武器适用属性 */
    public static final List<String> WEAPON_ATTRIBUTES = List.of(ATTR_BASE_DAMAGE, ATTR_ATTACK_SPEED);
    /** 盔甲适用属性 */
    public static final List<String> ARMOR_ATTRIBUTES  = List.of(ATTR_ARMOR_VALUE, ATTR_ARMOR_TOUGHNESS);
    /** 鞘翅适用属性 */
    public static final List<String> ELYTRA_ATTRIBUTES = List.of(ATTR_MAX_DURABILITY);

    /**
     * 根据盔甲材料获取对应的装备槽位
     *
     * @param material 盔甲材料（如 DIAMOND_CHESTPLATE）
     * @return 对应槽位（HEAD / CHEST / LEGS / FEET / ANY）
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
     * 获取属性键对应的默认转换系数
     */
    public static double getFactor(String attrKey) {
        return DEFAULT_FACTORS.getOrDefault(attrKey, 1.0);
    }
}
