package com.zbennoz.zbenadmintool.rank;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import com.zbennoz.zbenadmintool.storage.Database;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

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
            connection.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS player_ranks (uuid TEXT PRIMARY KEY, rank_name TEXT);");
            connection.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS rank_permissions (rank_name TEXT, permission TEXT, PRIMARY KEY(rank_name, permission));");
        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler beim Erstellen der Tabellen: " + e.getMessage());
        }
        loadRanks();
        loadPlayerRanks();
        ensureDefaults();
        refreshAllTeams();
    }

    private void ensureDefaults() {
        if (ranks.isEmpty()) {
            createRank("Owner", ChatColor.DARK_RED.getName(), ChatColor.DARK_RED.toString(), 100, ChatColor.DARK_RED + "[Owner] ", "");
            createRank("Admin", ChatColor.RED.getName(), ChatColor.RED.toString(), 80, ChatColor.RED + "[Admin] ", "");
            createRank("Moderator", ChatColor.GOLD.getName(), ChatColor.GOLD.toString(), 60, ChatColor.GOLD + "[Mod] ", "");
            createRank("Supporter", ChatColor.GREEN.getName(), ChatColor.GREEN.toString(), 40, ChatColor.GREEN + "[Sup] ", "");
            createRank("Spieler", ChatColor.WHITE.getName(), ChatColor.WHITE.toString(), 0, "", "");
        }
    }

    private void loadRanks() {
        ranks.clear();
        try (Connection connection = database.getConnection();
             PreparedStatement st = connection.prepareStatement("SELECT * FROM ranks")) {
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                String name = rs.getString("name");
                String colorText = rs.getString("color");
                String legacyColor = parseLegacyColor(colorText);
                int priority = rs.getInt("priority");
                String prefix = rs.getString("prefix");
                String suffix = rs.getString("suffix");
                Rank rank = new Rank(name, colorText, legacyColor, priority, prefix, suffix);
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
                playerRanks.put(UUID.fromString(rs.getString("uuid")), rs.getString("rank_name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Konnte Spieler-Ränge nicht laden: " + e.getMessage());
        }
    }

    public boolean createRank(String name, String colorText, String legacyColor, int priority, String prefix, String suffix) {
        try (Connection connection = database.getConnection();
             PreparedStatement st = connection.prepareStatement("INSERT OR REPLACE INTO ranks(name, color, priority, prefix, suffix) VALUES(?,?,?,?,?)")) {
            st.setString(1, name);
            st.setString(2, colorText);
            st.setInt(3, priority);
            st.setString(4, prefix);
            st.setString(5, suffix);
            st.executeUpdate();
            Rank rank = new Rank(name, colorText, legacyColor, priority, prefix, suffix);
            ranks.put(name.toLowerCase(Locale.ROOT), rank);
            refreshAllTeams();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("Konnte Rang nicht erstellen: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteRank(String name) {
        try (Connection connection = database.getConnection()) {
            try (PreparedStatement deletePerms = connection.prepareStatement("DELETE FROM rank_permissions WHERE rank_name = ?")) {
                deletePerms.setString(1, name);
                deletePerms.executeUpdate();
            }
            try (PreparedStatement updatePlayers = connection.prepareStatement("UPDATE player_ranks SET rank_name = ? WHERE rank_name = ?")) {
                updatePlayers.setString(1, plugin.getConfig().getString("ranks.defaultRank", "Spieler"));
                updatePlayers.setString(2, name);
                updatePlayers.executeUpdate();
            }
            try (PreparedStatement st = connection.prepareStatement("DELETE FROM ranks WHERE name = ?")) {
                st.setString(1, name);
                st.executeUpdate();
            }
            ranks.remove(name.toLowerCase(Locale.ROOT));
            loadPlayerRanks();
            refreshAllTeams();
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
             PreparedStatement st = connection.prepareStatement("INSERT OR REPLACE INTO player_ranks(uuid, rank_name) VALUES(?,?)")) {
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
             PreparedStatement st = connection.prepareStatement("DELETE FROM player_ranks WHERE uuid = ?")) {
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
             PreparedStatement st = connection.prepareStatement("INSERT OR REPLACE INTO rank_permissions(rank_name, permission) VALUES(?,?)")) {
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
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        removeFromRankTeams(scoreboard, player.getName());

        Rank rank = getPlayerRank(player);
        if (rank == null) {
            player.setPlayerListName(player.getName());
            return;
        }

        String teamName = ("rank_" + rank.getName()).replaceAll("[^A-Za-z0-9_]", "");
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        team.setPrefix(rank.getPrefix());
        team.setSuffix(rank.getLegacyColor() + " [" + rank.getName() + "]");
        team.addEntry(player.getName());
        player.setPlayerListName(rank.getLegacyColor() + player.getName() + ChatColor.RESET + " [" + rank.getName() + "]");
    }

    public void refreshAllTeams() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        scoreboard.getTeams().stream()
                .filter(team -> team.getName().startsWith("rank_"))
                .forEach(Team::unregister);

        Bukkit.getOnlinePlayers().forEach(this::refreshPlayerTeam);
    }

    private void removeFromRankTeams(Scoreboard scoreboard, String playerName) {
        scoreboard.getTeams().stream()
                .filter(team -> team.getName().startsWith("rank_"))
                .forEach(team -> team.removeEntry(playerName));
    }

    public String parseLegacyColor(String input) {
        if (input == null) {
            return ChatColor.WHITE.toString();
        }
        try {
            return ChatColor.of(input).toString();
        } catch (IllegalArgumentException ignored) {
            try {
                return ChatColor.valueOf(input.toUpperCase(Locale.ROOT)).toString();
            } catch (IllegalArgumentException e) {
                return ChatColor.WHITE.toString();
            }
        }
    }

    public boolean isValidColor(String input) {
        if (input == null) {
            return false;
        }
        try {
            ChatColor.of(input);
            return true;
        } catch (IllegalArgumentException ignored) {
            try {
                ChatColor.valueOf(input.toUpperCase(Locale.ROOT));
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }
}
