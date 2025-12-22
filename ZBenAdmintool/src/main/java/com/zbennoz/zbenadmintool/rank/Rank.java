package com.zbennoz.zbenadmintool.rank;

import org.bukkit.ChatColor;

import java.util.HashSet;
import java.util.Set;

public class Rank {
    private final String name;
    private final ChatColor color;
    private final int priority;
    private final String prefix;
    private final String suffix;
    private final Set<String> permissions = new HashSet<>();

    public Rank(String name, ChatColor color, int priority, String prefix, String suffix) {
        this.name = name;
        this.color = color;
        this.priority = priority;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public String getName() {
        return name;
    }

    public ChatColor getColor() {
        return color;
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
