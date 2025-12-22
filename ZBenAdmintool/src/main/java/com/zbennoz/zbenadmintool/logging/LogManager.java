package com.zbennoz.zbenadmintool.logging;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import com.zbennoz.zbenadmintool.text.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class LogManager {

    private final ZBenAdmintool plugin;
    private final MessageService messages;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    public LogManager(ZBenAdmintool plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void logBlock(Location location, Player player, Material block, String action) {
        if (!plugin.getConfig().getBoolean("logging.blocks.enabled", true)) return;
        CompletableFuture.runAsync(() -> {
            try (Connection connection = plugin.getDatabase().openConnection();
                 PreparedStatement st = connection.prepareStatement("INSERT INTO block_logs(player_uuid, player_name, world, x, y, z, block_type, action, timestamp) VALUES(?,?,?,?,?,?,?,?,?)")) {
                st.setString(1, player.getUniqueId().toString());
                st.setString(2, player.getName());
                st.setString(3, location.getWorld().getName());
                st.setInt(4, location.getBlockX());
                st.setInt(5, location.getBlockY());
                st.setInt(6, location.getBlockZ());
                st.setString(7, block.name());
                st.setString(8, action);
                st.setLong(9, Instant.now().getEpochSecond());
                st.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Fehler beim Speichern eines Block-Logs: " + e.getMessage());
            }
        });
    }

    public void logContainer(Location location, Player player, Material material, int amount, String action, String container) {
        if (!plugin.getConfig().getBoolean("logging.containers.enabled", true)) return;
        CompletableFuture.runAsync(() -> {
            try (Connection connection = plugin.getDatabase().openConnection();
                 PreparedStatement st = connection.prepareStatement("INSERT INTO container_logs(uuid, name, world, x, y, z, container_type, action, material, amount, ts) VALUES(?,?,?,?,?,?,?,?,?,?,?)")) {
                st.setString(1, player.getUniqueId().toString());
                st.setString(2, player.getName());
                st.setString(3, location.getWorld().getName());
                st.setInt(4, location.getBlockX());
                st.setInt(5, location.getBlockY());
                st.setInt(6, location.getBlockZ());
                st.setString(7, container);
                st.setString(8, action);
                st.setString(9, material.name());
                st.setInt(10, amount);
                st.setLong(11, Instant.now().getEpochSecond());
                st.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Fehler beim Speichern eines Container-Logs: " + e.getMessage());
            }
        });
    }

    public void sendBlockLogs(Player player, Location location, int page) {
        int limit = plugin.getConfig().getInt("logging.maxRowsPerQuery", 10);
        int offset = (page - 1) * limit;
        CompletableFuture.supplyAsync(() -> {
                    try {
                        return queryBlockLogs(location, limit, offset);
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.WARNING, "Fehler beim Abfragen von Block-Logs", e);
                        return null;
                    }
                })
                .thenAccept(entries -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (entries == null) {
                        player.sendMessage(messages.raw("inspect.error"));
                        return;
                    }
                    if (entries.isEmpty()) {
                        player.sendMessage(messages.raw("inspect.no_logs"));
                    } else {
                        entries.forEach(player::sendMessage);
                    }
                }));
    }

    public void sendContainerLogs(Player player, Location location, int page) {
        if (!plugin.getConfig().getBoolean("logging.containers.enabled", true)) {
            player.sendMessage(messages.raw("inspect.containers_disabled"));
            return;
        }
        int limit = 15;
        int offset = (page - 1) * limit;
        CompletableFuture.supplyAsync(() -> {
                    try {
                        return queryContainerLogs(location, limit, offset);
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.WARNING, "Fehler beim Abfragen von Container-Logs", e);
                        return null;
                    }
                })
                .thenAccept(entries -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (entries == null) {
                        player.sendMessage(messages.raw("inspect.error"));
                        return;
                    }
                    if (entries.isEmpty()) {
                        player.sendMessage(messages.raw("inspect.no_logs"));
                    } else {
                        entries.forEach(player::sendMessage);
                    }
                }));
    }

    private List<String> queryBlockLogs(Location location, int limit, int offset) throws SQLException {
        List<String> result = new ArrayList<>();
        try (Connection connection = plugin.getDatabase().openConnection();
             PreparedStatement st = connection.prepareStatement("SELECT player_name, action, block_type, timestamp FROM block_logs WHERE world=? AND x=? AND y=? AND z=? ORDER BY timestamp DESC LIMIT ? OFFSET ?")) {
            st.setString(1, location.getWorld().getName());
            st.setInt(2, location.getBlockX());
            st.setInt(3, location.getBlockY());
            st.setInt(4, location.getBlockZ());
            st.setInt(5, limit);
            st.setInt(6, offset);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                String time = timeFormatter.format(Instant.ofEpochSecond(rs.getLong("timestamp")));
                String msg = "[" + time + "] " + rs.getString("player_name") + " -> " + rs.getString("action") + " " + rs.getString("block_type");
                result.add(msg);
            }
        }
        return result;
    }

    private List<String> queryContainerLogs(Location location, int limit, int offset) throws SQLException {
        List<String> result = new ArrayList<>();
        try (Connection connection = plugin.getDatabase().openConnection();
             PreparedStatement st = connection.prepareStatement("SELECT name, action, material, amount, ts FROM container_logs WHERE world=? AND x=? AND y=? AND z=? ORDER BY ts DESC LIMIT ? OFFSET ?")) {
            st.setString(1, location.getWorld().getName());
            st.setInt(2, location.getBlockX());
            st.setInt(3, location.getBlockY());
            st.setInt(4, location.getBlockZ());
            st.setInt(5, limit);
            st.setInt(6, offset);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                String time = timeFormatter.format(Instant.ofEpochSecond(rs.getLong("ts")));
                String action = rs.getString("action").equalsIgnoreCase("ADD") ? "eingelagert" : "entfernt";
                String msg = messages.raw("inspect.container_entry")
                        .replace("%time%", time)
                        .replace("%player%", rs.getString("name"))
                        .replace("%amount%", String.valueOf(rs.getInt("amount")))
                        .replace("%material%", rs.getString("material"))
                        .replace("%action%", action);
                result.add(msg);
            }
        }
        return result;
    }
}
