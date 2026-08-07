package com.yinwu.api;

import net.yinwu.lib.api.YinwuServiceBridge;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.List;

/**
 * YinwuEnchant 联动 facade。
 *
 * 各插件各自 shade YinwuPluginLib，直接 ServicesManager.load 会因 Class 不同而失效，
 * 故通过反射桥取 provider 并缓存方法引用。线程安全：方法引用只读，provider 由 Enchant 注册。
 */
public final class EnchantLink {

    private final Object provider;
    private final Method mIds;
    private final Method mMaxLevel;
    private final Method mApply;
    private final Method mGetLevel;

    private EnchantLink(Object provider) throws Exception {
        this.provider = provider;
        Class<?> api = provider.getClass();
        mIds = api.getMethod("getEnchantmentIds");
        mMaxLevel = api.getMethod("getMaxLevel", String.class);
        mApply = api.getMethod("applyEnchantment", ItemStack.class, String.class, int.class);
        mGetLevel = api.getMethod("getEnchantmentLevel", ItemStack.class, String.class);
    }

    /** 从 YinwuEnchant 插件创建链接；插件未加载/未注册时返回 null */
    public static EnchantLink create() {
        try {
            Object provider = YinwuServiceBridge.getProvider("YinwuEnchant", "net.yinwu.lib.api.EnchantAPI");
            if (provider == null) return null;
            return new EnchantLink(provider);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getEnchantmentIds() {
        try {
            return (List<String>) mIds.invoke(provider);
        } catch (Exception e) {
            return List.of();
        }
    }

    public int getMaxLevel(String id) {
        try {
            return (Integer) mMaxLevel.invoke(provider, id);
        } catch (Exception e) {
            return 1;
        }
    }

    public void applyEnchantment(ItemStack item, String id, int level) {
        try {
            mApply.invoke(provider, item, id, level);
        } catch (Exception e) {
            // 联动失败不中断锻造
        }
    }

    public int getEnchantmentLevel(ItemStack item, String id) {
        try {
            return (Integer) mGetLevel.invoke(provider, item, id);
        } catch (Exception e) {
            return 0;
        }
    }
}
