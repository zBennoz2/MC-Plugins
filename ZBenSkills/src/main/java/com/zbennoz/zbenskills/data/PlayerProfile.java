package com.zbennoz.zbenskills.data;

import com.zbennoz.zbenskills.model.SkillType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerProfile {
    private final UUID uuid;
    private final Map<SkillType, Integer> levels = new HashMap<>();
    private final Map<SkillType, Double> xp = new HashMap<>();
    private final Map<SkillType, Integer> prestige = new HashMap<>();
    private final Set<String> unlockedNodes = new HashSet<>();
    private final Set<String> achievements = new HashSet<>();
    private final Map<String, Integer> challengeProgress = new HashMap<>();
    private final Map<String, Long> lastChallengeClaim = new HashMap<>();
    private int skillPoints; // global pool used by trees and achievements

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Map<SkillType, Integer> getLevels() {
        return levels;
    }

    public Map<SkillType, Double> getXp() {
        return xp;
    }

    public Map<SkillType, Integer> getPrestige() {
        return prestige;
    }

    public Set<String> getUnlockedNodes() {
        return unlockedNodes;
    }

    public Set<String> getAchievements() {
        return achievements;
    }

    public Map<String, Integer> getChallengeProgress() {
        return challengeProgress;
    }

    public Map<String, Long> getLastChallengeClaim() {
        return lastChallengeClaim;
    }

    public int getSkillPoints() {
        return skillPoints;
    }

    public void addSkillPoints(int amount) {
        this.skillPoints += amount;
    }

    public boolean spendSkillPoints(int amount) {
        if (skillPoints < amount) {
            return false;
        }
        skillPoints -= amount;
        return true;
    }
}
