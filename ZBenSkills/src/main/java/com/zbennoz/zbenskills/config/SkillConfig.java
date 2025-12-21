package com.zbennoz.zbenskills.config;

import com.zbennoz.zbenskills.ZBenSkillsPlugin;
import com.zbennoz.zbenskills.model.AchievementDefinition;
import com.zbennoz.zbenskills.model.ChallengeDefinition;
import com.zbennoz.zbenskills.model.SkillNode;
import com.zbennoz.zbenskills.model.SkillType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class SkillConfig {
    private final ZBenSkillsPlugin plugin;
    private final Map<SkillType, List<SkillNode>> skillNodes = new EnumMap<>(SkillType.class);
    private final List<AchievementDefinition> achievements = new ArrayList<>();
    private final List<ChallengeDefinition> challenges = new ArrayList<>();

    private final int maxLevel;
    private final double baseXp;
    private final double exponentialBase;
    private final double softCapStart;
    private final double softCapMultiplier;
    private final int prestigeTokenReward;
    private final int nodeCountPerSkill;

    private final long actionCooldownMs;
    private final int diminishWindowSeconds;
    private final int diminishThreshold;
    private final double diminishMultiplier;
    private final Set<String> disabledWorlds;
    private final boolean disableSpawnerMobs;

    public SkillConfig(ZBenSkillsPlugin plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        this.maxLevel = config.getInt("skills.max-level", 200);
        this.baseXp = config.getDouble("skills.base-xp", 125.0);
        this.exponentialBase = config.getDouble("skills.exponential-base", 1.18);
        this.softCapStart = config.getDouble("skills.softcap-start", 150);
        this.softCapMultiplier = config.getDouble("skills.softcap-multiplier", 1.32);
        this.prestigeTokenReward = config.getInt("skills.prestige.tokens", 1);
        this.nodeCountPerSkill = Math.max(25, config.getInt("skill-tree.default-node-count", 25));

        this.actionCooldownMs = config.getLong("anti-exploit.action-cooldown-ms", 1500L);
        this.diminishWindowSeconds = config.getInt("anti-exploit.diminish-window-seconds", 30);
        this.diminishThreshold = config.getInt("anti-exploit.diminish-threshold", 10);
        this.diminishMultiplier = config.getDouble("anti-exploit.diminish-multiplier", 0.6);
        this.disabledWorlds = new HashSet<>();
        for (String world : config.getStringList("anti-exploit.disable-worlds")) {
            this.disabledWorlds.add(world.toLowerCase());
        }
        this.disableSpawnerMobs = config.getBoolean("anti-exploit.disable-spawner-mobs", true);

        loadNodes(config);
        loadAchievements();
        loadChallenges();
    }

    private void loadNodes(FileConfiguration config) {
        ConfigurationSection nodeSection = config.getConfigurationSection("skill-tree.nodes");
        for (SkillType type : SkillType.values()) {
            List<SkillNode> nodes = new ArrayList<>();
            if (nodeSection != null) {
                ConfigurationSection skillSection = nodeSection.getConfigurationSection(type.name());
                if (skillSection != null) {
                    for (String id : skillSection.getKeys(false)) {
                        ConfigurationSection section = skillSection.getConfigurationSection(id);
                        if (section == null) continue;
                        String name = section.getString("name", id);
                        String desc = section.getString("description", "Node for " + type.getDisplayName());
                        int required = section.getInt("level", 1);
                        int cost = section.getInt("cost", 1);
                        List<String> prereq = section.getStringList("requires");
                        boolean prestigeLocked = section.getBoolean("prestige", false);
                        nodes.add(new SkillNode(type.name() + ":" + id, type, name, desc, required, cost, prereq, prestigeLocked));
                    }
                }
            }
            if (nodes.size() < nodeCountPerSkill) {
                int start = nodes.size() + 1;
                for (int i = start; i <= nodeCountPerSkill; i++) {
                    String id = type.name().toLowerCase() + "_tier_" + i;
                    nodes.add(new SkillNode(id, type, type.getDisplayName() + " Tier " + i,
                            "Scaling perk tier " + i, 1 + i * 2, 1 + (i / 2),
                            i == 1 ? Collections.emptyList() : Collections.singletonList(type.name().toLowerCase() + "_tier_" + (i - 1)),
                            i > nodeCountPerSkill - 5));
                }
            }
            nodes.sort(Comparator.comparingInt(SkillNode::getRequiredLevel));
            skillNodes.put(type, nodes);
        }
    }

    private void loadAchievements() {
        File file = new File(plugin.getDataFolder(), "achievements.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("achievements");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection node = section.getConfigurationSection(key);
                if (node == null) continue;
                SkillType skill = SkillType.valueOf(node.getString("skill", "MINING"));
                int goal = node.getInt("goal", 100);
                int points = node.getInt("skill-points", 2);
                String name = node.getString("name", key);
                String description = node.getString("description", "Milestone");
                achievements.add(new AchievementDefinition(key, name, description, skill, goal, points));
            }
        }
    }

    private void loadChallenges() {
        File file = new File(plugin.getDataFolder(), "achievements.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("challenges.daily");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection node = section.getConfigurationSection(key);
                if (node == null) continue;
                SkillType skill = SkillType.valueOf(node.getString("skill", "MINING"));
                int goal = node.getInt("goal", 50);
                int reward = node.getInt("reward", 1);
                challenges.add(new ChallengeDefinition("daily:" + key, ChallengeDefinition.ChallengeType.DAILY, skill, goal, reward));
            }
        }
        section = config.getConfigurationSection("challenges.weekly");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection node = section.getConfigurationSection(key);
                if (node == null) continue;
                SkillType skill = SkillType.valueOf(node.getString("skill", "MINING"));
                int goal = node.getInt("goal", 400);
                int reward = node.getInt("reward", 4);
                challenges.add(new ChallengeDefinition("weekly:" + key, ChallengeDefinition.ChallengeType.WEEKLY, skill, goal, reward));
            }
        }
    }

    public double xpRequiredForLevel(int level) {
        double soft = Math.max(1.0, level - softCapStart);
        double multiplier = soft > 0 ? Math.pow(softCapMultiplier, soft / 10.0) : 1.0;
        return baseXp * Math.pow(exponentialBase, level) * multiplier;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getPrestigeTokenReward() {
        return prestigeTokenReward;
    }

    public Map<SkillType, List<SkillNode>> getSkillNodes() {
        return skillNodes;
    }

    public List<AchievementDefinition> getAchievements() {
        return achievements;
    }

    public List<ChallengeDefinition> getChallenges() {
        return challenges;
    }

    public Collection<SkillType> getSkillTypes() {
        return Arrays.asList(SkillType.values());
    }

    public long getActionCooldownMs() {
        return actionCooldownMs;
    }

    public int getDiminishWindowSeconds() {
        return diminishWindowSeconds;
    }

    public int getDiminishThreshold() {
        return diminishThreshold;
    }

    public double getDiminishMultiplier() {
        return diminishMultiplier;
    }

    public Set<String> getDisabledWorlds() {
        return disabledWorlds;
    }

    public boolean isDisableSpawnerMobs() {
        return disableSpawnerMobs;
    }
}
