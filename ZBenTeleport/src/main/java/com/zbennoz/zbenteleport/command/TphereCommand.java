package com.zbennoz.zbenteleport.command;

import com.zbennoz.zbenteleport.ZBenTeleportPlugin;
import com.zbennoz.zbenteleport.util.PlayerSettingsManager;
import com.zbennoz.zbenteleport.util.TpaManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TphereCommand implements CommandExecutor {

    private final ZBenTeleportPlugin plugin;
    private final TpaManager tpaManager;
    private final PlayerSettingsManager settingsManager;

    public TphereCommand(ZBenTeleportPlugin plugin, TpaManager tpaManager) {
        this.plugin = plugin;
        this.tpaManager = tpaManager;
        this.settingsManager = plugin.playerSettingsManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this."));
            return true;
        }
        if (!player.hasPermission("zbenteleport.tphere")) {
            player.sendMessage(plugin.messages().component("no-permission"));
            return true;
        }
        if (plugin.cooldownManager().isOnCooldown(player.getUniqueId(), "tpa") && !player.hasPermission("zbenteleport.bypass.cooldown")) {
            long remaining = plugin.cooldownManager().remaining(player.getUniqueId(), "tpa") / 1000;
            player.sendMessage(plugin.messages().component("cooldown", Placeholder.unparsed("seconds", String.valueOf(remaining))));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(plugin.messages().component("usage.tphere"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || target.equals(player)) {
            player.sendMessage(plugin.messages().component("tpa.invalid-target"));
            return true;
        }
        if (!settingsManager.isTpaEnabled(target.getUniqueId())) {
            player.sendMessage(plugin.messages().component("tpa.target-disabled"));
            return true;
        }
        if (settingsManager.isBlocked(target.getUniqueId(), player.getUniqueId())) {
            player.sendMessage(plugin.messages().component("tpa.blocked"));
            return true;
        }

        var status = tpaManager.createRequest(player, target, TpaManager.RequestType.TPA_HERE);
        if (status.result() == TpaManager.RequestResult.DUPLICATE) {
            player.sendMessage(plugin.messages().component("tpa.already-sent"));
            return true;
        }

        Component message = plugin.messages().component("tpa.request.here",
                Placeholder.unparsed("sender", player.getName()),
                Placeholder.unparsed("id", status.request().id().toString()));

        target.sendMessage(message);
        player.sendMessage(plugin.messages().component("tpa.here.sent", Placeholder.unparsed("target", target.getName())));
        plugin.cooldownManager().apply(player.getUniqueId(), "tpa");
        if (status.result() == TpaManager.RequestResult.REPLACED) {
            player.sendMessage(plugin.messages().component("tpa.replaced"));
        }
        return true;
    }
}
