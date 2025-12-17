package com.zbennoz.zbencore.util;

import org.bukkit.command.CommandSender;

public final class Perm {
    private Perm() {}

    public static boolean has(CommandSender sender, String node) {
        // Owner rank bypass:
        if (sender.hasPermission("zben.owner")) return true;
        return sender.hasPermission(node);
    }
}
