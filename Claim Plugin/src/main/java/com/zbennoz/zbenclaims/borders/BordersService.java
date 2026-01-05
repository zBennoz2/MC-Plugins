package com.zbennoz.zbenclaims.borders;

import com.zbennoz.zbenclaims.Claim;
import com.zbennoz.zbenclaims.ClaimService;
import com.zbennoz.zbenclaims.ZBenClaimsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BordersService implements Listener {

    private final ZBenClaimsPlugin plugin;
    private final ClaimService claimService;
    private final Set<UUID> toggledPlayers = ConcurrentHashMap.newKeySet();

    private BukkitTask task;

    public BordersService(ZBenClaimsPlugin plugin, ClaimService claimService) {
        this.plugin = plugin;
        this.claimService = claimService;
    }

    public void reload() {
        stop();
        if (plugin.getConfig().getBoolean("borders.enabled", true)) {
            start();
        }
    }

    public boolean toggle(Player player) {
        if (toggledPlayers.remove(player.getUniqueId())) {
            return false;
        }
        toggledPlayers.add(player.getUniqueId());
        return true;
    }

    public boolean isEnabled(Player player) {
        return toggledPlayers.contains(player.getUniqueId());
    }

    private void start() {
        long interval = plugin.getConfig().getLong("borders.updateIntervalTicks", 15L);
        if (interval < 1) interval = 1;

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::renderAll, interval, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void renderAll() {
        if (!plugin.getConfig().getBoolean("borders.enabled", true)) return;

        for (UUID uuid : toggledPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            renderFor(player);
        }
    }

    private void renderFor(Player player) {
        World world = player.getWorld();
        int radius = plugin.getConfig().getInt("borders.radiusChunks", 3);
        double spacing = Math.max(0.25, plugin.getConfig().getDouble("borders.particleSpacing", 1.5));
        boolean showOnlyOwn = plugin.getConfig().getBoolean("borders.showOnlyOwnClaims", false);
        boolean canSeeAll = player.hasPermission("claim.borders.seeall");

        int centerX = player.getLocation().getChunk().getX();
        int centerZ = player.getLocation().getChunk().getZ();

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                Optional<Claim> claimOpt = claimService.getClaim(world, x, z);
                if (claimOpt.isEmpty()) continue;

                Claim claim = claimOpt.get();
                if (showOnlyOwn && !canSeeAll && !claimService.isMember(claim, player.getUniqueId())) {
                    continue;
                }

                renderChunkBorder(player, world, x, z, spacing);
            }
        }
    }

    private void renderChunkBorder(Player player, World world, int chunkX, int chunkZ, double spacing) {
        double baseX = chunkX << 4;
        double baseZ = chunkZ << 4;
        double maxX = baseX + 16;
        double maxZ = baseZ + 16;
        double y = determineY(player);

        Particle.DustOptions dust = new Particle.DustOptions(Color.LIME, 1.2F);

        for (double x = baseX; x <= maxX; x += spacing) {
            spawnParticle(player, new Location(world, x, y, baseZ), dust);
            spawnParticle(player, new Location(world, x, y, maxZ), dust);
        }
        for (double z = baseZ; z <= maxZ; z += spacing) {
            spawnParticle(player, new Location(world, baseX, y, z), dust);
            spawnParticle(player, new Location(world, maxX, y, z), dust);
        }
    }

    private void spawnParticle(Player player, Location loc, Particle.DustOptions dust) {
        player.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dust, true);
    }

    private double determineY(Player player) {
        String mode = plugin.getConfig().getString("borders.yMode", "PLAYER_FEET").toUpperCase();
        Location loc = player.getLocation();
        return switch (mode) {
            case "SEA_LEVEL" -> player.getWorld().getSeaLevel();
            case "WORLD_SURFACE" -> player.getWorld().getHighestBlockYAt(loc) + 1.0;
            default -> loc.getY();
        };
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        toggledPlayers.remove(event.getPlayer().getUniqueId());
    }
}
