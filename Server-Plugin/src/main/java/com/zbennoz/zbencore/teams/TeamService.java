package com.zbennoz.zbencore.teams;

import com.zbennoz.zbencore.util.Msg;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public final class TeamService {

    private final JavaPlugin plugin;
    private final Map<String, Team> teams = new HashMap<>();
    private final Map<UUID, String> playerTeams = new HashMap<>();
    private File teamsFile;
    private FileConfiguration teamsConfig;

    public TeamService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        ensureFile();
        this.teamsConfig = YamlConfiguration.loadConfiguration(teamsFile);
        this.teams.clear();
        this.playerTeams.clear();

        var section = teamsConfig.getConfigurationSection("teams");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String display = section.getString(key + ".displayName", key);
                String prefix = section.getString(key + ".prefix", "");
                String color = section.getString(key + ".color", "WHITE");
                int weight = section.getInt(key + ".weight", 0);
                teams.put(key.toLowerCase(Locale.ROOT), new Team(key, display, prefix, color, weight));
            }
        }

        ConfigurationSection playersSection = teamsConfig.getConfigurationSection("players");
        if (playersSection != null) {
            for (String rawId : playersSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(rawId);
                    String teamKey = playersSection.getString(rawId);
                    if (teamKey != null && exists(teamKey)) {
                        playerTeams.put(uuid, teamKey.toLowerCase(Locale.ROOT));
                    }
                } catch (IllegalArgumentException ignored) {
                    // Invalid UUID entry, ignore
                }
            }
        }
    }

    public void reload() {
        load();
    }

    public void saveTeams() {
        teamsConfig.set("teams", null);
        teamsConfig.set("players", null);
        for (Team team : teams.values()) {
            String base = "teams." + team.getKey();
            teamsConfig.set(base + ".displayName", team.getDisplayName());
            teamsConfig.set(base + ".prefix", team.getPrefix());
            teamsConfig.set(base + ".color", team.getColor());
            teamsConfig.set(base + ".weight", team.getWeight());
        }
        for (Map.Entry<UUID, String> entry : playerTeams.entrySet()) {
            teamsConfig.set("players." + entry.getKey(), entry.getValue());
        }
        try {
            teamsConfig.save(teamsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Konnte teams.yml nicht speichern: " + e.getMessage());
        }
    }

    public boolean exists(String key) {
        return teams.containsKey(key.toLowerCase(Locale.ROOT));
    }

    public Team get(String key) {
        return teams.get(key.toLowerCase(Locale.ROOT));
    }

    public void add(Team team) {
        teams.put(team.getKey().toLowerCase(Locale.ROOT), team);
        saveTeams();
    }

    public void update(String key, Team updated) {
        teams.remove(key.toLowerCase(Locale.ROOT));
        teams.put(updated.getKey().toLowerCase(Locale.ROOT), updated);
        saveTeams();
    }

    public List<Team> listSorted() {
        return teams.values().stream()
                .sorted(Comparator.comparingInt(Team::getWeight).reversed()
                        .thenComparing(r -> r.getKey().toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
    }

    public Team getTeamFor(UUID playerId) {
        String key = playerTeams.get(playerId);
        if (key == null) return null;
        return get(key);
    }

    public void setTeam(UUID playerId, String key) {
        if (!exists(key)) {
            throw new IllegalArgumentException("Team existiert nicht: " + key);
        }
        playerTeams.put(playerId, key.toLowerCase(Locale.ROOT));
        saveTeams();
    }

    public void clearTeam(UUID playerId) {
        playerTeams.remove(playerId);
        saveTeams();
    }

    public ChatColor parseColor(String input) {
        if (input == null || input.isBlank()) return ChatColor.WHITE;
        String trimmed = input.trim();
        try {
            return ChatColor.valueOf(trimmed.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ChatColor.WHITE;
        }
    }

    public String normalizeColor(String input) {
        if (input == null || input.isBlank()) return "WHITE";
        String trimmed = input.trim();
        try {
            return ChatColor.valueOf(trimmed.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            return "WHITE";
        }
    }

    public void applyTeamDecorations(Player player) {
        Team team = getTeamFor(player.getUniqueId());
        if (team == null) {
            player.setDisplayName(player.getName());
            player.setPlayerListName(player.getName());
            return;
        }
        String prefix = Msg.color(team.getPrefix());
        ChatColor color = parseColor(team.getColor());
        String existingDisplay = player.getDisplayName();
        if (existingDisplay == null || existingDisplay.isBlank()) {
            existingDisplay = player.getName();
        }
        if (!existingDisplay.startsWith(prefix)) {
            existingDisplay = prefix + color + existingDisplay + ChatColor.RESET;
        }
        player.setDisplayName(existingDisplay);

        String existingList = player.getPlayerListName();
        if (existingList == null || existingList.isBlank()) {
            existingList = player.getName();
        }
        if (!existingList.startsWith(prefix)) {
            existingList = prefix + color + existingList + ChatColor.RESET;
        }
        player.setPlayerListName(existingList);
    }

    private void ensureFile() {
        if (teamsFile == null) {
            teamsFile = new File(plugin.getDataFolder(), "teams.yml");
        }
        if (!teamsFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                plugin.saveResource("teams.yml", false);
            } catch (IllegalArgumentException ignored) {
                createDefaultFile();
            }
        }
    }

    private void createDefaultFile() {
        teamsConfig = new YamlConfiguration();
        teamsConfig.set("teams.admin.displayName", "Admin");
        teamsConfig.set("teams.admin.prefix", "&c[Team Admin]&r ");
        teamsConfig.set("teams.admin.color", "RED");
        teamsConfig.set("teams.admin.weight", 100);
        try {
            teamsConfig.save(teamsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Konnte teams.yml nicht erstellen: " + e.getMessage());
        }
    }
}
