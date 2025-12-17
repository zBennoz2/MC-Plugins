package com.zbennoz.zbenteleport.command;

import com.zbennoz.zbenteleport.ZBenTeleportPlugin;
import com.zbennoz.zbenteleport.util.TpaManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TpaCommand implements CommandExecutor {

    private final ZBenTeleportPlugin plugin;
    private final TpaManager tpaManager;

    public TpaCommand(ZBenTeleportPlugin plugin, TpaManager tpaManager) {
        this.plugin = plugin;
        this.tpaManager = tpaManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }
        if (!player.hasPermission("zbenteleport.tpa")) {
            player.sendMessage(Component.text("No permission."));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /tpa <player>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || target.equals(player)) {
            player.sendMessage(Component.text("Target not found."));
            return true;
        }
        tpaManager.sendRequest(player, target);
        target.sendMessage(Component.text(player.getName() + " requested to teleport to you. /tpaccept or /tpdeny"));
        player.sendMessage(Component.text("Request sent."));
        return true;
    }
}
