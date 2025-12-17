package com.zbennoz.zbenclaims;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class MessageService {

    private final ZBenClaimsPlugin plugin;
    private LegacyComponentSerializer serializer;

    public MessageService(ZBenClaimsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.serializer = LegacyComponentSerializer.legacyAmpersand();
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String raw = plugin.getConfig().getString("messages." + key, "");
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
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        sender.sendMessage(serializer.deserialize(prefix + raw));
    }

    public void sendProtection(Player p, String key) {
        send(p, key);
    }
}
