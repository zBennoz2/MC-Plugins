package com.zbennoz.zbenlootr;

import com.zbennoz.zbenlootr.cache.LootCache;
import com.zbennoz.zbenlootr.commands.ZBenLootrCommand;
import com.zbennoz.zbenlootr.container.ContainerType;
import com.zbennoz.zbenlootr.database.Database;
import com.zbennoz.zbenlootr.database.MySQLDatabase;
import com.zbennoz.zbenlootr.database.SQLiteDatabase;
import com.zbennoz.zbenlootr.listeners.ContainerOpenListener;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Level;

public class ZBenLootrPlugin extends JavaPlugin {

    private Database database;
    private LootCache lootCache;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        setupCache();
        setupDatabase();
        registerCommands();
        registerListeners();
        getLogger().info("ZBenLootr enabled.");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
        if (lootCache != null) {
            lootCache.clear();
        }
    }

    public Database getDatabase() {
        return database;
    }

    public LootCache getLootCache() {
        return lootCache;
    }

    public Set<ContainerType> getEnabledContainerTypes() {
        ConfigurationSection section = getConfig().getConfigurationSection("containers");
        Set<ContainerType> types = EnumSet.noneOf(ContainerType.class);
        if (section != null) {
            for (String entry : section.getStringList("enabledTypes")) {
                try {
                    types.add(ContainerType.valueOf(entry.toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                    getLogger().warning("Unknown container type in config: " + entry);
                }
            }
        }
        if (types.isEmpty()) {
            types.add(ContainerType.CHEST);
        }
        return types;
    }

    public boolean detectDoubleChest() {
        return getConfig().getBoolean("containers.detectDoubleChest", true);
    }

    public String getLootMode() {
        return getConfig().getString("loot.mode", "VANILLA_LOOTTABLE");
    }

    public String getVanillaLootTable() {
        return getConfig().getString("loot.vanillaLootTable", "minecraft:chests/simple_dungeon");
    }

    public String getSeedMode() {
        return getConfig().getString("loot.seedMode", "PER_PLAYER");
    }

    public void reloadPlugin() {
        reloadConfig();
        setupCache();
        setupDatabase();
    }

    private void setupCache() {
        FileConfiguration config = getConfig();
        int maxEntries = config.getInt("cache.maxEntries", 10000);
        long expireSeconds = config.getLong("cache.expireSeconds", 600L);
        lootCache = new LootCache(maxEntries, expireSeconds);
    }

    private void setupDatabase() {
        FileConfiguration config = getConfig();
        String mode = config.getString("storage", "SQLITE").toUpperCase();
        if (database != null) {
            database.close();
        }
        if (mode.equals("MYSQL")) {
            ConfigurationSection mysql = config.getConfigurationSection("mysql");
            if (mysql == null) {
                getLogger().severe("MySQL configuration section missing, falling back to SQLite.");
                database = new SQLiteDatabase(new File(getDataFolder(), "loot.db"), this);
            } else {
                String host = mysql.getString("host", "localhost");
                int port = mysql.getInt("port", 3306);
                String databaseName = mysql.getString("database", "zbenlootr");
                String user = mysql.getString("user", "root");
                String password = mysql.getString("password", "password");
                database = new MySQLDatabase(host, port, databaseName, user, password, this);
            }
        } else {
            database = new SQLiteDatabase(new File(getDataFolder(), "loot.db"), this);
        }
        try {
            database.init();
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Failed to initialize database", ex);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void registerCommands() {
        ZBenLootrCommand commandExecutor = new ZBenLootrCommand(this);
        if (getCommand("zbenlootr") != null) {
            getCommand("zbenlootr").setExecutor(commandExecutor);
            getCommand("zbenlootr").setTabCompleter(commandExecutor);
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ContainerOpenListener(this), this);
    }
}
