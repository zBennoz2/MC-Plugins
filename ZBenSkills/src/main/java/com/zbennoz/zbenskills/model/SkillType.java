package com.zbennoz.zbenskills.model;

public enum SkillType {
    MINING,
    WOODCUTTING,
    FARMING,
    FISHING,
    COMBAT,
    ARCHERY,
    ALCHEMY,
    ENCHANTING,
    BUILDING,
    TRADING,
    EXPLORATION,
    CRAFTING;

    public String getDisplayName() {
        String lower = name().toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
