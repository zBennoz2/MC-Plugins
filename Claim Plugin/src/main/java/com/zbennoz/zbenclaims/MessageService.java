package com.zbennoz.zbenclaims;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class MessageService {

    private final ZBenClaimsPlugin plugin;
    private LegacyComponentSerializer serializer;
    private FileConfiguration messages;
    private File messagesFile;

    public MessageService(ZBenClaimsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.serializer = LegacyComponentSerializer.legacyAmpersand();
        ensureMessagesFile();
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);
        try (InputStreamReader reader = new InputStreamReader(plugin.getResource("messages.yml"), StandardCharsets.UTF_8)) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
            this.messages.setDefaults(defaults);
        } catch (Exception ignored) { }
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        String prefix = messages.getString("prefix", "");
        String raw = messages.getString(key, "");
        String msg = (prefix + raw);
        for (var e : placeholders.entrySet()) {
            msg = msg.replace("%" + e.getKey() + "%", e.getValue());
        }
        sender.sendMessage(serializer.deserialize(msg));
    }

    public Component comp(String raw) {
        return serializer.deserialize(raw);
    }

    public void sendRaw(CommandSender sender, String raw) {
        String prefix = messages.getString("prefix", "");
        sender.sendMessage(serializer.deserialize(prefix + raw));
    }

    public void sendProtection(Player p, String key) {
        send(p, key);
    }

    public String get(String key) {
        return messages.getString(key, "");
    }

    private void ensureMessagesFile() {
        if (messagesFile == null) {
            messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        }
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
    }
}
