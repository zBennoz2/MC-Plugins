package com.zbennoz.zbenadmintool.service;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeleportLogService {

    private final ZBenAdmintool plugin;
    private final File file;

    public TeleportLogService(ZBenAdmintool plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "teleport-logs.yml");
    }

    public void logTeleport(String admin, String target, Location location) {
        try {
            if (!file.exists()) {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            }
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            List<Map<String, Object>> list = new ArrayList<>();
            for (Map<?, ?> raw : config.getMapList("logs")) {
                Map<String, Object> converted = new HashMap<>();
                raw.forEach((k, v) -> converted.put(String.valueOf(k), v));
                list.add(converted);
            }
            Map<String, Object> entry = new HashMap<>();
            entry.put("admin", admin);
            entry.put("target", target);
            entry.put("time", System.currentTimeMillis());
            entry.put("world", location.getWorld().getName());
            entry.put("x", location.getX());
            entry.put("y", location.getY());
            entry.put("z", location.getZ());
            list = new ArrayList<>(list);
            list.add(entry);
            config.set("logs", list);
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Konnte Teleport-Log nicht speichern: " + e.getMessage());
        }
    }
}
