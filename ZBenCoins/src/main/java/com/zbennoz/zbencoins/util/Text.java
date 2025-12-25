package com.zbennoz.zbencoins.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ChatColor;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;

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

    public static List<String> wrap(String text, int length) {
        List<String> lines = new ArrayList<>();
        if (text == null) {
            return lines;
        }
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (current.length() + word.length() + 1 > length) {
                lines.add(current.toString());
                current = new StringBuilder();
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(word);
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    public static String strip(String input) {
        return ChatColor.stripColor(colorize(input));
    }
}
