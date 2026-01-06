package com.zbennoz.zbenadmintool.storage;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class Database {

    private final Plugin plugin;
    private String jdbcUrl;

    public Database(Plugin plugin) {
        this.plugin = plugin;
        init();
    }

    private void init() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dataFolder.mkdirs();
        }
        File dbFile = new File(dataFolder, "zbenadmintool.db");
        this.jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }

    public Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL;");
            st.execute("PRAGMA synchronous=NORMAL;");
            st.execute("PRAGMA foreign_keys=ON;");
        }
        return connection;
    }

    public void initSchema() {
        try (Connection connection = openConnection();
             Statement st = connection.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS ranks(name TEXT PRIMARY KEY, color TEXT, priority INT, prefix TEXT, suffix TEXT, backpack_slots INT DEFAULT 27, max_claim_chunks INT DEFAULT 10);");
            try {
                st.executeUpdate("ALTER TABLE ranks ADD COLUMN backpack_slots INT DEFAULT 27;");
            } catch (SQLException ignored) {
                // Column exists
            }
            try {
                st.executeUpdate("ALTER TABLE ranks ADD COLUMN max_claim_chunks INT DEFAULT 10;");
            } catch (SQLException ignored) {
                // Column exists
            }
            st.executeUpdate("CREATE TABLE IF NOT EXISTS player_ranks(uuid TEXT PRIMARY KEY, rank_name TEXT);");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS rank_permissions(rank_name TEXT, permission TEXT, PRIMARY KEY(rank_name, permission));");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS container_logs( id INTEGER PRIMARY KEY AUTOINCREMENT, uuid TEXT, name TEXT, world TEXT, x INT, y INT, z INT, container_type TEXT, action TEXT, material TEXT, amount INT, ts INTEGER);");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS block_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, player_uuid TEXT, player_name TEXT, world TEXT, x INTEGER, y INTEGER, z INTEGER, block_type TEXT, action TEXT, timestamp INTEGER);");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Konnte Datenbank-Schema nicht initialisieren", ex);
        }
    }

    public void close() {
        // Nothing to close in connection-per-query mode
    }
}
