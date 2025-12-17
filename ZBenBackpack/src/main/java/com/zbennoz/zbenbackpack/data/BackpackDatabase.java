package com.zbennoz.zbenbackpack.data;

import org.bukkit.Bukkit;

import java.io.File;
import java.sql.*;
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
                st.executeUpdate("CREATE TABLE IF NOT EXISTS backpacks (player TEXT PRIMARY KEY, data TEXT);");
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

    public void saveBackpackAsync(String uuid, String data) {
        async.execute(() -> {
            try (PreparedStatement ps = connection.prepareStatement("INSERT OR REPLACE INTO backpacks(player,data) VALUES(?,?)")) {
                ps.setString(1, uuid);
                ps.setString(2, data);
                ps.executeUpdate();
            } catch (SQLException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "Failed to save backpack", ex);
            }
        });
    }

    public String loadBackpack(String uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT data FROM backpacks WHERE player=?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to load backpack", ex);
        }
        return null;
    }
}
