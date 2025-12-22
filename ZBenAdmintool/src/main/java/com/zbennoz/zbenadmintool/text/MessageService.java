package com.zbennoz.zbenadmintool.text;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Map;

public class MessageService {

    private final Plugin plugin;
    private FileConfiguration config;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public MessageService(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public String raw(String key) {
        return config.getString(key, key);
    }

    public void send(Player player, String key) {
        player.sendMessage(LegacyComponentSerializer.legacySection().serialize(miniMessage.deserialize(raw(key))));
    }

    public void send(Player player, String key, Map<String, String> placeholders) {
        String message = raw(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        player.sendMessage(LegacyComponentSerializer.legacySection().serialize(miniMessage.deserialize(message)));
    }
}
