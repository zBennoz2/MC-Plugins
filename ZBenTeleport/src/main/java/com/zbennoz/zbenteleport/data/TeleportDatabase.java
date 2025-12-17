package com.zbennoz.zbenteleport.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class TeleportDatabase {

    private final File file;
    private Connection connection;
    private ExecutorService async;

    public TeleportDatabase(File file) {
        this.file = file;
    }

    public void init() {
        try {
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                throw new IllegalStateException("Unable to create plugin data folder");
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("PRAGMA journal_mode=WAL;");
                statement.executeUpdate("PRAGMA synchronous=NORMAL;");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS homes (player TEXT, name TEXT, world TEXT, x REAL, y REAL, z REAL, yaw REAL, pitch REAL, PRIMARY KEY(player, name));");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS last_locations (player TEXT PRIMARY KEY, world TEXT, x REAL, y REAL, z REAL, yaw REAL, pitch REAL, type TEXT);");
            }
        } catch (SQLException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to initialize database", ex);
        }
        async = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "ZBenTeleport-DB");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void shutdown() {
        if (async != null) {
            async.shutdownNow();
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }

    public void saveHomeAsync(UUID playerId, String name, Location location) {
        async.execute(() -> saveHome(playerId, name, location));
    }

    private void saveHome(UUID playerId, String name, Location location) {
        String sql = "INSERT OR REPLACE INTO homes(player, name, world, x, y, z, yaw, pitch) VALUES(?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, name.toLowerCase());
            ps.setString(3, location.getWorld().getName());
            ps.setDouble(4, location.getX());
            ps.setDouble(5, location.getY());
            ps.setDouble(6, location.getZ());
            ps.setFloat(7, location.getYaw());
            ps.setFloat(8, location.getPitch());
            ps.executeUpdate();
        } catch (SQLException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save home", ex);
        }
    }

    public void deleteHomeAsync(UUID playerId, String name) {
        async.execute(() -> deleteHome(playerId, name));
    }

    private void deleteHome(UUID playerId, String name) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM homes WHERE player=? AND name=?")) {
            ps.setString(1, playerId.toString());
            ps.setString(2, name.toLowerCase());
            ps.executeUpdate();
        } catch (SQLException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to delete home", ex);
        }
    }

    public List<HomeRecord> loadHomes(UUID playerId) {
        List<HomeRecord> homes = new ArrayList<>();
        String sql = "SELECT name, world, x, y, z, yaw, pitch FROM homes WHERE player=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    homes.add(new HomeRecord(
                            rs.getString("name"),
                            rs.getString("world"),
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getDouble("z"),
                            rs.getFloat("yaw"),
                            rs.getFloat("pitch")));
                }
            }
        } catch (SQLException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to load homes", ex);
        }
        return homes;
    }

    public void saveLastLocationAsync(UUID playerId, Location location, String type) {
        async.execute(() -> saveLastLocation(playerId, location, type));
    }

    private void saveLastLocation(UUID playerId, Location location, String type) {
        String sql = "INSERT OR REPLACE INTO last_locations(player, world, x, y, z, yaw, pitch, type) VALUES(?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, location.getWorld().getName());
            ps.setDouble(3, location.getX());
            ps.setDouble(4, location.getY());
            ps.setDouble(5, location.getZ());
            ps.setFloat(6, location.getYaw());
            ps.setFloat(7, location.getPitch());
            ps.setString(8, type);
            ps.executeUpdate();
        } catch (SQLException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save last location", ex);
        }
    }

    public Location loadLastLocation(UUID playerId, String preferredType) {
        String sql = "SELECT world, x, y, z, yaw, pitch FROM last_locations WHERE player=? AND type=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, preferredType);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return toLocation(rs);
                }
            }
        } catch (SQLException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to load last location", ex);
        }
        return null;
    }

    public Location loadAnyLastLocation(UUID playerId) {
        String sql = "SELECT world, x, y, z, yaw, pitch FROM last_locations WHERE player=? ORDER BY type='death' DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return toLocation(rs);
                }
            }
        } catch (SQLException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to load last location", ex);
        }
        return null;
    }

    private Location toLocation(ResultSet rs) throws SQLException {
        String worldName = rs.getString("world");
        var world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world,
                rs.getDouble("x"),
                rs.getDouble("y"),
                rs.getDouble("z"),
                rs.getFloat("yaw"),
                rs.getFloat("pitch"));
    }

    public record HomeRecord(String name, String world, double x, double y, double z, float yaw, float pitch) {
    }
}
