package com.zbennoz.zbenadmintool.rank;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import com.zbennoz.zbenadmintool.storage.Database;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class RankManager {

    private final ZBenAdmintool plugin;
    private final Database database;
    private final Map<String, Rank> ranks = new HashMap<>();
    private final Map<UUID, String> playerRanks = new HashMap<>();

    public RankManager(ZBenAdmintool plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    public void init() {
        try (Connection connection = database.getConnection()) {
            connection.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS ranks (name TEXT PRIMARY KEY, color TEXT, priority INTEGER, prefix TEXT, suffix TEXT);");
            connection.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS player_ranks (player_uuid TEXT PRIMARY KEY, rank_name TEXT);");
            connection.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS rank_permissions (rank_name TEXT, permission TEXT);");
        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler beim Erstellen der Tabellen: " + e.getMessage());
        }
        loadRanks();
        loadPlayerRanks();
        ensureDefaults();
    }

    private void ensureDefaults() {
        if (ranks.isEmpty()) {
            createRank("Owner", ChatColor.DARK_RED, 100, ChatColor.DARK_RED + "[Owner] ", "");
            createRank("Admin", ChatColor.RED, 80, ChatColor.RED + "[Admin] ", "");
            createRank("Moderator", ChatColor.GOLD, 60, ChatColor.GOLD + "[Mod] ", "");
            createRank("Supporter", ChatColor.GREEN, 40, ChatColor.GREEN + "[Sup] ", "");
            createRank("Spieler", ChatColor.WHITE, 0, "", "");
        }
    }

    private void loadRanks() {
        ranks.clear();
        try (Connection connection = database.getConnection();
             PreparedStatement st = connection.prepareStatement("SELECT * FROM ranks")) {
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                String name = rs.getString("name");
                ChatColor color = ChatColor.valueOf(rs.getString("color"));
                int priority = rs.getInt("priority");
                String prefix = rs.getString("prefix");
                String suffix = rs.getString("suffix");
                Rank rank = new Rank(name, color, priority, prefix, suffix);
                loadPermissions(connection, rank);
                ranks.put(name.toLowerCase(Locale.ROOT), rank);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Konnte Ränge nicht laden: " + e.getMessage());
        }
    }

    private void loadPermissions(Connection connection, Rank rank) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT permission FROM rank_permissions WHERE rank_name = ?")) {
            ps.setString(1, rank.getName());
            ResultSet perm = ps.executeQuery();
            while (perm.next()) {
                rank.getPermissions().add(perm.getString("permission"));
            }
        }
    }

    private void loadPlayerRanks() {
        playerRanks.clear();
        try (Connection connection = database.getConnection();
             PreparedStatement st = connection.prepareStatement("SELECT * FROM player_ranks")) {
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                playerRanks.put(UUID.fromString(rs.getString("player_uuid")), rs.getString("rank_name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Konnte Spieler-Ränge nicht laden: " + e.getMessage());
        }
    }

    public boolean createRank(String name, ChatColor color, int priority, String prefix, String suffix) {
        try (Connection connection = database.getConnection();
             PreparedStatement st = connection.prepareStatement("INSERT OR REPLACE INTO ranks(name, color, priority, prefix, suffix) VALUES(?,?,?,?,?)")) {
            st.setString(1, name);
            st.setString(2, color.name());
            st.setInt(3, priority);
            st.setString(4, prefix);
            st.setString(5, suffix);
            st.executeUpdate();
            Rank rank = new Rank(name, color, priority, prefix, suffix);
            ranks.put(name.toLowerCase(Locale.ROOT), rank);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("Konnte Rang nicht erstellen: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteRank(String name) {
        try (Connection connection = database.getConnection();
             PreparedStatement st = connection.prepareStatement("DELETE FROM ranks WHERE name = ?")) {
            st.setString(1, name);
            st.executeUpdate();
            ranks.remove(name.toLowerCase(Locale.ROOT));
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("Konnte Rang nicht löschen: " + e.getMessage());
            return false;
        }
    }

    public Rank getRank(String name) {
        return ranks.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<Rank> getRanks() {
        return ranks.values();
    }

    public void setPlayerRank(UUID uuid, String rankName) {
        try (Connection connection = database.getConnection();
             PreparedStatement st = connection.prepareStatement("INSERT OR REPLACE INTO player_ranks(player_uuid, rank_name) VALUES(?,?)")) {
            st.setString(1, uuid.toString());
            st.setString(2, rankName);
            st.executeUpdate();
            playerRanks.put(uuid, rankName);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                refreshPlayerTeam(player);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Konnte Spieler-Rang nicht speichern: " + e.getMessage());
        }
    }

    public void removePlayerRank(UUID uuid) {
        try (Connection connection = database.getConnection();
             PreparedStatement st = connection.prepareStatement("DELETE FROM player_ranks WHERE player_uuid = ?")) {
            st.setString(1, uuid.toString());
            st.executeUpdate();
            playerRanks.remove(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                refreshPlayerTeam(player);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Konnte Spieler-Rang nicht entfernen: " + e.getMessage());
        }
    }

    public Rank getPlayerRank(OfflinePlayer player) {
        String name = playerRanks.get(player.getUniqueId());
        if (name == null) {
            String defaultRank = plugin.getConfig().getString("ranks.defaultRank", "Spieler");
            return ranks.getOrDefault(defaultRank.toLowerCase(Locale.ROOT), null);
        }
        return ranks.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean addPermission(String rankName, String permission) {
        try (Connection connection = database.getConnection();
             PreparedStatement st = connection.prepareStatement("INSERT INTO rank_permissions(rank_name, permission) VALUES(?,?)")) {
            st.setString(1, rankName);
            st.setString(2, permission);
            st.executeUpdate();
            Rank rank = getRank(rankName);
            if (rank != null) {
                rank.getPermissions().add(permission);
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("Konnte Permission nicht hinzufügen: " + e.getMessage());
            return false;
        }
    }

    public boolean removePermission(String rankName, String permission) {
        try (Connection connection = database.getConnection();
             PreparedStatement st = connection.prepareStatement("DELETE FROM rank_permissions WHERE rank_name = ? AND permission = ?")) {
            st.setString(1, rankName);
            st.setString(2, permission);
            st.executeUpdate();
            Rank rank = getRank(rankName);
            if (rank != null) {
                rank.getPermissions().remove(permission);
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("Konnte Permission nicht entfernen: " + e.getMessage());
            return false;
        }
    }

    public boolean hasRankPermission(Player player, String permission) {
        Rank rank = getPlayerRank(player);
        return rank != null && rank.getPermissions().stream().anyMatch(p -> p.equalsIgnoreCase(permission));
    }

    public void refreshPlayerTeam(Player player) {
        Rank rank = getPlayerRank(player);
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "rank" + (rank != null ? rank.getPriority() : 0);
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        team.setPrefix(rank != null ? rank.getPrefix() : "");
        team.setSuffix(rank != null ? rank.getColor() + " [" + rank.getName() + "]" : "");
        team.addEntry(player.getName());
        player.setPlayerListName((rank != null ? rank.getColor() + player.getName() + ChatColor.RESET + " [" + rank.getName() + "]" : player.getName()));
    }
}
