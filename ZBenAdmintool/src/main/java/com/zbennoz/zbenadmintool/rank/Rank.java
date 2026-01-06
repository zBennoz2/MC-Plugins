package com.zbennoz.zbenadmintool.rank;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class Rank {
    private final String name;
    private final String colorText;
    private final String legacyColor;
    private final int priority;
    private final String prefix;
    private final String suffix;
    private final Set<String> bukkitPermissions = new HashSet<>();
    private final Set<RankPermission> rolePermissions = EnumSet.noneOf(RankPermission.class);
    private final int backpackSlots;
    private final int maxClaimChunks;

    public Rank(String name, String colorText, String legacyColor, int priority, String prefix, String suffix, int backpackSlots,
                int maxClaimChunks) {
        this.name = name;
        this.colorText = colorText;
        this.legacyColor = legacyColor;
        this.priority = priority;
        this.prefix = prefix;
        this.suffix = suffix;
        this.backpackSlots = backpackSlots;
        this.maxClaimChunks = maxClaimChunks;
    }

    public String getName() {
        return name;
    }

    public String getColorText() {
        return colorText;
    }

    public String getLegacyColor() {
        return legacyColor;
    }

    public int getPriority() {
        return priority;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public Set<String> getBukkitPermissions() {
        return bukkitPermissions;
    }

    public Set<RankPermission> getRolePermissions() {
        return rolePermissions;
    }

    public int getBackpackSlots() {
        return backpackSlots;
    }

    public int getMaxClaimChunks() {
        return maxClaimChunks;
    }
}
