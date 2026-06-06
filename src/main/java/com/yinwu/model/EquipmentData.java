package com.yinwu.model;

import java.util.ArrayList;
import java.util.List;

public class EquipmentData {
    private int forgeLevel;  // 锻造等级
    private int forgeCount;  // 锻造次数
    private String additionalEffect;  // 附加效果
    private List<PotionEffectData> potionEffects;  // 药水效果列表
    private boolean hasSpecialEffect;  // 是否有特殊效果（特殊效果后无法锻造）
    private EquipmentAttributes attributes;  // 装备属性修改
    
    public EquipmentData() {
        this.forgeLevel = 0;
        this.forgeCount = 0;
        this.additionalEffect = null;
        this.potionEffects = new ArrayList<>();
        this.hasSpecialEffect = false;
        this.attributes = new EquipmentAttributes();
    }
    
    public EquipmentData(int forgeLevel, int forgeCount, String additionalEffect) {
        this.forgeLevel = forgeLevel;
        this.forgeCount = forgeCount;
        this.additionalEffect = additionalEffect;
        this.potionEffects = new ArrayList<>();
        this.hasSpecialEffect = false;
        this.attributes = new EquipmentAttributes();
    }
    
    public int getForgeLevel() {
        return forgeLevel;
    }
    
    public void setForgeLevel(int forgeLevel) {
        this.forgeLevel = forgeLevel;
    }
    
    public int getForgeCount() {
        return forgeCount;
    }
    
    public void setForgeCount(int forgeCount) {
        this.forgeCount = forgeCount;
    }
    
    public String getAdditionalEffect() {
        return additionalEffect;
    }
    
    public void setAdditionalEffect(String additionalEffect) {
        this.additionalEffect = additionalEffect;
    }
    
    public List<PotionEffectData> getPotionEffects() {
        return potionEffects;
    }
    
    public void setPotionEffects(List<PotionEffectData> potionEffects) {
        this.potionEffects = potionEffects;
    }
    
    public boolean hasSpecialEffect() {
        return hasSpecialEffect;
    }
    
    public void setHasSpecialEffect(boolean hasSpecialEffect) {
        this.hasSpecialEffect = hasSpecialEffect;
    }
    
    /**
     * 添加药水效果
     */
    public void addPotionEffect(PotionEffectData effect) {
        // 检查是否已存在相同效果
        boolean exists = potionEffects.stream()
            .anyMatch(e -> e.getEffectName().equals(effect.getEffectName()) && e.isSpecial() == effect.isSpecial());
        if (!exists) {
            potionEffects.add(effect);
            if (effect.isSpecial()) {
                this.hasSpecialEffect = true;
            }
        }
    }
    
    /**
     * 移除药水效果
     */
    public void removePotionEffect(PotionEffectData effect) {
        potionEffects.removeIf(e -> e.getEffectName().equals(effect.getEffectName()) && e.isSpecial() == effect.isSpecial());
    }
    
    /**
     * 检查是否有特殊效果
     */
    public boolean containsSpecialEffect() {
        return potionEffects.stream().anyMatch(PotionEffectData::isSpecial);
    }
    
    public void incrementForgeCount() {
        this.forgeCount++;  // 增加锻造次数
    }
    
    public void incrementForgeLevel() {
        this.forgeLevel++;  // 增加锻造等级
    }
    
    public void decrementForgeLevel() {
        if (this.forgeLevel > 0) {
            this.forgeLevel--;  // 降低锻造等级
        }
    }
    
    public EquipmentAttributes getAttributes() {
        return attributes;
    }
    
    public void setAttributes(EquipmentAttributes attributes) {
        this.attributes = attributes;
    }
}
