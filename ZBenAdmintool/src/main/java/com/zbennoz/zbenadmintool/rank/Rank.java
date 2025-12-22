package com.zbennoz.zbenadmintool.rank;

import java.util.HashSet;
import java.util.Set;

public class Rank {
    private final String name;
    private final String colorText;
    private final String legacyColor;
    private final int priority;
    private final String prefix;
    private final String suffix;
    private final Set<String> permissions = new HashSet<>();

    public Rank(String name, String colorText, String legacyColor, int priority, String prefix, String suffix) {
        this.name = name;
        this.colorText = colorText;
        this.legacyColor = legacyColor;
        this.priority = priority;
        this.prefix = prefix;
        this.suffix = suffix;
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

    public Set<String> getPermissions() {
        return permissions;
    }
}
