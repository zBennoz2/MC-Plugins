package com.zbennoz.zbenbackpack.data;

import org.bukkit.Bukkit;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class BackpackDatabase {
    private final File file;
    private Connection connection;
    private ExecutorService async;

    public BackpackDatabase(File file) {
        this.file = file;
    }

    public void init() {
        try {
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            try (Statement st = connection.createStatement()) {
                st.executeUpdate("PRAGMA journal_mode=WAL;");
                st.executeUpdate("PRAGMA synchronous=NORMAL;");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS backpacks (player TEXT PRIMARY KEY, data TEXT, size INT DEFAULT 9);");
                ensureSizeColumn(st);
            }
        } catch (SQLException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to init backpack db", ex);
        }
        async = Executors.newSingleThreadExecutor(r -> new Thread(r, "ZBenBackpack-DB"));
    }

    public void shutdown() {
        if (async != null) async.shutdownNow();
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }

    public void saveBackpackAsync(String uuid, String data, int size) {
        async.execute(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR REPLACE INTO backpacks(player,data,size) VALUES(?,?,?)")) {
                ps.setString(1, uuid);
                ps.setString(2, data);
                ps.setInt(3, size);
                ps.executeUpdate();
            } catch (SQLException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "Failed to save backpack", ex);
            }
        });
    }

    public BackpackRecord loadBackpack(String uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT data, size FROM backpacks WHERE player=?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new BackpackRecord(rs.getString("data"), rs.getInt("size"));
                }
            }
        } catch (SQLException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to load backpack", ex);
        }
        return null;
    }

    private void ensureSizeColumn(Statement st) throws SQLException {
        try (ResultSet rs = st.executeQuery("PRAGMA table_info(backpacks);")) {
            boolean hasSize = false;
            while (rs.next()) {
                if ("size".equalsIgnoreCase(rs.getString("name"))) {
                    hasSize = true;
                    break;
                }
            }
            if (!hasSize) {
                st.executeUpdate("ALTER TABLE backpacks ADD COLUMN size INT DEFAULT 9;");
            }
        }
    }

    public record BackpackRecord(String data, int size) {
    }
}
