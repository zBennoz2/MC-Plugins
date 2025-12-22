package com.zbennoz.zbenadmintool.command;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LogsCommand implements CommandExecutor {

    private final ZBenAdmintool plugin;

    public LogsCommand(ZBenAdmintool plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur Spieler können das nutzen.");
            return true;
        }
        if (!plugin.getPermissionResolver().has(player, "zbenadmintool.logs")) {
            player.sendMessage(plugin.getMessages().raw("no_permission"));
            return true;
        }
        if (args.length < 4) {
            player.sendMessage("/logs <block|chest> <x> <y> <z> [world] [seite]");
            return true;
        }
        String type = args[0];
        int x = Integer.parseInt(args[1]);
        int y = Integer.parseInt(args[2]);
        int z = Integer.parseInt(args[3]);
        World world = args.length >= 5 ? Bukkit.getWorld(args[4]) : player.getWorld();
        int page = args.length >= 6 ? Integer.parseInt(args[5]) : 1;
        if (world == null) {
            player.sendMessage("Welt nicht gefunden.");
            return true;
        }
        Location loc = new Location(world, x, y, z);
        if (type.equalsIgnoreCase("block")) {
            plugin.getLogManager().sendBlockLogs(player, loc, page);
        } else {
            if (!plugin.getConfig().getBoolean("logging.containers.enabled", true)) {
                player.sendMessage(plugin.getMessages().raw("inspect.containers_disabled"));
                return true;
            }
            plugin.getLogManager().sendContainerLogs(player, loc, page);
        }
        return true;
    }
}
