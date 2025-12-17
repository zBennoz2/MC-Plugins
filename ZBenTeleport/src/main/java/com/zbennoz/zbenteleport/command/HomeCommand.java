package com.zbennoz.zbenteleport.command;

import com.zbennoz.zbenteleport.ZBenTeleportPlugin;
import com.zbennoz.zbenteleport.util.HomeManager;
import net.kyori.adventure.text.Component;
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
                boolean ok = homeManager.setHome(player.getUniqueId(), homeName, player.getLocation(), limit);
                if (!ok) {
                    player.sendMessage(Component.text("Home limit reached."));
                } else {
                    player.sendMessage(Component.text("Home saved."));
                }
            }
            case TELEPORT -> {
                var location = homes.get(homeName);
                if (location == null) {
                    player.sendMessage(Component.text("Home not found."));
                    return true;
                }
                player.teleportAsync(location);
                plugin.database().saveLastLocationAsync(player.getUniqueId(), player.getLocation(), "teleport");
                player.sendMessage(Component.text("Teleported to home."));
            }
            case DELETE -> {
                if (homeManager.deleteHome(player.getUniqueId(), homeName)) {
                    player.sendMessage(Component.text("Home deleted."));
                } else {
                    player.sendMessage(Component.text("Home not found."));
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
