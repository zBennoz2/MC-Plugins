package com.zbennoz.zbenskills.model;

public class ChallengeDefinition {
    public enum ChallengeType { DAILY, WEEKLY }

    private final String id;
    private final ChallengeType type;
    private final SkillType skill;
    private final int goal;
    private final int rewardPoints;

    public ChallengeDefinition(String id, ChallengeType type, SkillType skill, int goal, int rewardPoints) {
        this.id = id;
        this.type = type;
        this.skill = skill;
        this.goal = goal;
        this.rewardPoints = rewardPoints;
    }

    public String getId() {
        return id;
    }

    public ChallengeType getType() {
        return type;
    }

    public SkillType getSkill() {
        return skill;
    }

    public int getGoal() {
        return goal;
    }

    public int getRewardPoints() {
        return rewardPoints;
    }
}
