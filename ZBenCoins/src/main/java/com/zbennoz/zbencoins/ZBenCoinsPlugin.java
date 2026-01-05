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
import com.zbennoz.zbencoins.listener.MarketChatListener;
import com.zbennoz.zbencoins.listener.ServerOfferChatListener;
import com.zbennoz.zbencoins.listener.JobChatListener;
import com.zbennoz.zbencoins.job.JobDao;
import com.zbennoz.zbencoins.job.JobLogDao;
import com.zbennoz.zbencoins.market.MarketLogDao;
import com.zbennoz.zbencoins.market.OfferDao;
import com.zbennoz.zbencoins.serveroffer.ServerOfferDao;
import com.zbennoz.zbencoins.serveroffer.ServerOfferService;
import com.zbennoz.zbencoins.service.CoinService;
import com.zbennoz.zbencoins.service.JobService;
import com.zbennoz.zbencoins.service.MarketService;
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
    private MarketService marketService;
    private JobService jobService;
    private ServerOfferService serverOfferService;
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
        OfferDao offerDao = new OfferDao(database.getConnection());
        MarketLogDao marketLogDao = new MarketLogDao(database.getConnection());
        ServerOfferDao serverOfferDao = new ServerOfferDao(database.getConnection());
        JobDao jobDao = new JobDao(database.getConnection());
        JobLogDao jobLogDao = new JobLogDao(database.getConnection());
        playerService = new PlayerService(this, playerDao, configManager.getConfig());
        coinService = new CoinService(this, playerDao, transactionDao);
        marketService = new MarketService(this, offerDao, marketLogDao, playerDao, transactionDao, database.getConnection());
        serverOfferService = new ServerOfferService(this, serverOfferDao, playerDao, transactionDao, database.getConnection());
        jobService = new JobService(this, jobDao, jobLogDao, playerDao, transactionDao, database.getConnection());

        guiManager = new GuiManager();

        registerCommands();
        registerListeners();
    }

    private void registerCommands() {
        getCommand("markt").setExecutor(new MarktCommand(this));
        getCommand("coins").setExecutor(new CoinsCommand(this, coinService, playerService));
        getCommand("pay").setExecutor(new PayCommand(this, coinService, playerService));
        getCommand("baltop").setExecutor(new BaltopCommand(this, coinService));
        getCommand("serveroffer").setExecutor(new com.zbennoz.zbencoins.command.ServerOfferCommand(this, serverOfferService));
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(playerService, marketService, this), this);
        Bukkit.getPluginManager().registerEvents(guiManager, this);
        Bukkit.getPluginManager().registerEvents(new MarketChatListener(marketService), this);
        Bukkit.getPluginManager().registerEvents(new ServerOfferChatListener(serverOfferService), this);
        Bukkit.getPluginManager().registerEvents(new JobChatListener(jobService), this);
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

    public MarketService getMarketService() {
        return marketService;
    }

    public ServerOfferService getServerOfferService() {
        return serverOfferService;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public JobService getJobService() {
        return jobService;
    }
}
