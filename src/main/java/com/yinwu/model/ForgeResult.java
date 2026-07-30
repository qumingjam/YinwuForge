package com.yinwu.model;

import org.bukkit.ChatColor;

public enum ForgeResult {
    FAIL_NO_PENALTY(ChatColor.GRAY, "锻造失败", "装备未受损"),
    EQUIPMENT_DESTROYED(ChatColor.RED, "锻造失败", "装备已损毁！"),
    DOWNGRADE(ChatColor.YELLOW, "锻造失败", "装备等级降低！"),
    SUCCESS(ChatColor.GREEN, "锻造成功", "装备等级提升！"),
    PERFECT(ChatColor.AQUA, "完美锻造", "获得极品属性！");

    private final ChatColor color;  // 颜色
    private final String title;  // 标题
    private final String description;  // 描述

    ForgeResult(ChatColor color, String title, String description) {
        this.color = color;
        this.title = title;
        this.description = description;
    }

    public ChatColor getColor() {
        return color;
    }

    public String getTitle() {
        return color + title;
    }

    public String getDescription() {
        return ChatColor.WHITE + description;
    }

    public String getFullMessage() {
        return getTitle() + " - " + getDescription();
    }
}
