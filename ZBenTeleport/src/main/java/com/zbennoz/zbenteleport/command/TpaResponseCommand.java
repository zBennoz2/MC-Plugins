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
import org.jetbrains.annotations.Nullable;

public class TpaResponseCommand implements CommandExecutor {

    private final ZBenTeleportPlugin plugin;
    private final TpaManager tpaManager;
    private final Boolean accept;

    public TpaResponseCommand(ZBenTeleportPlugin plugin, TpaManager tpaManager, @Nullable Boolean accept) {
        this.plugin = plugin;
        this.tpaManager = tpaManager;
        this.accept = accept;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }

        if (accept == null) {
            tpaManager.cancel(player);
            player.sendMessage(Component.text("Request cancelled."));
            return true;
        }

        var targetId = tpaManager.consume(player, accept);
        if (targetId == null) {
            player.sendMessage(Component.text("No pending request."));
            return true;
        }
        Player requester = Bukkit.getPlayer(targetId);
        if (requester == null) {
            player.sendMessage(Component.text("Requester offline."));
            return true;
        }
        if (accept) {
            requester.teleportAsync(player.getLocation());
            requester.sendMessage(Component.text("Teleported."));
            player.sendMessage(Component.text("Accepted request."));
        } else {
            requester.sendMessage(Component.text("Request denied."));
            player.sendMessage(Component.text("Denied request."));
        }
        return true;
    }
}
