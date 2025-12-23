package com.zbennoz.zbenskills.service;

import com.zbennoz.zbenskills.ZBenSkillsPlugin;
import com.zbennoz.zbenskills.config.SkillConfig;
import com.zbennoz.zbenskills.data.PlayerProfile;
import com.zbennoz.zbenskills.model.AchievementDefinition;
import com.zbennoz.zbenskills.model.SkillNode;
import com.zbennoz.zbenskills.model.SkillType;
import com.zbennoz.zbenskills.storage.PlayerSkillRepository;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SkillService {
    private final PlayerSkillRepository repository;
    private final SkillConfig config;
    private final AntiExploitService antiExploitService;
    private final ChallengeService challengeService;
    private final MessageService messages;

    public SkillService(ZBenSkillsPlugin plugin, PlayerSkillRepository repository, SkillConfig config, AntiExploitService antiExploitService, ChallengeService challengeService, MessageService messages) {
        this.repository = repository;
        this.config = config;
        this.antiExploitService = antiExploitService;
        this.challengeService = challengeService;
        this.messages = messages;
    }

    public void addXp(Player player, SkillType skill, double amount, String actionKey) {
        if (config.getDisabledWorlds().contains(player.getWorld().getName().toLowerCase())) {
            return;
        }
        if (antiExploitService.isOnCooldown(player, actionKey)) {
            return;
        }
        double factor = antiExploitService.diminishingFactor(player, actionKey);
        double finalAmount = amount * factor;
        PlayerProfile profile = repository.getProfile(player.getUniqueId());
        double currentXp = profile.getXp().getOrDefault(skill, 0.0);
        int level = profile.getLevels().getOrDefault(skill, 1);
        int prestige = profile.getPrestige().getOrDefault(skill, 0);
        double bonus = config.getBenefitValue(skill, "xp-bonus", level, prestige);
        finalAmount *= 1 + bonus;
        if (level >= config.getMaxLevel()) {
            return;
        }
        currentXp += finalAmount;
        double needed = config.xpRequiredForLevel(level);
        while (currentXp >= needed && level < config.getMaxLevel()) {
            currentXp -= needed;
            level++;
            profile.getLevels().put(skill, level);
            profile.addSkillPoints(1);
            Bukkit.getPluginManager().callEvent(new com.zbennoz.zbenskills.api.SkillLevelUpEvent(player, skill, level));
            messages.send(player, "level-up", Map.of(
                    "skill", skill.getDisplayName(),
                    "level", String.valueOf(level)
            ));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            needed = config.xpRequiredForLevel(level);
        }
        profile.getXp().put(skill, currentXp);
        repository.saveProfileAsync(profile);
        processAchievements(player, profile, skill);
        challengeService.progress(player.getUniqueId(), "daily:" + skill.name().toLowerCase(), 1);
        challengeService.progress(player.getUniqueId(), "weekly:" + skill.name().toLowerCase(), 1);
    }

    private void processAchievements(Player player, PlayerProfile profile, SkillType skill) {
        List<AchievementDefinition> defs = config.getAchievements();
        for (AchievementDefinition def : defs) {
            if (def.getSkill() != skill) continue;
            if (profile.getAchievements().contains(def.getId())) continue;
            int level = profile.getLevels().getOrDefault(skill, 1);
            if (level >= def.getGoal()) {
                profile.getAchievements().add(def.getId());
                profile.addSkillPoints(def.getSkillPoints());
                repository.saveProfileAsync(profile);
                messages.send(player, "achievement", Map.of(
                        "name", def.getName(),
                        "points", String.valueOf(def.getSkillPoints())
                ));
            }
        }
    }

    public boolean unlockNode(Player player, SkillNode node) {
        PlayerProfile profile = repository.getProfile(player.getUniqueId());
        if (profile.getUnlockedNodes().contains(node.getId())) {
            return false;
        }
        int level = profile.getLevels().getOrDefault(node.getSkill(), 1);
        if (level < node.getRequiredLevel()) {
            messages.send(player, "node-level", Map.of("level", String.valueOf(node.getRequiredLevel())));
            return false;
        }
        for (String req : node.getPrerequisites()) {
            if (!profile.getUnlockedNodes().contains(req)) {
                messages.send(player, "node-requirement", Map.of("req", req));
                return false;
            }
        }
        if (!profile.spendSkillPoints(node.getCost())) {
            messages.send(player, "not-enough-points", Map.of());
            return false;
        }
        profile.getUnlockedNodes().add(node.getId());
        repository.saveProfileAsync(profile);
        Bukkit.getPluginManager().callEvent(new com.zbennoz.zbenskills.api.NodeUnlockEvent(player, node));
        messages.send(player, "node-unlocked", Map.of("name", node.getName()));
        return true;
    }

    public boolean prestige(Player player, SkillType skill) {
        PlayerProfile profile = repository.getProfile(player.getUniqueId());
        int level = profile.getLevels().getOrDefault(skill, 1);
        if (level < config.getMaxLevel()) {
            messages.send(player, "prestige-requirement", Map.of());
            return false;
        }
        profile.getLevels().put(skill, 1);
        profile.getXp().put(skill, 0.0);
        int prestige = profile.getPrestige().getOrDefault(skill, 0) + 1;
        profile.getPrestige().put(skill, prestige);
        profile.addSkillPoints(config.getPrestigeTokenReward());
        repository.saveProfileAsync(profile);
        Bukkit.getPluginManager().callEvent(new com.zbennoz.zbenskills.api.PrestigeEvent(player, skill, prestige));
        double prestigeBoost = (config.getPrestigeBenefitMultiplier() * 100);
        messages.send(player, "prestige-success", Map.of(
                "prestige", String.valueOf(prestige),
                "bonus", String.format(Locale.GERMAN, "%.1f", prestigeBoost)
        ));
        return true;
    }

    public int getLevel(UUID uuid, SkillType skill) {
        return repository.getProfile(uuid).getLevels().getOrDefault(skill, 1);
    }

    public int getPrestige(UUID uuid, SkillType skill) {
        return repository.getProfile(uuid).getPrestige().getOrDefault(skill, 0);
    }

    public boolean hasNode(UUID uuid, String nodeId) {
        return repository.getProfile(uuid).getUnlockedNodes().contains(nodeId);
    }

    public Set<String> getUnlocked(UUID uuid) {
        return repository.getProfile(uuid).getUnlockedNodes();
    }

    public PlayerProfile getProfile(UUID uuid) {
        return repository.getProfile(uuid);
    }
}
