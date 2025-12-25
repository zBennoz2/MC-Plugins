package com.zbennoz.zbencoins.job;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Logt alle Jobaktionen für Transparenz.
 */
public class JobLogDao {

    private final Connection connection;

    public JobLogDao(Connection connection) {
        this.connection = connection;
    }

    public synchronized void log(int jobId, String action, UUID actor, String actorName, String note) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO job_logs(job_id, action, actor_uuid, actor_name, note, created_at) VALUES (?, ?, ?, ?, ?, ?)")) {
            stmt.setInt(1, jobId);
            stmt.setString(2, action);
            stmt.setString(3, actor == null ? null : actor.toString());
            stmt.setString(4, actorName);
            stmt.setString(5, note);
            stmt.setLong(6, Instant.now().getEpochSecond());
            stmt.executeUpdate();
        }
    }

    public synchronized List<String> findRecentForPlayer(UUID player, int limit) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM job_logs WHERE actor_uuid = ? ORDER BY created_at DESC LIMIT ?")) {
            stmt.setString(1, player.toString());
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                List<String> notes = new ArrayList<>();
                while (rs.next()) {
                    notes.add(rs.getString("note"));
                }
                return notes;
            }
        }
    }
}
