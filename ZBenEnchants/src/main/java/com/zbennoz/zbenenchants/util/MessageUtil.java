package com.zbennoz.zbenenchants.util;

import com.zbennoz.zbenenchants.core.ZBenEnchantsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

/**
 * Nachrichtentool mit Prefix und MiniMessage-Unterstützung.
 */
public final class MessageUtil {

    private MessageUtil() {
    }

    public static void send(ZBenEnchantsPlugin plugin, CommandSender sender, String path) {
        String message = plugin.getMessage(path);
        if (message == null || message.isEmpty()) {
            return;
        }
        sender.sendMessage(parse(plugin, message));
    }

    public static void send(ZBenEnchantsPlugin plugin, CommandSender sender, String path, String replacement) {
        String message = plugin.getMessage(path);
        if (message == null || message.isEmpty()) {
            return;
        }
        sender.sendMessage(parse(plugin, message.replace("{value}", replacement)));
    }

    public static Component parse(ZBenEnchantsPlugin plugin, String raw) {
        String withPrefix = plugin.getPrefix() + raw;
        return MiniMessage.miniMessage().deserialize(withPrefix);
    }
}
