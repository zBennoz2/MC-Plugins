package com.zbennoz.zbenclaims.api;

import java.util.UUID;

/**
 * Optional suffix information for a player's active job or profession.
 */
public interface JobProvider {
    String getJob(UUID playerId);
}
