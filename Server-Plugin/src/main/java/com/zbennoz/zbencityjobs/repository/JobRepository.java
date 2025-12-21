package com.zbennoz.zbencityjobs.repository;

import com.zbennoz.zbencityjobs.model.Job;
import com.zbennoz.zbencityjobs.model.JobStatus;
import com.zbennoz.zbencityjobs.model.JobType;
import com.zbennoz.zbencityjobs.storage.DatabaseManager;
import com.zbennoz.zbencityjobs.util.ItemSerializer;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JobRepository {
    private final DatabaseManager database;

    public JobRepository(DatabaseManager database) {
        this.database = database;
    }

    public void init() throws SQLException {
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS jobs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "type TEXT NOT NULL, " +
                    "requester_uuid TEXT NOT NULL, " +
                    "worker_uuid TEXT, " +
                    "reward REAL NOT NULL, " +
                    "escrow INTEGER NOT NULL, " +
                    "status TEXT NOT NULL, " +
                    "description TEXT, " +
                    "delivery_item TEXT, " +
                    "created_at INTEGER NOT NULL" +
                    ")");
        }
    }

    public List<Job> loadAll() throws SQLException {
        List<Job> jobs = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM jobs")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                jobs.add(mapRow(rs));
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new SQLException("Failed to read delivery item", e);
        }
        return jobs;
    }

    public int insert(Job job) throws SQLException {
        String sql = "INSERT INTO jobs(type, requester_uuid, worker_uuid, reward, escrow, status, description, delivery_item, created_at) VALUES(?,?,?,?,?,?,?,?,?)";
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, job.getType().name());
            ps.setString(2, job.getRequester().toString());
            ps.setString(3, job.getWorker() != null ? job.getWorker().toString() : null);
            ps.setDouble(4, job.getReward());
            ps.setInt(5, job.isEscrow() ? 1 : 0);
            ps.setString(6, job.getStatus().name());
            ps.setString(7, job.getDescription());
            ps.setString(8, serializeItem(job.getDeliveryItem()));
            ps.setLong(9, job.getCreatedAt());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                return keys.getInt(1);
            }
            return 0;
        } catch (IOException e) {
            throw new SQLException("Failed to serialize delivery item", e);
        }
    }

    public void update(Job job) throws SQLException {
        String sql = "UPDATE jobs SET worker_uuid=?, status=?, delivery_item=? WHERE id=?";
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, job.getWorker() != null ? job.getWorker().toString() : null);
            ps.setString(2, job.getStatus().name());
            ps.setString(3, serializeItem(job.getDeliveryItem()));
            ps.setInt(4, job.getId());
            ps.executeUpdate();
        } catch (IOException e) {
            throw new SQLException("Failed to serialize delivery item", e);
        }
    }

    private Job mapRow(ResultSet rs) throws SQLException, IOException, ClassNotFoundException {
        int id = rs.getInt("id");
        JobType type = JobType.valueOf(rs.getString("type"));
        UUID requester = UUID.fromString(rs.getString("requester_uuid"));
        String workerRaw = rs.getString("worker_uuid");
        UUID worker = workerRaw != null ? UUID.fromString(workerRaw) : null;
        double reward = rs.getDouble("reward");
        boolean escrow = rs.getInt("escrow") == 1;
        JobStatus status = JobStatus.valueOf(rs.getString("status"));
        String description = rs.getString("description");
        String delivery = rs.getString("delivery_item");
        ItemStack itemStack = delivery != null ? ItemSerializer.fromBase64(delivery) : null;
        long createdAt = rs.getLong("created_at");
        return new Job(id, type, requester, worker, reward, escrow, status, description, itemStack, createdAt);
    }

    private String serializeItem(ItemStack stack) throws IOException {
        if (stack == null) return null;
        return ItemSerializer.toBase64(stack);
    }
}
