package com.yinwu.manager;

import org.bukkit.Material;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BaseWeaponStats {
    private BaseWeaponStats() {}

    private static final Map<Material, Double> WEAPON_BASE_DAMAGE_INNER = new HashMap<>();
    private static final Map<Material, Double> WEAPON_BASE_SPEED_INNER = new HashMap<>();
    private static final Map<Material, Double> ARMOR_BASE_VALUES_INNER = new HashMap<>();
    private static final Map<Material, Double> ARMOR_TOUGHNESS_BASE_VALUES_INNER = new HashMap<>();
    private static final Map<Material, Double> KNOCKBACK_RESISTANCE_BASE_VALUES_INNER = new HashMap<>();

    public static final Map<Material, Double> WEAPON_BASE_DAMAGE = Collections.unmodifiableMap(WEAPON_BASE_DAMAGE_INNER);
    public static final Map<Material, Double> WEAPON_BASE_SPEED = Collections.unmodifiableMap(WEAPON_BASE_SPEED_INNER);
    public static final Map<Material, Double> ARMOR_BASE_VALUES = Collections.unmodifiableMap(ARMOR_BASE_VALUES_INNER);
    public static final Map<Material, Double> ARMOR_TOUGHNESS_BASE_VALUES = Collections.unmodifiableMap(ARMOR_TOUGHNESS_BASE_VALUES_INNER);
    public static final Map<Material, Double> KNOCKBACK_RESISTANCE_BASE_VALUES = Collections.unmodifiableMap(KNOCKBACK_RESISTANCE_BASE_VALUES_INNER);
    public static final double BASE_PLAYER_ATTACK_SPEED = 4.0;

    static {
        // 剑类
        WEAPON_BASE_DAMAGE_INNER.put(Material.WOODEN_SWORD, 3.0);
        WEAPON_BASE_DAMAGE_INNER.put(Material.STONE_SWORD, 4.0);
        WEAPON_BASE_DAMAGE_INNER.put(Material.IRON_SWORD, 5.0);
        WEAPON_BASE_DAMAGE_INNER.put(Material.GOLDEN_SWORD, 3.0);
        WEAPON_BASE_DAMAGE_INNER.put(Material.DIAMOND_SWORD, 6.0);
        WEAPON_BASE_DAMAGE_INNER.put(Material.NETHERITE_SWORD, 7.0);
        for (Material sw : List.of(Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD)) {
            WEAPON_BASE_SPEED_INNER.put(sw, 1.6);
        }

        // 斧头类
        WEAPON_BASE_DAMAGE_INNER.put(Material.WOODEN_AXE, 6.0);
        WEAPON_BASE_DAMAGE_INNER.put(Material.STONE_AXE, 8.0);
        WEAPON_BASE_DAMAGE_INNER.put(Material.IRON_AXE, 8.0);
        WEAPON_BASE_DAMAGE_INNER.put(Material.GOLDEN_AXE, 6.0);
        WEAPON_BASE_DAMAGE_INNER.put(Material.DIAMOND_AXE, 8.0);
        WEAPON_BASE_DAMAGE_INNER.put(Material.NETHERITE_AXE, 9.0);
        WEAPON_BASE_SPEED_INNER.put(Material.WOODEN_AXE, 0.8);
        WEAPON_BASE_SPEED_INNER.put(Material.STONE_AXE, 0.8);
        WEAPON_BASE_SPEED_INNER.put(Material.IRON_AXE, 0.9);
        WEAPON_BASE_SPEED_INNER.put(Material.GOLDEN_AXE, 1.0);
        WEAPON_BASE_SPEED_INNER.put(Material.DIAMOND_AXE, 1.0);
        WEAPON_BASE_SPEED_INNER.put(Material.NETHERITE_AXE, 1.0);

        // 镐类
        WEAPON_BASE_DAMAGE_INNER.put(Material.WOODEN_PICKAXE, 1.0);
        WEAPON_BASE_DAMAGE_INNER.put(Material.STONE_PICKAXE, 2.0);
        WEAPON_BASE_DAMAGE_INNER.put(Material.IRON_PICKAXE, 3.0);
        WEAPON_BASE_DAMAGE_INNER.put(Material.GOLDEN_PICKAXE, 1.0);
        WEAPON_BASE_DAMAGE_INNER.put(Material.DIAMOND_PICKAXE, 4.0);
        WEAPON_BASE_DAMAGE_INNER.put(Material.NETHERITE_PICKAXE, 5.0);
        for (Material pk : List.of(Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
                Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE)) {
            WEAPON_BASE_SPEED_INNER.put(pk, 1.2);
        }

        // 铲类
        WEAPON_BASE_DAMAGE_INNER.put(Material.WOODEN_SHOVEL, 1.5);
        WEAPON_BASE_DAMAGE_INNER.put(Material.STONE_SHOVEL, 2.5);
        WEAPON_BASE_DAMAGE_INNER.put(Material.IRON_SHOVEL, 3.5);
        WEAPON_BASE_DAMAGE_INNER.put(Material.GOLDEN_SHOVEL, 1.5);
        WEAPON_BASE_DAMAGE_INNER.put(Material.DIAMOND_SHOVEL, 4.5);
        WEAPON_BASE_DAMAGE_INNER.put(Material.NETHERITE_SHOVEL, 5.5);
        for (Material sh : List.of(Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
                Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL)) {
            WEAPON_BASE_SPEED_INNER.put(sh, 1.0);
        }

        // 锄类
        WEAPON_BASE_DAMAGE_INNER.put(Material.WOODEN_HOE, 0.0);
        WEAPON_BASE_DAMAGE_INNER.put(Material.STONE_HOE, 0.0);
        WEAPON_BASE_DAMAGE_INNER.put(Material.IRON_HOE, 0.0);
        WEAPON_BASE_DAMAGE_INNER.put(Material.GOLDEN_HOE, 0.0);
        WEAPON_BASE_DAMAGE_INNER.put(Material.DIAMOND_HOE, 0.0);
        WEAPON_BASE_DAMAGE_INNER.put(Material.NETHERITE_HOE, 0.0);
        WEAPON_BASE_SPEED_INNER.put(Material.WOODEN_HOE, 1.0);
        WEAPON_BASE_SPEED_INNER.put(Material.STONE_HOE, 2.0);
        WEAPON_BASE_SPEED_INNER.put(Material.IRON_HOE, 3.0);
        WEAPON_BASE_SPEED_INNER.put(Material.GOLDEN_HOE, 1.0);
        WEAPON_BASE_SPEED_INNER.put(Material.DIAMOND_HOE, 4.0);
        WEAPON_BASE_SPEED_INNER.put(Material.NETHERITE_HOE, 4.0);

        // 特殊武器
        WEAPON_BASE_DAMAGE_INNER.put(Material.TRIDENT, 8.0);
        WEAPON_BASE_SPEED_INNER.put(Material.TRIDENT, 1.1);
        WEAPON_BASE_DAMAGE_INNER.put(Material.MACE, 5.0);
        WEAPON_BASE_SPEED_INNER.put(Material.MACE, 0.6);

        // 盔甲 - 头盔
        ARMOR_BASE_VALUES_INNER.put(Material.LEATHER_HELMET, 1.0);
        ARMOR_BASE_VALUES_INNER.put(Material.CHAINMAIL_HELMET, 2.0);
        ARMOR_BASE_VALUES_INNER.put(Material.IRON_HELMET, 2.0);
        ARMOR_BASE_VALUES_INNER.put(Material.GOLDEN_HELMET, 2.0);
        ARMOR_BASE_VALUES_INNER.put(Material.DIAMOND_HELMET, 3.0);
        ARMOR_BASE_VALUES_INNER.put(Material.NETHERITE_HELMET, 3.0);
        ARMOR_BASE_VALUES_INNER.put(Material.TURTLE_HELMET, 2.0);
        // 盔甲 - 胸甲
        ARMOR_BASE_VALUES_INNER.put(Material.LEATHER_CHESTPLATE, 3.0);
        ARMOR_BASE_VALUES_INNER.put(Material.CHAINMAIL_CHESTPLATE, 5.0);
        ARMOR_BASE_VALUES_INNER.put(Material.IRON_CHESTPLATE, 6.0);
        ARMOR_BASE_VALUES_INNER.put(Material.GOLDEN_CHESTPLATE, 5.0);
        ARMOR_BASE_VALUES_INNER.put(Material.DIAMOND_CHESTPLATE, 8.0);
        ARMOR_BASE_VALUES_INNER.put(Material.NETHERITE_CHESTPLATE, 8.0);
        // 盔甲 - 护腿
        ARMOR_BASE_VALUES_INNER.put(Material.LEATHER_LEGGINGS, 2.0);
        ARMOR_BASE_VALUES_INNER.put(Material.CHAINMAIL_LEGGINGS, 4.0);
        ARMOR_BASE_VALUES_INNER.put(Material.IRON_LEGGINGS, 5.0);
        ARMOR_BASE_VALUES_INNER.put(Material.GOLDEN_LEGGINGS, 3.0);
        ARMOR_BASE_VALUES_INNER.put(Material.DIAMOND_LEGGINGS, 6.0);
        ARMOR_BASE_VALUES_INNER.put(Material.NETHERITE_LEGGINGS, 6.0);
        // 盔甲 - 靴子
        ARMOR_BASE_VALUES_INNER.put(Material.LEATHER_BOOTS, 1.0);
        ARMOR_BASE_VALUES_INNER.put(Material.CHAINMAIL_BOOTS, 1.0);
        ARMOR_BASE_VALUES_INNER.put(Material.IRON_BOOTS, 2.0);
        ARMOR_BASE_VALUES_INNER.put(Material.GOLDEN_BOOTS, 1.0);
        ARMOR_BASE_VALUES_INNER.put(Material.DIAMOND_BOOTS, 3.0);
        ARMOR_BASE_VALUES_INNER.put(Material.NETHERITE_BOOTS, 3.0);

        // 护甲韧性（钻石+2，下界合金+3）
        for (Material mat : List.of(Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE,
                Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS)) {
            ARMOR_TOUGHNESS_BASE_VALUES_INNER.put(mat, 2.0);
        }
        for (Material mat : List.of(Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
                Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS)) {
            ARMOR_TOUGHNESS_BASE_VALUES_INNER.put(mat, 3.0);
        }

        // 击退抗性（仅下界合金系列，每件+0.1）
        for (Material mat : List.of(Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
                Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS)) {
            KNOCKBACK_RESISTANCE_BASE_VALUES_INNER.put(mat, 0.1);
        }

        // 铜盔甲（动态加载）
        for (String copper : List.of("COPPER_HELMET|2", "COPPER_CHESTPLATE|4", "COPPER_LEGGINGS|3", "COPPER_BOOTS|1")) {
            String[] parts = copper.split("\\|");
            Material mat = Material.matchMaterial(parts[0]);
            if (mat != null) {
                ARMOR_BASE_VALUES_INNER.put(mat, Double.parseDouble(parts[1]));
            }
        }
    }
}
