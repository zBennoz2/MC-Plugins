package com.zbennoz.zbencoins.database;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import org.sqlite.SQLiteConfig;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

/**
 * Verwaltet die SQLite Verbindung und Migrationen.
 */
public class Database {

    private final ZBenCoinsPlugin plugin;
    private Connection connection;

    public Database(ZBenCoinsPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() throws SQLException {
        File dbFile = new File(plugin.getDataFolder(), "data.db");
        SQLiteConfig config = new SQLiteConfig();
        config.setBusyTimeout("5000");
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        connection = DriverManager.getConnection(url, config.toProperties());
        runMigrations();
    }

    public Connection getConnection() {
        return connection;
    }

    private void runMigrations() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS players (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "name TEXT, " +
                    "coins INTEGER NOT NULL DEFAULT 0, " +
                    "created_at INTEGER, " +
                    "updated_at INTEGER)"
            );
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS transactions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid TEXT NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "amount INTEGER NOT NULL, " +
                    "note TEXT, " +
                    "created_at INTEGER)"
            );
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_transactions_uuid ON transactions(uuid)");
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Konnte Datenbankverbindung nicht schließen", e);
            }
        }
    }
}
