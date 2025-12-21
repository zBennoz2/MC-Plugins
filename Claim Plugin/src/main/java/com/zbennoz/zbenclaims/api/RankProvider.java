package com.zbennoz.zbenclaims.api;

import java.util.UUID;

/**
 * Lightweight rank provider interface exposed via Bukkit services.
 */
public interface RankProvider {
    RankView getRank(UUID playerId);
}
