package com.zbennoz.zbencore.commands;

import com.zbennoz.zbencore.branding.BrandingService;
import com.zbennoz.zbencore.util.Msg;
import com.zbennoz.zbencore.util.Perm;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class AboutCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final BrandingService branding;

    public AboutCommand(JavaPlugin plugin, BrandingService branding) {
        this.plugin = plugin;
        this.branding = branding;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!Perm.has(sender, "zben.about")) {
            sender.sendMessage(Msg.pref(plugin, plugin.getConfig().getString("messages.noPermission")));
            return true;
        }

        sender.sendMessage(Msg.pref(plugin, "&aZBenCore &7- Info"));
        sender.sendMessage(branding.aboutLine1());
        sender.sendMessage(branding.aboutLine2());
        return true;
    }
}
