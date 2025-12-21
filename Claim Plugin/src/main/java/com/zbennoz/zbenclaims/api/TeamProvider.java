package com.zbennoz.zbenclaims.api;

import java.util.UUID;

/**
 * Provides a lightweight team label for tab list rendering.
 */
public interface TeamProvider {
    String getTeam(UUID playerId);
}
