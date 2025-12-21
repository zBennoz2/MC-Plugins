package com.zbennoz.zbenlootr.database;

import com.zbennoz.zbenlootr.util.InventorySerializer;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class SQLiteDatabase implements Database {

    private final File file;
    private final Plugin plugin;
    private Connection connection;

    public SQLiteDatabase(File file, Plugin plugin) {
        this.file = file;
        this.plugin = plugin;
    }

    @Override
    public void init() throws SQLException {
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            throw new SQLException("Failed to create database folder");
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        createTables();
    }

    private void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS containers (" +
                    "container_id TEXT PRIMARY KEY," +
                    "world TEXT," +
                    "x INT," +
                    "y INT," +
                    "z INT," +
                    "type TEXT," +
                    "created_at TIMESTAMP)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_loot (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "container_id TEXT," +
                    "player_uuid TEXT," +
                    "inventory_base64 TEXT," +
                    "first_opened_at TIMESTAMP," +
                    "updated_at TIMESTAMP," +
                    "UNIQUE(container_id, player_uuid))");
        }
    }

    @Override
    public Optional<Inventory> loadPlayerInventory(String containerId, UUID playerId) throws SQLException {
        String sql = "SELECT inventory_base64 FROM player_loot WHERE container_id = ? AND player_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, containerId);
            statement.setString(2, playerId.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    String base64 = rs.getString("inventory_base64");
                    return Optional.ofNullable(InventorySerializer.fromBase64(base64));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void saveContainer(String containerId, Location location, String type) throws SQLException {
        String sql = "INSERT OR IGNORE INTO containers(container_id, world, x, y, z, type, created_at) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, containerId);
            statement.setString(2, location.getWorld().getName());
            statement.setInt(3, location.getBlockX());
            statement.setInt(4, location.getBlockY());
            statement.setInt(5, location.getBlockZ());
            statement.setString(6, type);
            statement.setTimestamp(7, Timestamp.from(Instant.now()));
            statement.executeUpdate();
        }
    }

    @Override
    public void savePlayerInventory(String containerId, UUID playerId, Inventory inventory) throws SQLException {
        String sql = "INSERT INTO player_loot(container_id, player_uuid, inventory_base64, first_opened_at, updated_at) " +
                "VALUES(?,?,?,?,?) ON CONFLICT(container_id, player_uuid) DO UPDATE SET inventory_base64 = excluded.inventory_base64, updated_at = excluded.updated_at";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, containerId);
            statement.setString(2, playerId.toString());
            statement.setString(3, InventorySerializer.toBase64(inventory));
            Timestamp now = Timestamp.from(Instant.now());
            statement.setTimestamp(4, now);
            statement.setTimestamp(5, now);
            statement.executeUpdate();
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to close SQLite connection: " + e.getMessage());
        }
    }
}
