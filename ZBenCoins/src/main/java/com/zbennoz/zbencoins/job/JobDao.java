package com.zbennoz.zbencoins.job;

import org.bukkit.Material;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Datenzugriff für Jobs.
 */
public class JobDao {

    private final Connection connection;

    public JobDao(Connection connection) {
        this.connection = connection;
    }

    public synchronized JobRecord insert(JobType type, String title, String description, long reward, UUID creator,
                                         String creatorName, Instant expiresAt, Material itemType, int itemAmount)
            throws SQLException {
        long now = Instant.now().getEpochSecond();
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO jobs(type, title, description, reward, creator_uuid, creator_name, status, expires_at, " +
                        "created_at, updated_at, item_type, item_amount, completion_requested) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, type.name());
            stmt.setString(2, title);
            stmt.setString(3, description);
            stmt.setLong(4, reward);
            stmt.setString(5, creator.toString());
            stmt.setString(6, creatorName);
            stmt.setString(7, JobStatus.OFFEN.name());
            if (expiresAt == null) {
                stmt.setNull(8, Types.INTEGER);
            } else {
                stmt.setLong(8, expiresAt.getEpochSecond());
            }
            stmt.setLong(9, now);
            stmt.setLong(10, now);
            stmt.setString(11, itemType == null ? null : itemType.name());
            stmt.setInt(12, itemAmount);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return new JobRecord(keys.getInt(1), type, title, description, reward, creator, creatorName, null,
                            null, JobStatus.OFFEN, expiresAt, Instant.ofEpochSecond(now), Instant.ofEpochSecond(now),
                            itemType, itemAmount, false);
                }
            }
        }
        throw new SQLException("Kein Schlüssel beim Speichern des Jobs erhalten");
    }

    public synchronized Optional<JobRecord> findById(int id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM jobs WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(fromResult(rs));
                }
            }
        }
        return Optional.empty();
    }

    public synchronized List<JobRecord> findOpen(int limit, int offset) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM jobs WHERE status = ? AND (expires_at IS NULL OR expires_at > ?) " +
                        "ORDER BY created_at DESC LIMIT ? OFFSET ?")) {
            stmt.setString(1, JobStatus.OFFEN.name());
            stmt.setLong(2, Instant.now().getEpochSecond());
            stmt.setInt(3, limit);
            stmt.setInt(4, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                List<JobRecord> jobs = new ArrayList<>();
                while (rs.next()) {
                    jobs.add(fromResult(rs));
                }
                return jobs;
            }
        }
    }

    public synchronized List<JobRecord> findForCreator(UUID creator, JobStatus... statuses) throws SQLException {
        return findByField("creator_uuid", creator, statuses);
    }

    public synchronized List<JobRecord> findForAssignee(UUID assignee, JobStatus... statuses) throws SQLException {
        return findByField("assignee_uuid", assignee, statuses);
    }

    private List<JobRecord> findByField(String field, UUID uuid, JobStatus... statuses) throws SQLException {
        StringBuilder query = new StringBuilder("SELECT * FROM jobs WHERE " + field + " = ?");
        if (statuses.length > 0) {
            query.append(" AND status IN (");
            query.append("?,".repeat(statuses.length));
            query.setLength(query.length() - 1);
            query.append(")");
        }
        query.append(" ORDER BY created_at DESC");
        try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
            stmt.setString(1, uuid.toString());
            int index = 2;
            for (JobStatus status : statuses) {
                stmt.setString(index++, status.name());
            }
            try (ResultSet rs = stmt.executeQuery()) {
                List<JobRecord> jobs = new ArrayList<>();
                while (rs.next()) {
                    jobs.add(fromResult(rs));
                }
                return jobs;
            }
        }
    }

    public synchronized boolean acceptJob(int id, UUID assignee, String assigneeName) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE jobs SET status = ?, assignee_uuid = ?, assignee_name = ?, updated_at = ? " +
                        "WHERE id = ? AND status = ? AND (expires_at IS NULL OR expires_at > ? )")) {
            stmt.setString(1, JobStatus.ANGENOMMEN.name());
            stmt.setString(2, assignee.toString());
            stmt.setString(3, assigneeName);
            stmt.setLong(4, Instant.now().getEpochSecond());
            stmt.setInt(5, id);
            stmt.setString(6, JobStatus.OFFEN.name());
            stmt.setLong(7, Instant.now().getEpochSecond());
            return stmt.executeUpdate() == 1;
        }
    }

    public synchronized boolean updateStatus(int id, JobStatus expected, JobStatus next) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE jobs SET status = ?, updated_at = ? WHERE id = ? AND status = ?")) {
            stmt.setString(1, next.name());
            stmt.setLong(2, Instant.now().getEpochSecond());
            stmt.setInt(3, id);
            stmt.setString(4, expected.name());
            return stmt.executeUpdate() == 1;
        }
    }

    public synchronized boolean updateStatusWithRequest(int id, JobStatus expected, JobStatus next, boolean requested)
            throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE jobs SET status = ?, updated_at = ?, completion_requested = ? WHERE id = ? AND status = ?")) {
            stmt.setString(1, next.name());
            stmt.setLong(2, Instant.now().getEpochSecond());
            stmt.setInt(3, requested ? 1 : 0);
            stmt.setInt(4, id);
            stmt.setString(5, expected.name());
            return stmt.executeUpdate() == 1;
        }
    }

    public synchronized boolean requestCompletion(int id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE jobs SET completion_requested = 1, updated_at = ? WHERE id = ? AND status = ?")) {
            stmt.setLong(1, Instant.now().getEpochSecond());
            stmt.setInt(2, id);
            stmt.setString(3, JobStatus.ANGENOMMEN.name());
            return stmt.executeUpdate() == 1;
        }
    }

    public synchronized List<JobRecord> findExpiredActive() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM jobs WHERE expires_at IS NOT NULL AND expires_at <= ? AND status IN (?, ?)")) {
            stmt.setLong(1, Instant.now().getEpochSecond());
            stmt.setString(2, JobStatus.OFFEN.name());
            stmt.setString(3, JobStatus.ANGENOMMEN.name());
            try (ResultSet rs = stmt.executeQuery()) {
                List<JobRecord> jobs = new ArrayList<>();
                while (rs.next()) {
                    jobs.add(fromResult(rs));
                }
                return jobs;
            }
        }
    }

    public synchronized int countActiveForCreator(UUID creator) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) as cnt FROM jobs WHERE creator_uuid = ? AND status IN (?, ?)")) {
            stmt.setString(1, creator.toString());
            stmt.setString(2, JobStatus.OFFEN.name());
            stmt.setString(3, JobStatus.ANGENOMMEN.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt");
                }
            }
        }
        return 0;
    }

    public synchronized int countActiveForAssignee(UUID assignee) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) as cnt FROM jobs WHERE assignee_uuid = ? AND status = ?")) {
            stmt.setString(1, assignee.toString());
            stmt.setString(2, JobStatus.ANGENOMMEN.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt");
                }
            }
        }
        return 0;
    }

    private JobRecord fromResult(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        JobType type = JobType.valueOf(rs.getString("type"));
        String title = rs.getString("title");
        String description = rs.getString("description");
        long reward = rs.getLong("reward");
        UUID creator = UUID.fromString(rs.getString("creator_uuid"));
        String creatorName = rs.getString("creator_name");
        String assigneeStr = rs.getString("assignee_uuid");
        UUID assignee = assigneeStr == null ? null : UUID.fromString(assigneeStr);
        String assigneeName = rs.getString("assignee_name");
        JobStatus status = JobStatus.valueOf(rs.getString("status"));
        Long expiresAtLong = (Long) rs.getObject("expires_at");
        Instant expiresAt = expiresAtLong == null ? null : Instant.ofEpochSecond(expiresAtLong);
        Instant createdAt = Instant.ofEpochSecond(rs.getLong("created_at"));
        Instant updatedAt = Instant.ofEpochSecond(rs.getLong("updated_at"));
        String itemType = rs.getString("item_type");
        int itemAmount = rs.getInt("item_amount");
        boolean completionRequested = rs.getInt("completion_requested") == 1;
        return new JobRecord(id, type, title, description, reward, creator, creatorName, assignee, assigneeName, status,
                expiresAt, createdAt, updatedAt, itemType == null ? null : Material.valueOf(itemType), itemAmount,
                completionRequested);
    }
}
