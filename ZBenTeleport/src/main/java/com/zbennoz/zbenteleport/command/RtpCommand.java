package com.zbennoz.zbenteleport.command;

import com.zbennoz.zbenteleport.ZBenTeleportPlugin;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RtpCommand implements CommandExecutor {

    private final ZBenTeleportPlugin plugin;

    public RtpCommand(ZBenTeleportPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }
        if (!player.hasPermission("zbenteleport.rtp")) {
            player.sendMessage(plugin.messages().component("no-permission"));
            return true;
        }
        if (!plugin.getConfig().getBoolean("rtp.enabled", true)) {
            player.sendMessage(plugin.messages().component("rtp.disabled"));
            return true;
        }
        long cooldownMs = plugin.getConfig().getLong("rtp.cooldownSeconds", 30) * 1000L;
        if (plugin.cooldownManager().isOnCooldown(player.getUniqueId(), "rtp") && !player.hasPermission("zbenteleport.bypass.cooldown")) {
            long remaining = plugin.cooldownManager().remaining(player.getUniqueId(), "rtp") / 1000;
            player.sendMessage(plugin.messages().component("cooldown", Placeholder.unparsed("seconds", String.valueOf(remaining))));
            return true;
        }

        World world = player.getWorld();
        List<String> allowed = plugin.getConfig().getStringList("rtp.worldsAllowed");
        if (!allowed.isEmpty() && !allowed.contains(world.getName())) {
            player.sendMessage(plugin.messages().component("rtp.not-allowed"));
            return true;
        }

        int minRadius = plugin.getConfig().getInt("rtp.minRadius", 1000);
        int maxRadius = Math.max(minRadius + 1, plugin.getConfig().getInt("rtp.maxRadius", 5000));
        int attempts = plugin.getConfig().getInt("rtp.maxAttempts", 25);

        Location found = null;
        for (int i = 0; i < attempts; i++) {
            Location candidate = randomLocation(world, player.getLocation(), minRadius, maxRadius);
            if (isSafe(candidate)) {
                found = candidate;
                break;
            }
        }

        if (found == null) {
            player.sendMessage(plugin.messages().component("rtp.failed"));
            return true;
        }

        plugin.cooldownManager().apply(player.getUniqueId(), "rtp", cooldownMs);
        plugin.database().saveLastLocationAsync(player.getUniqueId(), player.getLocation(), "teleport");
        player.teleportAsync(found);
        player.sendMessage(plugin.messages().component("rtp.success", Placeholder.unparsed("x", String.valueOf(found.getBlockX())),
                Placeholder.unparsed("y", String.valueOf(found.getBlockY())),
                Placeholder.unparsed("z", String.valueOf(found.getBlockZ()))));
        return true;
    }

    private Location randomLocation(World world, Location center, int minRadius, int maxRadius) {
        double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
        double distance = ThreadLocalRandom.current().nextDouble(minRadius, maxRadius);
        int x = (int) Math.round(center.getX() + Math.cos(angle) * distance);
        int z = (int) Math.round(center.getZ() + Math.sin(angle) * distance);
        int y = world.getHighestBlockYAt(x, z);
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    private boolean isSafe(Location location) {
        Block block = location.getBlock();
        Block below = block.getRelative(0, -1, 0);
        if (below.isEmpty() || below.isLiquid()) {
            return false;
        }
        Material type = below.getType();
        if (type == Material.LAVA || type == Material.FIRE || type == Material.CACTUS) {
            return false;
        }
        if (block.isLiquid() || block.getType().isSolid()) {
            return false;
        }
        Block head = block.getRelative(0, 1, 0);
        return !head.getType().isSolid() && !head.isLiquid();
    }
}
