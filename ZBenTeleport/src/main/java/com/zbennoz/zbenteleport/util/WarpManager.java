package com.zbennoz.zbenteleport.util;

import com.zbennoz.zbenteleport.data.TeleportDatabase;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WarpManager {

    private final TeleportDatabase database;
    private final Map<String, Location> warps = new HashMap<>();

    public WarpManager(TeleportDatabase database) {
        this.database = database;
        refresh();
    }

    public void refresh() {
        warps.clear();
        List<String> names = database.listWarps();
        for (String name : names) {
            Location loc = database.loadWarp(name);
            if (loc != null) {
                warps.put(name.toLowerCase(Locale.ROOT), loc);
            }
        }
    }

    public Map<String, Location> warps() {
        return warps;
    }

    public Location getWarp(String name) {
        return warps.get(name.toLowerCase(Locale.ROOT));
    }

    public void setWarp(String name, Location location) {
        String key = name.toLowerCase(Locale.ROOT);
        warps.put(key, location.clone());
        database.saveWarpAsync(key, location);
    }

    public boolean deleteWarp(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        if (warps.remove(key) != null) {
            database.deleteWarpAsync(key);
            return true;
        }
        return false;
    }
}
