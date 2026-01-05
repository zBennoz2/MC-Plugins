package com.zbennoz.zbencoins.config;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.util.Text;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Lädt Config- und Nachrichten-Dateien.
 */
public class ConfigManager {

    private final ZBenCoinsPlugin plugin;
    private FileConfiguration messages;

    public ConfigManager(ZBenCoinsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        loadMessages();
    }

    private void loadMessages() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public String message(String key) {
        return Text.colorize(messages.getString(key, key));
    }

    public String message(String key, Map<String, String> placeholders) {
        return Text.format(messages.getString(key, key), placeholders);
    }

    public boolean isDebug() {
        return plugin.getConfig().getBoolean("debug", false);
    }
}
