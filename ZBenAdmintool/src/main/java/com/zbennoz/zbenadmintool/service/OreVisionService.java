package com.zbennoz.zbenadmintool.service;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import com.zbennoz.zbenadmintool.hook.ProtocolLibHook;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class OreVisionService {

    private final ZBenAdmintool plugin;
    private final ProtocolLibHook protocolLibHook;
    private final Set<UUID> highlight = Collections.synchronizedSet(new HashSet<>());
    private final Set<UUID> oreXray = Collections.synchronizedSet(new HashSet<>());
    private List<Material> ores;
    private int radius;
    private int interval;
    private int maxBlocks;
    private boolean particlesEnabled;
    private boolean enabled;

    public OreVisionService(ZBenAdmintool plugin, ProtocolLibHook protocolLibHook) {
        this.plugin = plugin;
        this.protocolLibHook = protocolLibHook;
        reload();
        startTask();
    }

    public void reload() {
        radius = plugin.getConfig().getInt("oreVision.radius", 20);
        interval = plugin.getConfig().getInt("oreVision.scanIntervalTicks", 20);
        maxBlocks = plugin.getConfig().getInt("oreVision.maxBlocksPerScan", 12000);
        particlesEnabled = plugin.getConfig().getBoolean("oreVision.particlesEnabled", true);
        enabled = plugin.getConfig().getBoolean("oreVision.enabled", true);
        ores = plugin.getConfig().getStringList("oreVision.ores").stream()
                .map(s -> Material.matchMaterial(s.toUpperCase(Locale.ROOT)))
                .filter(mat -> mat != null)
                .toList();
    }

    public boolean toggleHighlight(Player player) {
        if (!enabled) {
            return false;
        }
        if (highlight.remove(player.getUniqueId())) {
            return false;
        }
        highlight.add(player.getUniqueId());
        return true;
    }

    public boolean toggleOreXray(Player player) {
        if (!enabled) {
            return false;
        }
        if (!protocolLibHook.isAvailable()) {
            return false;
        }
        if (oreXray.remove(player.getUniqueId())) {
            restoreWorld(player);
            return false;
        }
        oreXray.add(player.getUniqueId());
        refreshXray(player);
        return true;
    }

    public boolean isHighlighting(Player player) {
        return highlight.contains(player.getUniqueId());
    }

    public boolean isOreXray(Player player) {
        return oreXray.contains(player.getUniqueId());
    }

    public void clear(Player player) {
        if (highlight.remove(player.getUniqueId()) || oreXray.remove(player.getUniqueId())) {
            restoreWorld(player);
        }
    }

    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                scan();
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    private void scan() {
        if (!particlesEnabled || ores.isEmpty()) return;
        Set<UUID> copy = new HashSet<>(highlight);
        for (UUID id : copy) {
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline()) {
                highlight.remove(id);
                continue;
            }
            highlightOres(player);
        }
    }

    private void highlightOres(Player player) {
        Location center = player.getLocation();
        int scanned = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (scanned++ > maxBlocks) {
                        return;
                    }
                    Location loc = center.clone().add(x, y, z);
                    Material type = loc.getBlock().getType();
                    if (ores.contains(type)) {
                        player.spawnParticle(Particle.GLOW, loc.toCenterLocation(), 1, 0, 0, 0, 0);
                    }
                }
            }
        }
    }

    private void refreshXray(Player player) {
        if (!protocolLibHook.isAvailable()) {
            return;
        }
        Location center = player.getLocation();
        int scanned = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (scanned++ > maxBlocks) {
                        return;
                    }
                    Location loc = center.clone().add(x, y, z);
                    Material type = loc.getBlock().getType();
                    if (!ores.contains(type)) {
                        player.sendBlockChange(loc, Material.AIR.createBlockData());
                    }
                }
            }
        }
    }

    private void restoreWorld(Player player) {
        Location center = player.getLocation();
        int scanned = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (scanned++ > maxBlocks) {
                        return;
                    }
                    Location loc = center.clone().add(x, y, z);
                    player.sendBlockChange(loc, loc.getBlock().getBlockData());
                }
            }
        }
    }
}
