package com.zbennoz.zbencityjobs.util;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

public class MessageService {
    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration config;
    private String prefix;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
    }

    public void load() {
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        this.prefix = ChatColor.translateAlternateColorCodes('&', config.getString("prefix", ""));
    }

    public String get(String path) {
        return format(config.getString(path, path));
    }

    public String get(String path, Map<String, String> placeholders) {
        String raw = config.getString(path, path);
        if (raw == null) {
            raw = path;
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return format(raw);
    }

    private String format(String input) {
        return prefix + ChatColor.translateAlternateColorCodes('&', input);
    }
}
