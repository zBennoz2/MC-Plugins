package com.zbennoz.zbenskills.service;

import com.zbennoz.zbenskills.ZBenSkillsPlugin;
import com.zbennoz.zbenskills.data.PlayerProfile;
import com.zbennoz.zbenskills.model.ChallengeDefinition;
import com.zbennoz.zbenskills.storage.PlayerSkillRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ChallengeService {
    private final PlayerSkillRepository repository;
    private final List<ChallengeDefinition> definitions;

    public ChallengeService(ZBenSkillsPlugin plugin, PlayerSkillRepository repository, com.zbennoz.zbenskills.config.SkillConfig config) {
        this.repository = repository;
        this.definitions = config.getChallenges();
    }

    public List<ChallengeDefinition> getDailyChallenges() {
        return definitions.stream().filter(d -> d.getType() == ChallengeDefinition.ChallengeType.DAILY).collect(Collectors.toList());
    }

    public List<ChallengeDefinition> getWeeklyChallenges() {
        return definitions.stream().filter(d -> d.getType() == ChallengeDefinition.ChallengeType.WEEKLY).collect(Collectors.toList());
    }

    public void progress(UUID uuid, String challengeId, int amount) {
        PlayerProfile profile = repository.getProfile(uuid);
        int progress = profile.getChallengeProgress().getOrDefault(challengeId, 0) + amount;
        profile.getChallengeProgress().put(challengeId, progress);
        repository.saveProfileAsync(profile);
    }

    public boolean tryComplete(UUID uuid, ChallengeDefinition def) {
        PlayerProfile profile = repository.getProfile(uuid);
        Integer progress = profile.getChallengeProgress().getOrDefault(def.getId(), 0);
        if (progress < def.getGoal()) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long last = profile.getLastChallengeClaim().get(def.getId());
        if (last != null && now - last < 3600_000L) {
            return false;
        }
        profile.getLastChallengeClaim().put(def.getId(), now);
        profile.addSkillPoints(def.getRewardPoints());
        repository.saveProfileAsync(profile);
        return true;
    }

    public void rotateIfNeeded(UUID uuid) {
        PlayerProfile profile = repository.getProfile(uuid);
        profile.getChallengeProgress().keySet().removeIf(this::isExpired);
        repository.saveProfileAsync(profile);
    }

    private boolean isExpired(String id) {
        return id.startsWith("daily:") || id.startsWith("weekly:");
    }
}
