package com.zbennoz.zbenteleport.util;

import com.zbennoz.zbenteleport.data.TeleportDatabase;
import org.bukkit.OfflinePlayer;

import java.util.*;

public class PlayerSettingsManager {

    private final TeleportDatabase database;
    private final Map<UUID, Boolean> tpaToggles = new HashMap<>();
    private final Map<UUID, Set<UUID>> blockLists = new HashMap<>();

    public PlayerSettingsManager(TeleportDatabase database) {
        this.database = database;
    }

    public boolean isTpaEnabled(UUID playerId) {
        return tpaToggles.computeIfAbsent(playerId, database::isTpaEnabled);
    }

    public boolean toggle(UUID playerId) {
        boolean enabled = !isTpaEnabled(playerId);
        tpaToggles.put(playerId, enabled);
        database.setTpaEnabledAsync(playerId, enabled);
        return enabled;
    }

    public boolean isBlocked(UUID owner, UUID requester) {
        return blockLists.computeIfAbsent(owner, id -> new HashSet<>(database.loadBlocked(id))).contains(requester);
    }

    public boolean addBlock(UUID owner, UUID blocked) {
        Set<UUID> list = blockLists.computeIfAbsent(owner, id -> new HashSet<>(database.loadBlocked(id)));
        boolean added = list.add(blocked);
        if (added) {
            database.addBlockAsync(owner, blocked);
        }
        return added;
    }

    public boolean removeBlock(UUID owner, UUID blocked) {
        Set<UUID> list = blockLists.computeIfAbsent(owner, id -> new HashSet<>(database.loadBlocked(id)));
        boolean removed = list.remove(blocked);
        if (removed) {
            database.removeBlockAsync(owner, blocked);
        }
        return removed;
    }

    public Set<UUID> getBlocks(UUID owner) {
        return new HashSet<>(blockLists.computeIfAbsent(owner, id -> new HashSet<>(database.loadBlocked(id))));
    }
}
