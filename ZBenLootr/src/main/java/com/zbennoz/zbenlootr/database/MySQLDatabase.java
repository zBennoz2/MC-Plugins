package com.zbennoz.zbenlootr.database;

import com.zbennoz.zbenlootr.util.InventorySerializer;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.sql.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class MySQLDatabase implements Database {

    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;
    private final Plugin plugin;
    private Connection connection;

    public MySQLDatabase(String host, int port, String database, String user, String password, Plugin plugin) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
        this.plugin = plugin;
    }

    @Override
    public void init() throws SQLException {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";
        connection = DriverManager.getConnection(url, user, password);
        createTables();
    }

    private void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS containers (" +
                    "container_id VARCHAR(191) PRIMARY KEY," +
                    "world VARCHAR(64)," +
                    "x INT," +
                    "y INT," +
                    "z INT," +
                    "type VARCHAR(32)," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_loot (" +
                    "id INT NOT NULL AUTO_INCREMENT," +
                    "container_id VARCHAR(191)," +
                    "player_uuid VARCHAR(36)," +
                    "inventory_base64 LONGTEXT," +
                    "first_opened_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "PRIMARY KEY(id)," +
                    "UNIQUE KEY unique_player_loot (container_id, player_uuid))");
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
        String sql = "INSERT IGNORE INTO containers(container_id, world, x, y, z, type, created_at) VALUES(?,?,?,?,?,?,?)";
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
                "VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE inventory_base64 = VALUES(inventory_base64), updated_at = VALUES(updated_at)";
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
            plugin.getLogger().warning("Failed to close MySQL connection: " + e.getMessage());
        }
    }
}
