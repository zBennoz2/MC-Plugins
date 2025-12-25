package com.zbennoz.zbencoins;

import com.zbennoz.zbencoins.command.BaltopCommand;
import com.zbennoz.zbencoins.command.CoinsCommand;
import com.zbennoz.zbencoins.command.MarktCommand;
import com.zbennoz.zbencoins.command.PayCommand;
import com.zbennoz.zbencoins.config.ConfigManager;
import com.zbennoz.zbencoins.database.Database;
import com.zbennoz.zbencoins.database.PlayerDao;
import com.zbennoz.zbencoins.database.TransactionDao;
import com.zbennoz.zbencoins.gui.GuiManager;
import com.zbennoz.zbencoins.listener.PlayerJoinListener;
import com.zbennoz.zbencoins.service.CoinService;
import com.zbennoz.zbencoins.service.PlayerService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Haupteinstieg für das Plugin.
 */
public class ZBenCoinsPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private Database database;
    private PlayerService playerService;
    private CoinService coinService;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.load();

        database = new Database(this);
        try {
            database.connect();
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Konnte Datenbank nicht öffnen", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        PlayerDao playerDao = new PlayerDao(database.getConnection());
        TransactionDao transactionDao = new TransactionDao(database.getConnection());
        playerService = new PlayerService(this, playerDao, configManager.getConfig());
        coinService = new CoinService(this, playerDao, transactionDao);

        guiManager = new GuiManager();

        registerCommands();
        registerListeners();
    }

    private void registerCommands() {
        getCommand("markt").setExecutor(new MarktCommand(this));
        getCommand("coins").setExecutor(new CoinsCommand(this, coinService, playerService));
        getCommand("pay").setExecutor(new PayCommand(this, coinService, playerService));
        getCommand("baltop").setExecutor(new BaltopCommand(this, coinService));
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(playerService, this), this);
        Bukkit.getPluginManager().registerEvents(guiManager, this);
    }

    @Override
    public void onDisable() {
        if (coinService != null) {
            coinService.shutdown();
        }
        if (database != null) {
            database.close();
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CoinService getCoinService() {
        return coinService;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }
}
