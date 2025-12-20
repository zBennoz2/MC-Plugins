package com.zbennoz.zbencore.teams;

public final class Team {
    private final String key;
    private final String displayName;
    private final String prefix;
    private final String color;
    private final int weight;

    public Team(String key, String displayName, String prefix, String color, int weight) {
        this.key = key;
        this.displayName = displayName;
        this.prefix = prefix;
        this.color = color;
        this.weight = weight;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getColor() {
        return color;
    }

    public int getWeight() {
        return weight;
    }
}
