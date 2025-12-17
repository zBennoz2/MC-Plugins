package com.zbennoz.zbenclaims;

import com.zbennoz.zbenclaims.db.Database;
import com.zbennoz.zbenclaims.ranks.RankManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClaimService {

    private final ZBenClaimsPlugin plugin;
    private final Database db;
    private final MessageService msg;
    private final RankManager rankManager;

    private final Map<ChunkKey, Claim> claimCache = new ConcurrentHashMap<>();
    private final Map<Long, Set<UUID>> trustedCache = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Boolean>> flagCache = new ConcurrentHashMap<>();

    public ClaimService(ZBenClaimsPlugin plugin, Database db, MessageService msg, RankManager rankManager) {
        this.plugin = plugin;
        this.db = db;
        this.msg = msg;
        this.rankManager = rankManager;
    }

    public void loadCache() {
        claimCache.clear();
        trustedCache.clear();
        flagCache.clear();

        boolean preload = plugin.getConfig().getBoolean("cache.preloadAllClaims", true);
        if (!preload) return;

        for (Claim c : db.getAllClaims()) {
            claimCache.put(new ChunkKey(c.world(), c.chunkX(), c.chunkZ()), c);
        }
        plugin.getLogger().info("Loaded " + claimCache.size() + " claims into cache.");
    }

    public void reload() {
        loadCache();
    }

    public void shutdown() {
        // nothing for now
    }

    public Optional<Claim> getClaim(World world, int chunkX, int chunkZ) {
        ChunkKey key = new ChunkKey(world.getName(), chunkX, chunkZ);
        Claim cached = claimCache.get(key);
        if (cached != null) return Optional.of(cached);

        Claim c = db.getClaim(world.getName(), chunkX, chunkZ);
        if (c != null) {
            claimCache.put(key, c);
            return Optional.of(c);
        }
        return Optional.empty();
    }

    public boolean isMember(Claim claim, UUID uuid) {
        if (claim.ownerUuid().equals(uuid)) return true;
        return isTrusted(claim.id(), uuid);
    }

    public boolean isTrusted(long claimId, UUID uuid) {
        Set<UUID> trusted = trustedCache.computeIfAbsent(claimId, id -> new HashSet<>(db.getTrusted(id)));
        return trusted.contains(uuid);
    }

    public Map<String, Boolean> getFlags(long claimId) {
        return flagCache.computeIfAbsent(claimId, id -> new HashMap<>(db.getFlags(id)));
    }

    public boolean getFlag(long claimId, String flag, boolean def) {
        return getFlags(claimId).getOrDefault(flag, def);
    }

    public ClaimResult claimChunk(Chunk chunk, UUID owner) {
        World w = chunk.getWorld();
        if (plugin.getConfig().getStringList("claims.disabledWorlds").contains(w.getName())) {
            return ClaimResult.fail("World disabled.");
        }
        if (plugin.getConfig().getBoolean("claims.requireChunkLoaded", true) && !w.isChunkLoaded(chunk.getX(), chunk.getZ())) {
            return ClaimResult.fail("Chunk not loaded.");
        }

        ChunkKey key = new ChunkKey(w.getName(), chunk.getX(), chunk.getZ());
        Claim existing = claimCache.get(key);
        if (existing == null) existing = db.getClaim(w.getName(), chunk.getX(), chunk.getZ());
        if (existing != null) return ClaimResult.alreadyClaimed(existing);

        int limit = rankManager.getClaimLimit(owner);
        int count = db.countClaimsByOwner(owner);
        if (count >= limit) return ClaimResult.limitReached(limit);

        long id = db.createClaim(owner, w.getName(), chunk.getX(), chunk.getZ());
        Claim claim = new Claim(id, owner, w.getName(), chunk.getX(), chunk.getZ());
        claimCache.put(key, claim);

        boolean interactDefault = plugin.getConfig().getBoolean("flags.default.interact_protected", false);
        db.setFlag(id, "interact_protected", interactDefault);
        flagCache.put(id, new HashMap<>(Map.of("interact_protected", interactDefault)));

        return ClaimResult.success(claim);
    }

    public ClaimResult unclaimChunk(Chunk chunk, UUID actor) {
        World w = chunk.getWorld();
        ChunkKey key = new ChunkKey(w.getName(), chunk.getX(), chunk.getZ());
        Claim claim = claimCache.get(key);
        if (claim == null) claim = db.getClaim(w.getName(), chunk.getX(), chunk.getZ());
        if (claim == null) return ClaimResult.notClaimed();

        if (!claim.ownerUuid().equals(actor)) return ClaimResult.notOwner(claim);

        db.deleteClaim(claim.id());
        claimCache.remove(key);
        trustedCache.remove(claim.id());
        flagCache.remove(claim.id());
        return ClaimResult.success(claim);
    }

    public TrustResult trust(Chunk chunk, UUID owner, UUID target) {
        World w = chunk.getWorld();
        Claim claim = getClaim(w, chunk.getX(), chunk.getZ()).orElse(null);
        if (claim == null) return TrustResult.notClaimed();
        if (!claim.ownerUuid().equals(owner)) return TrustResult.notOwner();
        if (claim.ownerUuid().equals(target)) return TrustResult.cannotTrustOwner();

        Set<UUID> trusted = trustedCache.computeIfAbsent(claim.id(), id -> new HashSet<>(db.getTrusted(id)));
        if (trusted.contains(target)) return TrustResult.alreadyTrusted();

        db.addTrusted(claim.id(), target);
        trusted.add(target);
        return TrustResult.success();
    }

    public TrustResult untrust(Chunk chunk, UUID owner, UUID target) {
        World w = chunk.getWorld();
        Claim claim = getClaim(w, chunk.getX(), chunk.getZ()).orElse(null);
        if (claim == null) return TrustResult.notClaimed();
        if (!claim.ownerUuid().equals(owner)) return TrustResult.notOwner();
        if (claim.ownerUuid().equals(target)) return TrustResult.cannotTrustOwner();

        Set<UUID> trusted = trustedCache.computeIfAbsent(claim.id(), id -> new HashSet<>(db.getTrusted(id)));
        if (!trusted.contains(target)) return TrustResult.notTrusted();

        db.removeTrusted(claim.id(), target);
        trusted.remove(target);
        return TrustResult.success();
    }

    public List<Claim> listClaims(UUID owner) {
        return db.getClaimsByOwner(owner);
    }

    public String ownerName(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        if (op.getName() != null) return op.getName();
        return uuid.toString();
    }
}
