package com.zbennoz.zbenclaims.db;

import com.zbennoz.zbenclaims.Claim;
import com.zbennoz.zbenclaims.ZBenClaimsPlugin;

import java.io.File;
import java.sql.*;
import java.util.*;

public class SQLiteDatabase implements Database {

    private final ZBenClaimsPlugin plugin;
    private Connection conn;

    public SQLiteDatabase(ZBenClaimsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        try {
            File dbFile = new File(plugin.getDataFolder(), plugin.getConfig().getString("database.file", "claims.db"));
            plugin.getDataFolder().mkdirs();

            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            conn = DriverManager.getConnection(url);
            conn.setAutoCommit(true);

            try (Statement st = conn.createStatement()) {
                st.executeUpdate("PRAGMA foreign_keys = ON;");

                st.executeUpdate("CREATE TABLE IF NOT EXISTS claims(" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "owner_uuid TEXT NOT NULL," +
                        "world TEXT NOT NULL," +
                        "chunk_x INTEGER NOT NULL," +
                        "chunk_z INTEGER NOT NULL," +
                        "UNIQUE(world, chunk_x, chunk_z)" +
                        ");");

                st.executeUpdate("CREATE TABLE IF NOT EXISTS trusted(" +
                        "claim_id INTEGER NOT NULL," +
                        "uuid TEXT NOT NULL," +
                        "UNIQUE(claim_id, uuid)," +
                        "FOREIGN KEY(claim_id) REFERENCES claims(id) ON DELETE CASCADE" +
                        ");");

                st.executeUpdate("CREATE TABLE IF NOT EXISTS flags(" +
                        "claim_id INTEGER NOT NULL," +
                        "flag TEXT NOT NULL," +
                        "value INTEGER NOT NULL," +
                        "UNIQUE(claim_id, flag)," +
                        "FOREIGN KEY(claim_id) REFERENCES claims(id) ON DELETE CASCADE" +
                        ");");

                st.executeUpdate("CREATE TABLE IF NOT EXISTS player_ranks(" +
                        "uuid TEXT PRIMARY KEY," +
                        "rank TEXT NOT NULL" +
                        ");");
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize SQLite: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        if (conn == null) return;
        try { conn.close(); } catch (SQLException ignored) {}
    }

    @Override
    public long createClaim(UUID owner, String world, int chunkX, int chunkZ) {
        String sql = "INSERT INTO claims(owner_uuid, world, chunk_x, chunk_z) VALUES(?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, owner.toString());
            ps.setString(2, world);
            ps.setInt(3, chunkX);
            ps.setInt(4, chunkZ);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("createClaim failed: " + e.getMessage());
        }
        return -1;
    }

    @Override
    public void deleteClaim(long claimId) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM claims WHERE id=?")) {
            ps.setLong(1, claimId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("deleteClaim failed: " + e.getMessage());
        }
    }

    @Override
    public Claim getClaim(String world, int chunkX, int chunkZ) {
        String sql = "SELECT id, owner_uuid, world, chunk_x, chunk_z FROM claims WHERE world=? AND chunk_x=? AND chunk_z=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapClaim(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("getClaim failed: " + e.getMessage());
        }
        return null;
    }

    @Override
    public Claim getClaimById(long claimId) {
        String sql = "SELECT id, owner_uuid, world, chunk_x, chunk_z FROM claims WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, claimId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapClaim(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("getClaimById failed: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Claim> getClaimsByOwner(UUID owner) {
        String sql = "SELECT id, owner_uuid, world, chunk_x, chunk_z FROM claims WHERE owner_uuid=? ORDER BY world, chunk_x, chunk_z";
        List<Claim> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapClaim(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("getClaimsByOwner failed: " + e.getMessage());
        }
        return out;
    }

    @Override
    public List<Claim> getAllClaims() {
        String sql = "SELECT id, owner_uuid, world, chunk_x, chunk_z FROM claims";
        List<Claim> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(mapClaim(rs));
        } catch (SQLException e) {
            plugin.getLogger().warning("getAllClaims failed: " + e.getMessage());
        }
        return out;
    }

    @Override
    public int countClaimsByOwner(UUID owner) {
        String sql = "SELECT COUNT(*) FROM claims WHERE owner_uuid=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("countClaimsByOwner failed: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public List<UUID> getTrusted(long claimId) {
        String sql = "SELECT uuid FROM trusted WHERE claim_id=?";
        List<UUID> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, claimId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(UUID.fromString(rs.getString(1)));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("getTrusted failed: " + e.getMessage());
        }
        return out;
    }

    @Override
    public void addTrusted(long claimId, UUID uuid) {
        String sql = "INSERT OR IGNORE INTO trusted(claim_id, uuid) VALUES(?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, claimId);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("addTrusted failed: " + e.getMessage());
        }
    }

    @Override
    public void removeTrusted(long claimId, UUID uuid) {
        String sql = "DELETE FROM trusted WHERE claim_id=? AND uuid=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, claimId);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("removeTrusted failed: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Boolean> getFlags(long claimId) {
        String sql = "SELECT flag, value FROM flags WHERE claim_id=?";
        Map<String, Boolean> out = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, claimId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getString(1), rs.getInt(2) != 0);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("getFlags failed: " + e.getMessage());
        }
        return out;
    }

    @Override
    public void setFlag(long claimId, String flag, boolean value) {
        String sql = "INSERT INTO flags(claim_id, flag, value) VALUES(?,?,?) " +
                "ON CONFLICT(claim_id, flag) DO UPDATE SET value=excluded.value";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, claimId);
            ps.setString(2, flag);
            ps.setInt(3, value ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("setFlag failed: " + e.getMessage());
        }
    }

    @Override
    public String getPlayerRank(UUID player) {
        String sql = "SELECT rank FROM player_ranks WHERE uuid=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("getPlayerRank failed: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void setPlayerRank(UUID player, String rank) {
        if (rank == null || rank.isBlank()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM player_ranks WHERE uuid=?")) {
                ps.setString(1, player.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("clearPlayerRank failed: " + e.getMessage());
            }
            return;
        }
        String sql = "INSERT INTO player_ranks(uuid, rank) VALUES(?,?) " +
                "ON CONFLICT(uuid) DO UPDATE SET rank=excluded.rank";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player.toString());
            ps.setString(2, rank);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("setPlayerRank failed: " + e.getMessage());
        }
    }

    private Claim mapClaim(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        UUID owner = UUID.fromString(rs.getString("owner_uuid"));
        String world = rs.getString("world");
        int x = rs.getInt("chunk_x");
        int z = rs.getInt("chunk_z");
        return new Claim(id, owner, world, x, z);
    }
}
