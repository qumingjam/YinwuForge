package com.yinwu.api;

import com.yinwu.YinwuForgePlugin;
import com.yinwu.manager.ForgeManager;
import com.yinwu.model.EquipmentAttributes;
import com.yinwu.model.EquipmentData;
import net.yinwu.lib.api.ForgeAPI;
import org.bukkit.inventory.ItemStack;

public class ForgeAPIImpl implements ForgeAPI {

    private final YinwuForgePlugin plugin;
    private final ForgeManager forgeManager;

    public ForgeAPIImpl(YinwuForgePlugin plugin, ForgeManager forgeManager) {
        this.plugin = plugin;
        this.forgeManager = forgeManager;
    }

    @Override
    public String apiVersion() {
        return "1.0.0";
    }

    @Override
    public boolean isForgeItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        EquipmentData data = forgeManager.getEquipmentData(item);
        return data != null && data.getForgeCount() > 0;
    }

    @Override
    public int getForgeLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        EquipmentData data = forgeManager.getEquipmentData(item);
        return data != null ? data.getForgeCount() : 0;
    }

    @Override
    public int getBaseDamageBonus(ItemStack item) {
        EquipmentData data = forgeManager.getEquipmentData(item);
        if (data == null) return 0;
        EquipmentAttributes attrs = data.getAttributes();
        return attrs != null && attrs.getBaseDamage() != null ? attrs.getBaseDamage() : 0;
    }

    @Override
    public int getArmorValueBonus(ItemStack item) {
        EquipmentData data = forgeManager.getEquipmentData(item);
        if (data == null) return 0;
        EquipmentAttributes attrs = data.getAttributes();
        return attrs != null && attrs.getArmorValue() != null ? attrs.getArmorValue() : 0;
    }

    @Override
    public int getMaxDurabilityBonus(ItemStack item) {
        EquipmentData data = forgeManager.getEquipmentData(item);
        if (data == null) return 0;
        EquipmentAttributes attrs = data.getAttributes();
        return attrs != null && attrs.getMaxDurability() != null ? attrs.getMaxDurability() : 0;
    }

    @Override
    public ForgeSimulation simulate(ItemStack item) {
        EquipmentData data = forgeManager.getEquipmentData(item);
        if (data == null) return new ForgeSimulation("fail_no_penalty", 0, null, 0);
        return new ForgeSimulation("success", data.getForgeCount() + 1, null, 0);
    }
}
