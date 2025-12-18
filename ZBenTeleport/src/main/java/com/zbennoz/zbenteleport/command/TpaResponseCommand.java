package com.zbennoz.zbenteleport.command;

import com.zbennoz.zbenteleport.ZBenTeleportPlugin;
import com.zbennoz.zbenteleport.util.TpaManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

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
            if (tpaManager.cancelOutgoing(player)) {
                player.sendMessage(plugin.messages().component("tpa.cancelled"));
            } else {
                player.sendMessage(plugin.messages().component("tpa.no-pending"));
            }
            return true;
        }

        UUID requestId = null;
        if (args.length > 0) {
            try {
                requestId = UUID.fromString(args[0]);
            } catch (IllegalArgumentException ignored) {
            }
        }

        var request = tpaManager.consume(player, requestId);
        if (request == null) {
            player.sendMessage(plugin.messages().component("tpa.no-pending"));
            return true;
        }

        Player requester = Bukkit.getPlayer(request.sender());
        if (requester == null) {
            player.sendMessage(plugin.messages().component("tpa.requester-offline"));
            return true;
        }

        if (Boolean.TRUE.equals(accept)) {
            if (request.type() == TpaManager.RequestType.TPA) {
                requester.teleportAsync(player.getLocation());
            } else {
                player.teleportAsync(requester.getLocation());
            }
            requester.sendMessage(plugin.messages().component("tpa.accepted.sender", Placeholder.unparsed("target", player.getName())));
            player.sendMessage(plugin.messages().component("tpa.accepted.target", Placeholder.unparsed("sender", requester.getName())));
        } else {
            requester.sendMessage(plugin.messages().component("tpa.denied.sender", Placeholder.unparsed("target", player.getName())));
            player.sendMessage(plugin.messages().component("tpa.denied.target", Placeholder.unparsed("sender", requester.getName())));
        }
        return true;
    }
}
