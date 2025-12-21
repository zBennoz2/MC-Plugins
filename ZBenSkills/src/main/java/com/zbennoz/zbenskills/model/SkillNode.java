package com.zbennoz.zbenskills.model;

import java.util.List;

public class SkillNode {
    private final String id;
    private final SkillType skill;
    private final String name;
    private final String description;
    private final int requiredLevel;
    private final int cost;
    private final List<String> prerequisites;
    private final boolean prestigeLocked;

    public SkillNode(String id, SkillType skill, String name, String description, int requiredLevel, int cost, List<String> prerequisites, boolean prestigeLocked) {
        this.id = id;
        this.skill = skill;
        this.name = name;
        this.description = description;
        this.requiredLevel = requiredLevel;
        this.cost = cost;
        this.prerequisites = prerequisites;
        this.prestigeLocked = prestigeLocked;
    }

    public String getId() {
        return id;
    }

    public SkillType getSkill() {
        return skill;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public int getCost() {
        return cost;
    }

    public List<String> getPrerequisites() {
        return prerequisites;
    }

    public boolean isPrestigeLocked() {
        return prestigeLocked;
    }
}
