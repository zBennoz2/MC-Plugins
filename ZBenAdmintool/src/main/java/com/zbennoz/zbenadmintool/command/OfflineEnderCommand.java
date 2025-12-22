package com.zbennoz.zbenadmintool.command;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OfflineEnderCommand implements CommandExecutor {

    private final ZBenAdmintool plugin;

    public OfflineEnderCommand(ZBenAdmintool plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur Spieler können dies nutzen.");
            return true;
        }
        if (!plugin.getPermissionResolver().has(player, "zbenadmintool.offec")) {
            sender.sendMessage(plugin.getMessages().raw("no_permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("/offec <spieler>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        plugin.getOfflineInventoryService().openInventory(sender, target, true);
        return true;
    }
}
