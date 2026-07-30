package com.yinwu.model;

/**
 * 装备属性数据模型
 */
public class EquipmentAttributes {
    private Integer maxDurability;      // 最大耐久值
    private Integer miningSpeed;        // 挖掘速度
    private Integer armorToughness;     // 护甲韧性
    private Integer armorValue;         // 盔甲值
    private Integer attackSpeed;        // 攻击速度
    private Integer baseDamage;         // 基础伤害

    public EquipmentAttributes() {
    }

    public Integer getMaxDurability() {
        return maxDurability;
    }

    public int getMaxDurabilityOrDefault() {
        return maxDurability != null ? maxDurability : 0;
    }

    public void setMaxDurability(Integer maxDurability) {
        this.maxDurability = maxDurability;
    }

    public Integer getMiningSpeed() {
        return miningSpeed;
    }

    public int getMiningSpeedOrDefault() {
        return miningSpeed != null ? miningSpeed : 0;
    }

    public void setMiningSpeed(Integer miningSpeed) {
        this.miningSpeed = miningSpeed;
    }

    public Integer getArmorToughness() {
        return armorToughness;
    }

    public int getArmorToughnessOrDefault() {
        return armorToughness != null ? armorToughness : 0;
    }

    public void setArmorToughness(Integer armorToughness) {
        this.armorToughness = armorToughness;
    }

    public Integer getArmorValue() {
        return armorValue;
    }

    public int getArmorValueOrDefault() {
        return armorValue != null ? armorValue : 0;
    }

    public void setArmorValue(Integer armorValue) {
        this.armorValue = armorValue;
    }

    public Integer getAttackSpeed() {
        return attackSpeed;
    }

    public int getAttackSpeedOrDefault() {
        return attackSpeed != null ? attackSpeed : 0;
    }

    public void setAttackSpeed(Integer attackSpeed) {
        this.attackSpeed = attackSpeed;
    }

    public Integer getBaseDamage() {
        return baseDamage;
    }

    public int getBaseDamageOrDefault() {
        return baseDamage != null ? baseDamage : 0;
    }

    public void setBaseDamage(Integer baseDamage) {
        this.baseDamage = baseDamage;
    }

    /**
     * 检查是否有属性修改
     */
    public boolean hasAnyAttribute() {
        return maxDurability != null || miningSpeed != null ||
               armorToughness != null || armorValue != null ||
               attackSpeed != null || baseDamage != null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (maxDurability != null) sb.append("maxDurability:").append(maxDurability).append(";");
        if (miningSpeed != null) sb.append("miningSpeed:").append(miningSpeed).append(";");
        if (armorToughness != null) sb.append("armorToughness:").append(armorToughness).append(";");
        if (armorValue != null) sb.append("armorValue:").append(armorValue).append(";");
        if (attackSpeed != null) sb.append("attackSpeed:").append(attackSpeed).append(";");
        if (baseDamage != null) sb.append("baseDamage:").append(baseDamage).append(";");
        return sb.toString();
    }
}
