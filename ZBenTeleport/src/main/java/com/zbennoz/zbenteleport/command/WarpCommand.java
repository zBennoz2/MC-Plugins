package com.zbennoz.zbenteleport.command;

import com.zbennoz.zbenteleport.ZBenTeleportPlugin;
import com.zbennoz.zbenteleport.util.WarpManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WarpCommand implements CommandExecutor, TabCompleter {

    public enum Mode {
        SET,
        DELETE,
        TELEPORT,
        LIST
    }

    private final ZBenTeleportPlugin plugin;
    private final WarpManager warpManager;
    private final Mode mode;

    public WarpCommand(ZBenTeleportPlugin plugin, WarpManager warpManager, Mode mode) {
        this.plugin = plugin;
        this.warpManager = warpManager;
        this.mode = mode;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this."));
            return true;
        }

        switch (mode) {
            case SET -> {
                if (!player.hasPermission("zbenteleport.warp.set")) {
                    player.sendMessage(plugin.messages().component("no-permission"));
                    return true;
                }
                if (args.length == 0) {
                    player.sendMessage(plugin.messages().component("usage.setwarp"));
                    return true;
                }
                String name = args[0].toLowerCase(Locale.ROOT);
                warpManager.setWarp(name, player.getLocation());
                player.sendMessage(plugin.messages().component("warp.set", Placeholder.unparsed("name", name)));
            }
            case DELETE -> {
                if (!player.hasPermission("zbenteleport.warp.del")) {
                    player.sendMessage(plugin.messages().component("no-permission"));
                    return true;
                }
                if (args.length == 0) {
                    player.sendMessage(plugin.messages().component("usage.delwarp"));
                    return true;
                }
                String name = args[0].toLowerCase(Locale.ROOT);
                if (warpManager.deleteWarp(name)) {
                    player.sendMessage(plugin.messages().component("warp.deleted", Placeholder.unparsed("name", name)));
                } else {
                    player.sendMessage(plugin.messages().component("warp.notfound", Placeholder.unparsed("name", name)));
                }
            }
            case TELEPORT -> {
                if (!player.hasPermission("zbenteleport.warp.use")) {
                    player.sendMessage(plugin.messages().component("no-permission"));
                    return true;
                }
                if (args.length == 0) {
                    player.sendMessage(plugin.messages().component("usage.warp"));
                    return true;
                }
                String name = args[0].toLowerCase(Locale.ROOT);
                var location = warpManager.getWarp(name);
                if (location == null) {
                    player.sendMessage(plugin.messages().component("warp.notfound", Placeholder.unparsed("name", name)));
                    return true;
                }
                plugin.database().saveLastLocationAsync(player.getUniqueId(), player.getLocation(), "teleport");
                player.teleportAsync(location);
                player.sendMessage(plugin.messages().component("warp.teleport", Placeholder.unparsed("name", name)));
            }
            case LIST -> {
                if (!player.hasPermission("zbenteleport.warp.list")) {
                    player.sendMessage(plugin.messages().component("no-permission"));
                    return true;
                }
                if (warpManager.warps().isEmpty()) {
                    player.sendMessage(plugin.messages().component("warp.none"));
                    return true;
                }
                String joined = String.join(", ", warpManager.warps().keySet());
                player.sendMessage(plugin.messages().component("warp.list", Placeholder.unparsed("warps", joined)));
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (mode == Mode.TELEPORT || mode == Mode.DELETE) {
            if (args.length == 1) {
                return new ArrayList<>(warpManager.warps().keySet());
            }
        }
        return List.of();
    }
}
