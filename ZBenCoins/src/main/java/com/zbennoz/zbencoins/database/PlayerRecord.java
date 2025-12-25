package com.zbennoz.zbencoins.database;

import java.util.UUID;

/**
 * Datenmodell für gespeicherte Spieler.
 */
public class PlayerRecord {
    private final UUID uuid;
    private final String name;
    private final long coins;

    public PlayerRecord(UUID uuid, String name, long coins) {
        this.uuid = uuid;
        this.name = name;
        this.coins = coins;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public long getCoins() {
        return coins;
    }
}
