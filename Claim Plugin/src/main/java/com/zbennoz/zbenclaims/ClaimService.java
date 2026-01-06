package com.zbennoz.zbenclaims;

import com.zbennoz.zbenclaims.db.Database;
import com.zbennoz.zbenadmintool.api.ZBenRankAPI;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ClaimService {

    private final ZBenClaimsPlugin plugin;
    private final Database db;
    private final MessageService msg;
    private boolean missingRankApiWarned = false;

    private final ExecutorService dbWriter = Executors.newSingleThreadExecutor();

    private final Map<ChunkKey, Claim> claimCache = new ConcurrentHashMap<>();
    private final Map<Long, Set<UUID>> trustedCache = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Boolean>> flagCache = new ConcurrentHashMap<>();

    public ClaimService(ZBenClaimsPlugin plugin, Database db, MessageService msg) {
        this.plugin = plugin;
        this.db = db;
        this.msg = msg;
    }

    public void loadCache() {
        claimCache.clear();
        trustedCache.clear();
        flagCache.clear();

        for (Claim c : db.getAllClaims()) {
            claimCache.put(new ChunkKey(c.worldUuid(), c.chunkX(), c.chunkZ()), c);
            trustedCache.put(c.id(), new HashSet<>(db.getTrusted(c.id())));
            flagCache.put(c.id(), new HashMap<>(db.getFlags(c.id())));
        }
        plugin.getLogger().info("Loaded " + claimCache.size() + " claims into cache.");
    }

    public void reload() {
        loadCache();
    }

    public void shutdown() {
        dbWriter.shutdown();
        try { dbWriter.awaitTermination(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
    }

    public Optional<Claim> getClaim(World world, int chunkX, int chunkZ) {
        ChunkKey key = new ChunkKey(world.getUID(), chunkX, chunkZ);
        Claim cached = claimCache.get(key);
        if (cached != null) return Optional.of(cached);
        return Optional.empty();
    }

    public boolean isChunkClaimed(World world, int chunkX, int chunkZ) {
        ChunkKey key = new ChunkKey(world.getUID(), chunkX, chunkZ);
        return claimCache.containsKey(key);
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

        ChunkKey key = new ChunkKey(w.getUID(), chunk.getX(), chunk.getZ());
        Claim existing = claimCache.get(key);
        if (existing != null) return ClaimResult.alreadyClaimed(existing);

        ZBenRankAPI rankApi = plugin.getService(ZBenRankAPI.class).orElse(null);
        if (rankApi == null) {
            warnMissingRankApi();
            return ClaimResult.fail(dependencyMissingMessage());
        }

        int count = db.countClaimsByOwner(owner);
        int limit;
        try {
            limit = rankApi.getMaxClaimChunks(owner);
        } catch (Exception ex) {
            warnMissingRankApi();
            return ClaimResult.fail(dependencyMissingMessage());
        }

        if (count >= limit) return ClaimResult.limitReached(count, limit);

        boolean interactDefault = plugin.getConfig().getBoolean("flags.default.interact_protected", false);
        Map<String, Boolean> defaults = new HashMap<>();
        defaults.put("interact_protected", interactDefault);

        dbWriter.submit(() -> {
            long id = db.createClaim(owner, w.getName(), w.getUID(), chunk.getX(), chunk.getZ());
            Claim stored = new Claim(id, owner, w.getName(), w.getUID(), chunk.getX(), chunk.getZ());
            claimCache.put(key, stored);
            defaults.forEach((f, v) -> db.setFlag(id, f, v));
            flagCache.put(id, new HashMap<>(defaults));
        });

        Claim provisional = new Claim(-1, owner, w.getName(), w.getUID(), chunk.getX(), chunk.getZ());
        claimCache.put(key, provisional);
        return ClaimResult.success(provisional);
    }

    public ClaimResult unclaimChunk(Chunk chunk, UUID actor) {
        World w = chunk.getWorld();
        ChunkKey key = new ChunkKey(w.getUID(), chunk.getX(), chunk.getZ());
        Claim claim = claimCache.get(key);
        if (claim == null) return ClaimResult.notClaimed();

        if (!claim.ownerUuid().equals(actor)) return ClaimResult.notOwner(claim);

        claimCache.remove(key);
        trustedCache.remove(claim.id());
        flagCache.remove(claim.id());
        dbWriter.submit(() -> db.deleteClaim(claim.id()));
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

        trusted.add(target);
        dbWriter.submit(() -> db.addTrusted(claim.id(), target));
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

        trusted.remove(target);
        dbWriter.submit(() -> db.removeTrusted(claim.id(), target));
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

    private void warnMissingRankApi() {
        if (missingRankApiWarned) return;
        missingRankApiWarned = true;
        plugin.getLogger().warning("ZBenAdmintool oder ZBenRankAPI nicht verfügbar – Claims können nicht gesetzt werden.");
    }

    private String dependencyMissingMessage() {
        String msg = this.msg.get("claimDependencyMissing");
        if (msg == null || msg.isBlank()) {
            return "§cClaim-System benötigt ZBenAdmintool. Bitte informiere einen Admin.";
        }
        return msg;
    }
}
