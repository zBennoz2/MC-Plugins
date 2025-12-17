package com.zbennoz.zbenclaims.db;

import com.zbennoz.zbenclaims.Claim;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface Database {
    void init();
    void close();

    long createClaim(UUID owner, String world, int chunkX, int chunkZ);
    void deleteClaim(long claimId);
    Claim getClaim(String world, int chunkX, int chunkZ);
    Claim getClaimById(long claimId);
    List<Claim> getClaimsByOwner(UUID owner);
    List<Claim> getAllClaims();
    int countClaimsByOwner(UUID owner);

    List<UUID> getTrusted(long claimId);
    void addTrusted(long claimId, UUID uuid);
    void removeTrusted(long claimId, UUID uuid);

    Map<String, Boolean> getFlags(long claimId);
    void setFlag(long claimId, String flag, boolean value);

    String getPlayerRank(UUID player);
    void setPlayerRank(UUID player, String rank);
}
