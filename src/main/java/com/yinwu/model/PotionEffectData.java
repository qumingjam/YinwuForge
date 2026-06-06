package com.yinwu.model;

import org.bukkit.potion.PotionEffectType;

import java.util.Objects;

/**
 * 药水效果数据模型
 */
public class PotionEffectData {
    private String effectName;  // 药水效果名称（如 SPEED, STRENGTH）
    private int level;          // 效果等级（1, 2, 3...）
    private boolean isSpecial;  // 是否为特殊效果
    private boolean isNegative; // 是否为负面效果
    
    public PotionEffectData() {
    }
    
    public PotionEffectData(String effectName, int level, boolean isSpecial) {
        this.effectName = effectName;
        this.level = level;
        this.isSpecial = isSpecial;
    }
    
    public PotionEffectData(String effectName, int level, boolean isSpecial, boolean isNegative) {
        this.effectName = effectName;
        this.level = level;
        this.isSpecial = isSpecial;
        this.isNegative = isNegative;
    }
    
    public String getEffectName() {
        return effectName;
    }
    
    public void setEffectName(String effectName) {
        this.effectName = effectName;
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    
    public boolean isSpecial() {
        return isSpecial;
    }
    
    public void setSpecial(boolean special) {
        isSpecial = special;
    }
    
    public boolean isNegative() {
        return isNegative;
    }
    
    public void setNegative(boolean negative) {
        isNegative = negative;
    }
    
    /**
     * 获取药水效果类型
     */
    public PotionEffectType getEffectType() {
        return PotionEffectType.getByName(effectName);
    }
    
    /**
     * 增加等级
     */
    public void incrementLevel() {
        this.level++;
    }
    
    /**
     * 降低等级
     */
    public void decrementLevel() {
        if (this.level > 1) {
            this.level--;
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PotionEffectData that = (PotionEffectData) o;
        return Objects.equals(effectName, that.effectName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(effectName);
    }
    
    @Override
    public String toString() {
        return effectName + ":" + level + ":" + isSpecial + ":" + isNegative;
    }
}
