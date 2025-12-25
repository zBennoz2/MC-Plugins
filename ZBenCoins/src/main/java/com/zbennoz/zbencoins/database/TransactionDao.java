package com.zbennoz.zbencoins.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Datenzugriff für Transaktionen.
 */
public class TransactionDao {

    private final Connection connection;

    public TransactionDao(Connection connection) {
        this.connection = connection;
    }

    public synchronized void insert(UUID uuid, String type, long amount, String note) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO transactions(uuid, type, amount, note, created_at) VALUES (?, ?, ?, ?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, type);
            stmt.setLong(3, amount);
            stmt.setString(4, note);
            stmt.setLong(5, Instant.now().getEpochSecond());
            stmt.executeUpdate();
        }
    }

    public synchronized long countLastDays(UUID uuid, int days) throws SQLException {
        long since = Instant.now().minus(days, ChronoUnit.DAYS).getEpochSecond();
        try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) as cnt FROM transactions WHERE uuid = ? AND created_at >= ?")) {
            stmt.setString(1, uuid.toString());
            stmt.setLong(2, since);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("cnt");
                }
            }
        }
        return 0;
    }
}
