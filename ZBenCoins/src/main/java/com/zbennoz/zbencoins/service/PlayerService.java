package com.zbennoz.zbencoins.service;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.database.PlayerDao;
import com.zbennoz.zbencoins.database.PlayerRecord;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Service für Spieler-spezifische Aktionen.
 */
public class PlayerService {

    private final ZBenCoinsPlugin plugin;
    private final PlayerDao playerDao;
    private final FileConfiguration config;

    public PlayerService(ZBenCoinsPlugin plugin, PlayerDao playerDao, FileConfiguration config) {
        this.plugin = plugin;
        this.playerDao = playerDao;
        this.config = config;
    }

    public PlayerRecord ensurePlayer(UUID uuid, String name) {
        try {
            PlayerRecord existing = playerDao.find(uuid);
            if (existing == null) {
                long starting = config.getLong("starting-coins", 0);
                playerDao.insert(uuid, name, starting);
                return new PlayerRecord(uuid, name, starting);
            }
            if (name != null && !name.equals(existing.getName())) {
                playerDao.updateName(uuid, name);
            }
            return playerDao.find(uuid);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Sicherstellen des Spielers", e);
            return null;
        }
    }

    public PlayerRecord find(UUID uuid) {
        try {
            return playerDao.find(uuid);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Lesen eines Spielers", e);
            return null;
        }
    }

    public PlayerRecord findByName(String name) {
        try {
            return playerDao.findByName(name);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Laden eines Spielers", e);
            return null;
        }
    }
}
