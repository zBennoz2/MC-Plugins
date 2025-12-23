package com.zbennoz.zbentimber;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class PlayerSettingsStorage {
    private final File file;
    private final FileConfiguration data;

    public PlayerSettingsStorage(ZBenTimberPlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "toggles.yml");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public boolean isLeavesEnabled(UUID uuid, boolean defaultValue) {
        String path = "players." + uuid + ".leaves";
        return data.contains(path) ? data.getBoolean(path) : defaultValue;
    }

    public void setLeaves(UUID uuid, boolean enabled) {
        data.set("players." + uuid + ".leaves", enabled);
        save();
    }

    public void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
