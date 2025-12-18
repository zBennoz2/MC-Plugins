package com.zbennoz.zbenteleport.command;

import com.zbennoz.zbenteleport.ZBenTeleportPlugin;
import com.zbennoz.zbenteleport.util.HomeManager;
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

public class HomeCommand implements CommandExecutor, TabCompleter {

    public enum Mode { SET, TELEPORT, DELETE }

    private final ZBenTeleportPlugin plugin;
    private final HomeManager homeManager;
    private final Mode mode;

    public HomeCommand(ZBenTeleportPlugin plugin, HomeManager homeManager, Mode mode) {
        this.plugin = plugin;
        this.homeManager = homeManager;
        this.mode = mode;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }
        String homeName = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "home";
        var homes = homeManager.loadHomes(player.getUniqueId());
        int limit = homeManager.getHomeLimit(player);

        switch (mode) {
            case SET -> {
                if (!player.hasPermission("zbenteleport.sethome")) {
                    player.sendMessage(plugin.messages().component("no-permission"));
                    return true;
                }
                boolean ok = homeManager.setHome(player.getUniqueId(), homeName, player.getLocation(), limit);
                if (!ok) {
                    player.sendMessage(plugin.messages().component("home.limit", Placeholder.unparsed("limit", String.valueOf(limit))));
                } else {
                    player.sendMessage(plugin.messages().component("home.saved", Placeholder.unparsed("name", homeName)));
                }
            }
            case TELEPORT -> {
                if (!player.hasPermission("zbenteleport.home")) {
                    player.sendMessage(plugin.messages().component("no-permission"));
                    return true;
                }
                var location = homes.get(homeName);
                if (location == null) {
                    player.sendMessage(plugin.messages().component("home.notfound", Placeholder.unparsed("name", homeName)));
                    return true;
                }
                plugin.database().saveLastLocationAsync(player.getUniqueId(), player.getLocation(), "teleport");
                player.teleportAsync(location);
                player.sendMessage(plugin.messages().component("home.teleport", Placeholder.unparsed("name", homeName)));
            }
            case DELETE -> {
                if (!player.hasPermission("zbenteleport.delhome")) {
                    player.sendMessage(plugin.messages().component("no-permission"));
                    return true;
                }
                if (homeManager.deleteHome(player.getUniqueId(), homeName)) {
                    player.sendMessage(plugin.messages().component("home.deleted", Placeholder.unparsed("name", homeName)));
                } else {
                    player.sendMessage(plugin.messages().component("home.notfound", Placeholder.unparsed("name", homeName)));
                }
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            return new ArrayList<>(homeManager.loadHomes(player.getUniqueId()).keySet());
        }
        return List.of();
    }
}
