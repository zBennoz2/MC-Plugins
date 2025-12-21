package com.zbennoz.zbenlootr.cache;

import org.bukkit.inventory.Inventory;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class LootCache {

    private static class CacheEntry {
        private final Inventory inventory;
        private volatile long lastAccess;

        CacheEntry(Inventory inventory) {
            this.inventory = inventory;
            touch();
        }

        Inventory getInventory() {
            touch();
            return inventory;
        }

        void touch() {
            lastAccess = Instant.now().getEpochSecond();
        }

        boolean isExpired(long expireSeconds) {
            return Instant.now().getEpochSecond() - lastAccess >= expireSeconds;
        }
    }

    private final Map<String, CacheEntry> cache;
    private final int maxEntries;
    private final long expireSeconds;

    public LootCache(int maxEntries, long expireSeconds) {
        this.maxEntries = maxEntries;
        this.expireSeconds = expireSeconds;
        this.cache = createCache(maxEntries);
    }

    private Map<String, CacheEntry> createCache(final int limit) {
        return new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > limit;
            }
        };
    }

    public synchronized Optional<Inventory> get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired(expireSeconds)) {
            cache.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.getInventory());
    }

    public synchronized void put(String key, Inventory inventory) {
        cache.put(key, new CacheEntry(inventory));
    }

    public synchronized void invalidate(String key) {
        cache.remove(key);
    }

    public synchronized void clear() {
        cache.clear();
    }

    public synchronized int size() {
        return cache.size();
    }

    @Override
    public synchronized String toString() {
        return "LootCache{size=" + cache.size() + ", max=" + maxEntries + ", ttl=" + expireSeconds + "}";
    }
}
