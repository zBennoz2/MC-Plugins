package com.zbennoz.zbencoins.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ChatColor;

import java.util.Map;

/**
 * Hilfsfunktionen für Farben und Platzhalter.
 */
public final class Text {

    private Text() {
    }

    public static String colorize(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public static String format(String input, Map<String, String> placeholders) {
        String result = input;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                result = result.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return colorize(result);
    }

    public static Component component(String input) {
        return MiniMessage.miniMessage().deserialize(ChatColor.translateAlternateColorCodes('&', input));
    }
}
