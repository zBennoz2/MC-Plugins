package com.zbennoz.zbencoins.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Datenzugriff für Spieler.
 */
public class PlayerDao {

    private final Connection connection;

    public PlayerDao(Connection connection) {
        this.connection = connection;
    }

    public synchronized PlayerRecord find(UUID uuid) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT uuid, name, coins FROM players WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new PlayerRecord(UUID.fromString(rs.getString("uuid")), rs.getString("name"), rs.getLong("coins"));
                }
            }
        }
        return null;
    }

    public synchronized PlayerRecord findByName(String name) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT uuid, name, coins FROM players WHERE LOWER(name) = LOWER(?)")) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new PlayerRecord(UUID.fromString(rs.getString("uuid")), rs.getString("name"), rs.getLong("coins"));
                }
            }
        }
        return null;
    }

    public synchronized void insert(UUID uuid, String name, long startingCoins) throws SQLException {
        long now = Instant.now().getEpochSecond();
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR IGNORE INTO players(uuid, name, coins, created_at, updated_at) VALUES (?, ?, ?, ?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            stmt.setLong(3, startingCoins);
            stmt.setLong(4, now);
            stmt.setLong(5, now);
            stmt.executeUpdate();
        }
    }

    public synchronized void updateName(UUID uuid, String name) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("UPDATE players SET name = ?, updated_at = ? WHERE uuid = ?")) {
            stmt.setString(1, name);
            stmt.setLong(2, Instant.now().getEpochSecond());
            stmt.setString(3, uuid.toString());
            stmt.executeUpdate();
        }
    }

    public synchronized void setCoins(UUID uuid, long coins) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("UPDATE players SET coins = ?, updated_at = ? WHERE uuid = ?")) {
            stmt.setLong(1, coins);
            stmt.setLong(2, Instant.now().getEpochSecond());
            stmt.setString(3, uuid.toString());
            stmt.executeUpdate();
        }
    }

    public synchronized void addCoins(UUID uuid, long delta) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("UPDATE players SET coins = coins + ?, updated_at = ? WHERE uuid = ?")) {
            stmt.setLong(1, delta);
            stmt.setLong(2, Instant.now().getEpochSecond());
            stmt.setString(3, uuid.toString());
            stmt.executeUpdate();
        }
    }

    public synchronized List<PlayerRecord> topBalances(int limit) throws SQLException {
        List<PlayerRecord> list = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement("SELECT uuid, name, coins FROM players ORDER BY coins DESC LIMIT ?")) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new PlayerRecord(UUID.fromString(rs.getString("uuid")), rs.getString("name"), rs.getLong("coins")));
                }
            }
        }
        return list;
    }
}
