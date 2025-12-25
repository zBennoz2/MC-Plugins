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
        config.setBusyTimeout(5000);
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

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS market_offers (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "seller_uuid TEXT NOT NULL, " +
                    "seller_name TEXT NOT NULL, " +
                    "buyer_uuid TEXT, " +
                    "item_data TEXT NOT NULL, " +
                    "amount INTEGER NOT NULL, " +
                    "price INTEGER NOT NULL, " +
                    "status TEXT NOT NULL, " +
                    "expires_at INTEGER NOT NULL, " +
                    "created_at INTEGER NOT NULL, " +
                    "delivered INTEGER NOT NULL DEFAULT 0)"
            );
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_market_offers_status ON market_offers(status)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_market_offers_seller ON market_offers(seller_uuid)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS market_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "offer_id INTEGER NOT NULL, " +
                    "action TEXT NOT NULL, " +
                    "actor_uuid TEXT, " +
                    "actor_name TEXT, " +
                    "note TEXT, " +
                    "created_at INTEGER NOT NULL)"
            );
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_market_logs_offer ON market_logs(offer_id)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS jobs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "type TEXT NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "description TEXT NOT NULL, " +
                    "reward INTEGER NOT NULL, " +
                    "creator_uuid TEXT NOT NULL, " +
                    "creator_name TEXT NOT NULL, " +
                    "assignee_uuid TEXT, " +
                    "assignee_name TEXT, " +
                    "status TEXT NOT NULL, " +
                    "expires_at INTEGER, " +
                    "created_at INTEGER NOT NULL, " +
                    "updated_at INTEGER NOT NULL, " +
                    "item_type TEXT, " +
                    "item_amount INTEGER NOT NULL DEFAULT 0, " +
                    "completion_requested INTEGER NOT NULL DEFAULT 0" +
                    ")");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_jobs_status ON jobs(status)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_jobs_creator ON jobs(creator_uuid)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS job_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "job_id INTEGER NOT NULL, " +
                    "action TEXT NOT NULL, " +
                    "actor_uuid TEXT, " +
                    "actor_name TEXT, " +
                    "note TEXT, " +
                    "created_at INTEGER NOT NULL)"
            );
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_job_logs_job ON job_logs(job_id)");
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
