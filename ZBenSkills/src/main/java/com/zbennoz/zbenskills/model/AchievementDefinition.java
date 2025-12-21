package com.zbennoz.zbenskills.model;

public class AchievementDefinition {
    private final String id;
    private final String name;
    private final String description;
    private final SkillType skill;
    private final int goal;
    private final int skillPoints;

    public AchievementDefinition(String id, String name, String description, SkillType skill, int goal, int skillPoints) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.skill = skill;
        this.goal = goal;
        this.skillPoints = skillPoints;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public SkillType getSkill() {
        return skill;
    }

    public int getGoal() {
        return goal;
    }

    public int getSkillPoints() {
        return skillPoints;
    }
}
