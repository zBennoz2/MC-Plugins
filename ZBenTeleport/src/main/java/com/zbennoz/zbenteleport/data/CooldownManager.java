package com.zbennoz.zbenteleport.data;

import com.zbennoz.zbenteleport.ZBenTeleportPlugin;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final ZBenTeleportPlugin plugin;
    private final Map<String, Long> cooldowns = new HashMap<>();

    public CooldownManager(ZBenTeleportPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isOnCooldown(UUID playerId, String key) {
        long now = Instant.now().toEpochMilli();
        long end = cooldowns.getOrDefault(playerKey(playerId, key), 0L);
        if (end <= now) {
            cooldowns.remove(playerKey(playerId, key));
            return false;
        }
        return true;
    }

    public long remaining(UUID playerId, String key) {
        long now = Instant.now().toEpochMilli();
        return Math.max(0L, cooldowns.getOrDefault(playerKey(playerId, key), 0L) - now);
    }

    public void apply(UUID playerId, String key) {
        long duration = plugin.getConfig().getLong("cooldowns." + key, 0L);
        if (duration <= 0) {
            cooldowns.remove(playerKey(playerId, key));
            return;
        }
        cooldowns.put(playerKey(playerId, key), Instant.now().toEpochMilli() + duration);
    }

    private String playerKey(UUID playerId, String key) {
        return playerId + ":" + key;
    }
}
