package com.zbennoz.zbenenchants.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Speichert Cooldowns pro Spieler.
 */
public class CooldownManager {

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public boolean isOnCooldown(UUID uuid) {
        Long until = cooldowns.get(uuid);
        return until != null && until > System.currentTimeMillis();
    }

    public long getRemaining(UUID uuid) {
        Long until = cooldowns.get(uuid);
        if (until == null) {
            return 0L;
        }
        return Math.max(0L, until - System.currentTimeMillis());
    }

    public void start(UUID uuid, long cooldownMillis) {
        cooldowns.put(uuid, System.currentTimeMillis() + cooldownMillis);
    }
}
