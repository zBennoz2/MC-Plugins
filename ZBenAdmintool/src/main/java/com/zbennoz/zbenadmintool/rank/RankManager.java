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
import java.util.EnumSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RankManager {

    private final ZBenAdmintool plugin;
    private final Database database;
    private final Map<String, Rank> ranks = new HashMap<>();
    private final Map<UUID, String> playerRanks = new HashMap<>();
    private RankPermissionBridge permissionBridge;
    private static final List<Integer> ALLOWED_BACKPACK_SIZES = List.of(9, 18, 27, 36, 45, 54);

    public RankManager(ZBenAdmintool plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    public void setPermissionBridge(RankPermissionBridge permissionBridge) {
        this.permissionBridge = permissionBridge;
    }

    public void init() {
        loadRanks();
        ensureDefaults();
        ensureRolePermissionMappings();
        loadPlayerRanks();
        refreshAllTeams();
    }

    private void ensureDefaults() {
        if (ranks.isEmpty()) {
            createRank("Owner", ChatColor.DARK_RED.getName(), ChatColor.DARK_RED.toString(), 100, "", "", 54);
            createRank("Admin", ChatColor.RED.getName(), ChatColor.RED.toString(), 80, "", "", 45);
            createRank("Moderator", ChatColor.GOLD.getName(), ChatColor.GOLD.toString(), 60, "", "", 36);
            createRank("Supporter", ChatColor.GREEN.getName(), ChatColor.GREEN.toString(), 40, "", "", 27);
            createRank("Spieler", ChatColor.WHITE.getName(), ChatColor.WHITE.toString(), 0, "", "", 9);
            applyDefaultPermissions();
        }
    }

    private void applyDefaultPermissions() {
        addDefaultPermissions("Owner",
                List.of(
                        "zbenadmintool.*",
                        "minecraft.command.kick",
                        "minecraft.command.ban",
                        "minecraft.command.unban"
                ),
                EnumSet.of(RankPermission.ALL));
        addDefaultPermissions("Admin",
                List.of(
                        "zbenadmintool.*",
                        "minecraft.command.kick",
                        "minecraft.command.ban",
                        "minecraft.command.unban"
                ),
                EnumSet.of(
                        RankPermission.BAN,
                        RankPermission.KICK,
                        RankPermission.MUTE,
                        RankPermission.WARN,
                        RankPermission.INSPECT,
                        RankPermission.RANK_MANAGE,
                        RankPermission.ADMIN_MENU,
                        RankPermission.ADMIN_MODE,
                        RankPermission.VANISH,
                        RankPermission.LOGS,
                        RankPermission.OFFLINE_INVENTORY,
                        RankPermission.OFFLINE_ENDERCHEST));
        addDefaultPermissions("Moderator",
                List.of(
                        "zbenadmintool.inspect",
                        "zbenadmintool.vanish",
                        "minecraft.command.kick",
                        "minecraft.command.ban"
                ),
                EnumSet.of(
                        RankPermission.KICK,
                        RankPermission.MUTE,
                        RankPermission.WARN,
                        RankPermission.INSPECT));
        addDefaultPermissions("Supporter",
                List.of(
                        "zbenadmintool.inspect",
                        "minecraft.command.kick"
                ),
                EnumSet.of(RankPermission.INSPECT));
    }

    private void ensureRolePermissionMappings() {
        ranks.values().forEach(rank -> {
            if (!rank.getRolePermissions().isEmpty()) {
                return;
            }
            String name = rank.getName().toLowerCase(Locale.ROOT);
            if (name.equals("owner")) {
                addPermission(rank.getName(), RankPermission.ALL.name());
            } else if (name.equals("admin")) {
                EnumSet.of(RankPermission.BAN, RankPermission.KICK, RankPermission.MUTE, RankPermission.WARN, RankPermission.INSPECT,
                        RankPermission.RANK_MANAGE, RankPermission.ADMIN_MENU, RankPermission.ADMIN_MODE, RankPermission.VANISH,
                        RankPermission.LOGS, RankPermission.OFFLINE_INVENTORY, RankPermission.OFFLINE_ENDERCHEST)
                        .forEach(perm -> addPermission(rank.getName(), perm.name()));
            } else if (name.equals("moderator")) {
                EnumSet.of(RankPermission.KICK, RankPermission.MUTE, RankPermission.WARN, RankPermission.INSPECT)
                        .forEach(perm -> addPermission(rank.getName(), perm.name()));
            }
        });
    }

    private void addDefaultPermissions(String rankName, List<String> permissions, Set<RankPermission> rolePermissions) {
        permissions.forEach(permission -> addPermission(rankName, permission));
        rolePermissions.forEach(permission -> addPermission(rankName, permission.name()));
    }

    private void loadRanks() {
        ranks.clear();
        try (Connection connection = database.openConnection();
             PreparedStatement st = connection.prepareStatement("SELECT * FROM ranks")) {
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                String name = rs.getString("name");
                String colorText = rs.getString("color");
                String legacyColor = parseLegacyColor(colorText);
                int priority = rs.getInt("priority");
                String prefix = rs.getString("prefix");
                String suffix = rs.getString("suffix");
                int backpackSlots = resolveBackpackSlots(name, rs.getInt("backpack_slots"));
                Rank rank = new Rank(name, colorText, legacyColor, priority, prefix, suffix, backpackSlots);
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
                String value = perm.getString("permission");
                RankPermission rolePermission = RankPermission.fromString(value);
                if (rolePermission != null) {
                    rank.getRolePermissions().add(rolePermission);
                } else {
                    rank.getBukkitPermissions().add(value);
                }
            }
        }
    }

    private void loadPlayerRanks() {
        playerRanks.clear();
        try (Connection connection = database.openConnection();
             PreparedStatement st = connection.prepareStatement("SELECT * FROM player_ranks")) {
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                playerRanks.put(UUID.fromString(rs.getString("uuid")), rs.getString("rank_name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Konnte Spieler-Ränge nicht laden: " + e.getMessage());
        }
    }

    public boolean createRank(String name, String colorText, String legacyColor, int priority, String prefix, String suffix, int backpackSlots) {
        if (ranks.containsKey(name.toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (!isValidBackpackSize(backpackSlots)) {
            backpackSlots = defaultBackpackSlotsFor(name);
        }
        try (Connection connection = database.openConnection();
             PreparedStatement st = connection.prepareStatement("INSERT INTO ranks(name, color, priority, prefix, suffix, backpack_slots) VALUES(?,?,?,?,?,?)")) {
            st.setString(1, name);
            st.setString(2, colorText);
            st.setInt(3, priority);
            st.setString(4, prefix);
            st.setString(5, suffix);
            st.setInt(6, backpackSlots);
            st.executeUpdate();
            Rank rank = new Rank(name, colorText, legacyColor, priority, prefix, suffix, backpackSlots);
            ranks.put(name.toLowerCase(Locale.ROOT), rank);
            refreshAllTeams();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("Konnte Rang nicht erstellen: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteRank(String name) {
        List<UUID> affectedPlayers = playerRanks.entrySet().stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(name))
                .map(Map.Entry::getKey)
                .toList();
        try (Connection connection = database.openConnection()) {
            try (PreparedStatement perm = connection.prepareStatement("DELETE FROM rank_permissions WHERE rank_name = ?")) {
                perm.setString(1, name);
                perm.executeUpdate();
            }
            try (PreparedStatement players = connection.prepareStatement("DELETE FROM player_ranks WHERE rank_name = ?")) {
                players.setString(1, name);
                players.executeUpdate();
            }
            try (PreparedStatement st = connection.prepareStatement("DELETE FROM ranks WHERE name = ?")) {
                st.setString(1, name);
                st.executeUpdate();
            }
            ranks.remove(name.toLowerCase(Locale.ROOT));
            playerRanks.entrySet().removeIf(entry -> entry.getValue().equalsIgnoreCase(name));
            refreshAllTeams();
            reapplyAfterDeletion(affectedPlayers);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("Konnte Rang nicht löschen: " + e.getMessage());
            return false;
        }
    }

    private void reapplyAfterDeletion(List<UUID> affectedPlayers) {
        if (permissionBridge == null) {
            return;
        }
        for (UUID uuid : affectedPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                permissionBridge.applyPermissions(player);
            }
        }
    }

    public Rank getRank(String name) {
        if (name == null) return null;
        return ranks.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<Rank> getRanks() {
        return ranks.values();
    }

    public boolean isValidBackpackSize(int slots) {
        return ALLOWED_BACKPACK_SIZES.contains(slots);
    }

    public int defaultBackpackSlotsFor(String name) {
        if (name != null && name.equalsIgnoreCase("spieler")) {
            return 9;
        }
        return 27;
    }

    private int resolveBackpackSlots(String rankName, int storedValue) {
        if (isValidBackpackSize(storedValue)) {
            return storedValue;
        }
        return defaultBackpackSlotsFor(rankName);
    }

    public void setPlayerRank(UUID uuid, String rankName) {
        if (getRank(rankName) == null) {
            return;
        }
        try (Connection connection = database.openConnection();
             PreparedStatement st = connection.prepareStatement("INSERT OR REPLACE INTO player_ranks(uuid, rank_name) VALUES(?,?)")) {
            st.setString(1, uuid.toString());
            st.setString(2, rankName);
            st.executeUpdate();
            playerRanks.put(uuid, rankName);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                refreshPlayerTeam(player);
                if (permissionBridge != null) {
                    permissionBridge.applyPermissions(player);
                }
            }
            Rank rank = getRank(rankName);
            if (rank != null) {
                plugin.getBackpackIntegration().applyBackpackSize(uuid, rank.getBackpackSlots());
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Konnte Spieler-Rang nicht speichern: " + e.getMessage());
        }
    }

    public void removePlayerRank(UUID uuid) {
        try (Connection connection = database.openConnection();
             PreparedStatement st = connection.prepareStatement("DELETE FROM player_ranks WHERE uuid = ?")) {
            st.setString(1, uuid.toString());
            st.executeUpdate();
            playerRanks.remove(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                refreshPlayerTeam(player);
                if (permissionBridge != null) {
                    permissionBridge.applyPermissions(player);
                }
                Rank fallback = getPlayerRank(player);
                if (fallback != null) {
                    plugin.getBackpackIntegration().applyBackpackSize(uuid, fallback.getBackpackSlots());
                }
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
        try (Connection connection = database.openConnection();
             PreparedStatement st = connection.prepareStatement("INSERT OR REPLACE INTO rank_permissions(rank_name, permission) VALUES(?,?)")) {
            st.setString(1, rankName);
            st.setString(2, permission);
            st.executeUpdate();
            Rank rank = getRank(rankName);
            if (rank != null) {
                RankPermission internal = RankPermission.fromString(permission);
                if (internal != null) {
                    rank.getRolePermissions().add(internal);
                } else {
                    rank.getBukkitPermissions().add(permission);
                }
            }
            reapplyPermissions(rankName);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("Konnte Permission nicht hinzufügen: " + e.getMessage());
            return false;
        }
    }

    public boolean removePermission(String rankName, String permission) {
        try (Connection connection = database.openConnection();
             PreparedStatement st = connection.prepareStatement("DELETE FROM rank_permissions WHERE rank_name = ? AND permission = ?")) {
            st.setString(1, rankName);
            st.setString(2, permission);
            st.executeUpdate();
            Rank rank = getRank(rankName);
            if (rank != null) {
                RankPermission internal = RankPermission.fromString(permission);
                if (internal != null) {
                    rank.getRolePermissions().remove(internal);
                }
                rank.getBukkitPermissions().remove(permission);
            }
            reapplyPermissions(rankName);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("Konnte Permission nicht entfernen: " + e.getMessage());
            return false;
        }
    }

    public boolean updateBackpackSlots(String rankName, int slots) {
        Rank rank = getRank(rankName);
        if (rank == null) {
            return false;
        }
        if (!isValidBackpackSize(slots)) {
            return false;
        }
        try (Connection connection = database.openConnection();
             PreparedStatement st = connection.prepareStatement("UPDATE ranks SET backpack_slots = ? WHERE name = ?")) {
            st.setInt(1, slots);
            st.setString(2, rankName);
            st.executeUpdate();
            Rank updated = new Rank(rank.getName(), rank.getColorText(), rank.getLegacyColor(), rank.getPriority(), rank.getPrefix(), rank.getSuffix(), slots);
            updated.getBukkitPermissions().addAll(rank.getBukkitPermissions());
            updated.getRolePermissions().addAll(rank.getRolePermissions());
            ranks.put(rankName.toLowerCase(Locale.ROOT), updated);
            refreshAllTeams();
            playerRanks.entrySet().stream()
                    .filter(entry -> entry.getValue().equalsIgnoreCase(rankName))
                    .map(Map.Entry::getKey)
                    .forEach(uuid -> plugin.getBackpackIntegration().applyBackpackSize(uuid, slots));
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("Konnte Backpack-Slots nicht aktualisieren: " + e.getMessage());
            return false;
        }
    }

    public boolean hasRankPermission(Player player, RankPermission permission) {
        Rank rank = getPlayerRank(player);
        if (rank == null) {
            return false;
        }
        if (rank.getRolePermissions().contains(RankPermission.ALL)) {
            return true;
        }
        if (rank.getRolePermissions().contains(permission)) {
            return true;
        }
        return hasBukkitWildcard(rank);
    }

    public boolean hasRankPermission(Player player, String permission) {
        RankPermission internal = RankPermission.fromString(permission);
        if (internal != null) {
            return hasRankPermission(player, internal);
        }
        Rank rank = getPlayerRank(player);
        return rank != null && rank.getBukkitPermissions().stream().anyMatch(p -> matchesPermission(p, permission));
    }

    private boolean hasBukkitWildcard(Rank rank) {
        return rank.getBukkitPermissions().stream().anyMatch(perm -> matchesPermission(perm, "zbenadmintool.*"));
    }

    private boolean matchesPermission(String rankPermission, String requested) {
        if (rankPermission.equalsIgnoreCase(requested)) {
            return true;
        }
        if (rankPermission.endsWith(".*")) {
            String prefix = rankPermission.substring(0, rankPermission.length() - 2).toLowerCase(Locale.ROOT);
            return requested.toLowerCase(Locale.ROOT).startsWith(prefix);
        }
        return false;
    }

    public void refreshPlayerTeam(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        removeFromRankTeams(scoreboard, player.getName());

        Rank rank = getPlayerRank(player);
        if (rank == null) {
            player.setPlayerListName(player.getName());
            return;
        }

        String teamName = createTeamName(rank);
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        team.setPrefix(rank.getPrefix());
        team.setSuffix(" " + rank.getLegacyColor() + "[" + rank.getName() + "]");
        team.addEntry(player.getName());
        player.setPlayerListName(rank.getLegacyColor() + player.getName() + ChatColor.RESET + " [" + rank.getName() + "]");
    }

    public void refreshAllTeams() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        scoreboard.getTeams().stream()
                .filter(team -> team.getName().startsWith("rank_"))
                .forEach(Team::unregister);

        List<Rank> sorted = ranks.values().stream()
                .sorted(Comparator.comparingInt(Rank::getPriority).reversed())
                .toList();
        for (Rank rank : sorted) {
            String teamName = createTeamName(rank);
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }
            team.setPrefix(rank.getPrefix());
            team.setSuffix(" " + rank.getLegacyColor() + "[" + rank.getName() + "]");
        }

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

    public boolean isValidRankName(String name) {
        if (name == null) {
            return false;
        }
        return name.length() >= 3 && name.length() <= 24 && name.matches("[A-Za-z0-9_-]+");
    }

    public List<String> getRankNames() {
        return new ArrayList<>(ranks.values()).stream()
                .sorted(Comparator.comparingInt(Rank::getPriority).reversed())
                .map(Rank::getName)
                .toList();
    }

    private void reapplyPermissions(String rankName) {
        if (permissionBridge == null) {
            return;
        }
        Bukkit.getOnlinePlayers().forEach(player -> {
            Rank current = getPlayerRank(player);
            if (current != null && current.getName().equalsIgnoreCase(rankName)) {
                permissionBridge.applyPermissions(player);
            }
        });
    }

    private String createTeamName(Rank rank) {
        String priorityPart = String.format("%04d", Math.max(0, 9999 - rank.getPriority()));
        String hash = Integer.toHexString(rank.getName().hashCode());
        String name = ("rank_" + priorityPart + "_" + hash);
        if (name.length() > 16) {
            name = name.substring(0, 16);
        }
        return name;
    }
}
