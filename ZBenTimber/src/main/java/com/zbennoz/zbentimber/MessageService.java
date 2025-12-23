package com.zbennoz.zbentimber;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;

public class MessageService {
    private final FileConfiguration config;
    private final String prefix;

    public MessageService(ZBenTimberPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        this.prefix = ChatColor.translateAlternateColorCodes('&', config.getString("prefix", ""));
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        String raw = config.getString(key, key);
        if (raw == null) raw = key;
        String msg = ChatColor.translateAlternateColorCodes('&', raw);
        if (placeholders != null) {
            for (var entry : placeholders.entrySet()) {
                msg = msg.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        if (!prefix.isEmpty()) {
            msg = prefix + msg;
        }
        sender.sendMessage(msg);
    }
}
