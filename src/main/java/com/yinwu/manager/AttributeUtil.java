package com.yinwu.manager;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AttributeUtil {

    private AttributeUtil() {}

    public static boolean isArmorType(Material material) {
        if (material == Material.ELYTRA) return true;
        String name = material.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") ||
               name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }

    public static EquipmentSlotGroup getArmorSlotGroup(Material material) {
        String name = material.name();
        if (name.endsWith("_HELMET") || material == Material.TURTLE_HELMET) return EquipmentSlotGroup.HEAD;
        if (name.endsWith("_CHESTPLATE")) return EquipmentSlotGroup.CHEST;
        if (name.endsWith("_LEGGINGS")) return EquipmentSlotGroup.LEGS;
        if (name.endsWith("_BOOTS")) return EquipmentSlotGroup.FEET;
        if (material == Material.ELYTRA) return EquipmentSlotGroup.CHEST;
        return EquipmentSlotGroup.ANY;
    }

    public static int getDefaultMaxDurability(ItemStack item) {
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

    public static void applyMaxDurability(ItemStack item, int durabilityBonus) {
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return;

        int baseMaxDurability = getDefaultMaxDurability(item);
        if (baseMaxDurability <= 0 || !damageable.hasMaxDamage()) return;

        int newMaxDurability = baseMaxDurability + (durabilityBonus * 150);
        damageable.setMaxDamage(newMaxDurability);
        damageable.setDamage(0);
        item.setItemMeta(meta);
    }

    public static Multimap<Attribute, AttributeModifier> removeYinwuModifiers(ItemMeta meta) {
        Multimap<Attribute, AttributeModifier> allModifiers = meta.getAttributeModifiers();
        if (allModifiers == null) {
            allModifiers = ArrayListMultimap.create();
        } else {
            allModifiers = ArrayListMultimap.create(allModifiers);
        }

        List<Map.Entry<Attribute, AttributeModifier>> toRemove = new ArrayList<>();
        for (Map.Entry<Attribute, AttributeModifier> entry : allModifiers.entries()) {
            if ("yinwu".equals(entry.getValue().getKey().getNamespace())) {
                toRemove.add(entry);
            }
        }
        for (Map.Entry<Attribute, AttributeModifier> entry : toRemove) {
            allModifiers.remove(entry.getKey(), entry.getValue());
        }
        return allModifiers;
    }
}
