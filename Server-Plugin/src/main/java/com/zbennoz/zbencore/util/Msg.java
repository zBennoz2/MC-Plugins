package com.zbennoz.zbencore.util;

import org.bukkit.plugin.java.JavaPlugin;

public final class Msg {
    private Msg() {}

    public static String color(String s) {
        if (s == null) return "";
        return s.replace("&", "§");
    }

    public static String pref(JavaPlugin plugin, String msg) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&aZBenCore&8]&r ");
        return color(prefix + (msg == null ? "" : msg));
    }
}
