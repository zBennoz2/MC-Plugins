package com.zbennoz.zbencityjobs.repository;

import com.zbennoz.zbencityjobs.storage.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuditLogRepository {
    private final DatabaseManager database;

    public AuditLogRepository(DatabaseManager database) {
        this.database = database;
    }

    public void init() throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement("CREATE TABLE IF NOT EXISTS audit_log (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                     "actor_uuid TEXT, " +
                     "action TEXT NOT NULL, " +
                     "context TEXT, " +
                     "created_at INTEGER NOT NULL" +
                     ")")) {
            ps.executeUpdate();
        }
    }

    public void insert(String actorUuid, String action, String context, long createdAt) throws SQLException {
        String sql = "INSERT INTO audit_log(actor_uuid, action, context, created_at) VALUES(?,?,?,?)";
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, actorUuid);
            ps.setString(2, action);
            ps.setString(3, context);
            ps.setLong(4, createdAt);
            ps.executeUpdate();
        }
    }
}
