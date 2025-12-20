package com.zbennoz.zbencore.ranks;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public final class RankService {

    private final JavaPlugin plugin;
    private final Map<String, Rank> ranks = new HashMap<>();
    private File ranksFile;
    private FileConfiguration ranksConfig;

    public RankService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        ensureFile();
        this.ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);
        this.ranks.clear();

        var section = ranksConfig.getConfigurationSection("ranks");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String display = section.getString(key + ".displayName", key);
                String prefix = section.getString(key + ".prefix", "");
                String color = section.getString(key + ".color", "WHITE");
                int weight = section.getInt(key + ".weight", 0);
                ranks.put(key.toLowerCase(Locale.ROOT), new Rank(key, display, prefix, color, weight));
            }
        }
    }

    public void reload() {
        load();
    }

    public void saveRanks() {
        ranksConfig.set("ranks", null);
        for (Rank rank : ranks.values()) {
            String base = "ranks." + rank.getKey();
            ranksConfig.set(base + ".displayName", rank.getDisplayName());
            ranksConfig.set(base + ".prefix", rank.getPrefix());
            ranksConfig.set(base + ".color", rank.getColor());
            ranksConfig.set(base + ".weight", rank.getWeight());
        }
        try {
            ranksConfig.save(ranksFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Konnte ranks.yml nicht speichern: " + e.getMessage());
        }
    }

    public boolean exists(String key) {
        return ranks.containsKey(key.toLowerCase(Locale.ROOT));
    }

    public Rank get(String key) {
        return ranks.get(key.toLowerCase(Locale.ROOT));
    }

    public void add(Rank rank) {
        ranks.put(rank.getKey().toLowerCase(Locale.ROOT), rank);
        saveRanks();
    }

    public void update(String key, Rank updated) {
        ranks.remove(key.toLowerCase(Locale.ROOT));
        ranks.put(updated.getKey().toLowerCase(Locale.ROOT), updated);
        saveRanks();
    }

    public List<Rank> listSorted() {
        return ranks.values().stream()
                .sorted(Comparator.comparingInt(Rank::getWeight).reversed()
                        .thenComparing(r -> r.getKey().toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
    }

    public ChatColor parseColor(String input) {
        if (input == null || input.isBlank()) return ChatColor.WHITE;
        String trimmed = input.trim();
        try {
            return ChatColor.valueOf(trimmed.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            try {
                return ChatColor.of(trimmed);
            } catch (IllegalArgumentException ignored) {
                return ChatColor.WHITE;
            }
        }
    }

    public String normalizeColor(String input) {
        if (input == null || input.isBlank()) return "WHITE";
        String trimmed = input.trim();
        try {
            return ChatColor.valueOf(trimmed.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            if (trimmed.startsWith("#") && trimmed.length() == 7) {
                return trimmed.toUpperCase(Locale.ROOT);
            }
            return "WHITE";
        }
    }

    private void ensureFile() {
        if (ranksFile == null) {
            ranksFile = new File(plugin.getDataFolder(), "ranks.yml");
        }
        if (!ranksFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                plugin.saveResource("ranks.yml", false);
            } catch (IllegalArgumentException ignored) {
                createDefaultFile();
            }
        }
    }

    private void createDefaultFile() {
        ranksConfig = new YamlConfiguration();
        ranksConfig.set("ranks.admin.displayName", "Admin");
        ranksConfig.set("ranks.admin.prefix", "&c[Admin]&r ");
        ranksConfig.set("ranks.admin.color", "RED");
        ranksConfig.set("ranks.admin.weight", 100);
        try {
            ranksConfig.save(ranksFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Konnte ranks.yml nicht erstellen: " + e.getMessage());
        }
    }
}
