package com.zbennoz.zbenskills.service;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

public class MessageService {
    private final FileConfiguration config;
    private final String prefix;

    public MessageService(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        this.prefix = ChatColor.translateAlternateColorCodes('&', config.getString("prefix", ""));
    }

    public String format(String key, Map<String, String> placeholders) {
        String raw = config.getString(key, key);
        if (raw == null) {
            raw = key;
        }
        String colored = ChatColor.translateAlternateColorCodes('&', raw);
        if (!prefix.isEmpty()) {
            colored = prefix + colored;
        }
        if (placeholders != null) {
            for (var entry : placeholders.entrySet()) {
                colored = colored.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        return colored;
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(format(key, placeholders));
    }

    public void send(Player player, String key, Map<String, String> placeholders) {
        player.sendMessage(format(key, placeholders));
    }
}
