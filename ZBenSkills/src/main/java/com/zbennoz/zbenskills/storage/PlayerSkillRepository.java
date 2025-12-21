package com.zbennoz.zbenskills.storage;

import com.zbennoz.zbenskills.ZBenSkillsPlugin;
import com.zbennoz.zbenskills.data.PlayerProfile;
import com.zbennoz.zbenskills.model.SkillType;
import java.io.File;
import java.sql.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class PlayerSkillRepository {
    private final ZBenSkillsPlugin plugin;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Connection connection;
    private final Map<UUID, PlayerProfile> cache = new java.util.concurrent.ConcurrentHashMap<>();

    public PlayerSkillRepository(ZBenSkillsPlugin plugin) {
        this.plugin = plugin;
        init();
    }

    private void init() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "skills.db");
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode=WAL;");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_skills(uuid TEXT, skill TEXT, level INTEGER, xp REAL, prestige INTEGER, skill_points INTEGER, PRIMARY KEY(uuid, skill))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_nodes(uuid TEXT, node_id TEXT, PRIMARY KEY(uuid, node_id))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_achievements(uuid TEXT, achievement_id TEXT, PRIMARY KEY(uuid, achievement_id))");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to init database", e);
        }
    }

    public PlayerProfile getProfile(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadProfile);
    }

    private PlayerProfile loadProfile(UUID uuid) {
        PlayerProfile profile = new PlayerProfile(uuid);
        boolean skillPointsLoaded = false;
        try (PreparedStatement stmt = connection.prepareStatement("SELECT skill, level, xp, prestige, skill_points FROM player_skills WHERE uuid=?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                SkillType skill = SkillType.valueOf(rs.getString("skill"));
                profile.getLevels().put(skill, rs.getInt("level"));
                profile.getXp().put(skill, rs.getDouble("xp"));
                profile.getPrestige().put(skill, rs.getInt("prestige"));
                if (!skillPointsLoaded) {
                    profile.addSkillPoints(rs.getInt("skill_points"));
                    skillPointsLoaded = true;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load skills for " + uuid, e);
        }
        try (PreparedStatement stmt = connection.prepareStatement("SELECT node_id FROM player_nodes WHERE uuid=?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                profile.getUnlockedNodes().add(rs.getString("node_id"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load nodes for " + uuid, e);
        }
        try (PreparedStatement stmt = connection.prepareStatement("SELECT achievement_id FROM player_achievements WHERE uuid=?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                profile.getAchievements().add(rs.getString("achievement_id"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load achievements for " + uuid, e);
        }
        return profile;
    }

    public void saveProfileAsync(PlayerProfile profile) {
        CompletableFuture.runAsync(() -> saveProfile(profile), executor);
    }

    public void saveProfile(PlayerProfile profile) {
        try {
            connection.setAutoCommit(false);
            for (SkillType type : SkillType.values()) {
                int level = profile.getLevels().getOrDefault(type, 1);
                double xp = profile.getXp().getOrDefault(type, 0.0);
                int prestige = profile.getPrestige().getOrDefault(type, 0);
                int points = profile.getSkillPoints();
                try (PreparedStatement stmt = connection.prepareStatement("REPLACE INTO player_skills(uuid, skill, level, xp, prestige, skill_points) VALUES(?,?,?,?,?,?)")) {
                    stmt.setString(1, profile.getUuid().toString());
                    stmt.setString(2, type.name());
                    stmt.setInt(3, level);
                    stmt.setDouble(4, xp);
                    stmt.setInt(5, prestige);
                    stmt.setInt(6, points);
                    stmt.executeUpdate();
                }
            }
            try (PreparedStatement del = connection.prepareStatement("DELETE FROM player_nodes WHERE uuid=?")) {
                del.setString(1, profile.getUuid().toString());
                del.executeUpdate();
            }
            try (PreparedStatement ins = connection.prepareStatement("INSERT OR IGNORE INTO player_nodes(uuid, node_id) VALUES(?,?)")) {
                for (String node : profile.getUnlockedNodes()) {
                    ins.setString(1, profile.getUuid().toString());
                    ins.setString(2, node);
                    ins.addBatch();
                }
                ins.executeBatch();
            }
            try (PreparedStatement del = connection.prepareStatement("DELETE FROM player_achievements WHERE uuid=?")) {
                del.setString(1, profile.getUuid().toString());
                del.executeUpdate();
            }
            try (PreparedStatement ins = connection.prepareStatement("INSERT OR IGNORE INTO player_achievements(uuid, achievement_id) VALUES(?,?)")) {
                for (String achievement : profile.getAchievements()) {
                    ins.setString(1, profile.getUuid().toString());
                    ins.setString(2, achievement);
                    ins.addBatch();
                }
                ins.executeBatch();
            }
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Rollback failed", ex);
            }
            plugin.getLogger().log(Level.SEVERE, "Failed to save profile" + profile.getUuid(), e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    public void flush() {
        cache.values().forEach(this::saveProfile);
    }

    public void close() {
        flush();
        executor.shutdownNow();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed closing connection", e);
        }
    }
}
