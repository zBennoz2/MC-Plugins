package com.zbennoz.zbenteleport.util;

import com.zbennoz.zbenteleport.ZBenTeleportPlugin;
import com.zbennoz.zbenteleport.data.TeleportDatabase;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HomeManager {

    private static final Pattern HOME_LIMIT_PERMISSION = Pattern.compile("zbenteleport\\.homes\\.(\\d+)");
    private final ZBenTeleportPlugin plugin;
    private final TeleportDatabase database;
    private final Map<UUID, Map<String, Location>> cache = new HashMap<>();

    public HomeManager(ZBenTeleportPlugin plugin, TeleportDatabase database) {
        this.plugin = plugin;
        this.database = database;
    }

    public Map<String, Location> loadHomes(UUID uuid) {
        return cache.computeIfAbsent(uuid, id -> {
            Map<String, Location> map = new HashMap<>();
            for (TeleportDatabase.HomeRecord record : database.loadHomes(id)) {
                var world = Bukkit.getWorld(record.world());
                if (world != null) {
                    map.put(record.name(), new Location(world, record.x(), record.y(), record.z(), record.yaw(), record.pitch()));
                }
            }
            return map;
        });
    }

    public int getHomeLimit(OfflinePlayer player) {
        int limit = plugin.getConfig().getInt("homes.default-limit", 1);
        for (var perm : player.getEffectivePermissions()) {
            Matcher matcher = HOME_LIMIT_PERMISSION.matcher(perm.getPermission());
            if (matcher.matches() && perm.getValue()) {
                limit = Math.max(limit, Integer.parseInt(matcher.group(1)));
            }
        }
        return limit;
    }

    public boolean setHome(UUID uuid, String name, Location location, int limit) {
        var homes = loadHomes(uuid);
        if (!homes.containsKey(name) && homes.size() >= limit) {
            return false;
        }
        homes.put(name, location.clone());
        database.saveHomeAsync(uuid, name, location);
        return true;
    }

    public boolean deleteHome(UUID uuid, String name) {
        var homes = loadHomes(uuid);
        if (homes.remove(name) != null) {
            database.deleteHomeAsync(uuid, name);
            return true;
        }
        return false;
    }
}
