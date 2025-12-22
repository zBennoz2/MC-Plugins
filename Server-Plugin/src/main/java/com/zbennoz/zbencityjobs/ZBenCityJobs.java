package com.zbennoz.zbencityjobs;

import com.zbennoz.zbencityjobs.commands.CityCommand;
import com.zbennoz.zbencityjobs.commands.CoinsCommand;
import com.zbennoz.zbencityjobs.commands.CompanyCommand;
import com.zbennoz.zbencityjobs.commands.JobsCommand;
import com.zbennoz.zbencityjobs.commands.MarketCommand;
import com.zbennoz.zbencityjobs.gui.JobBoardGUI;
import com.zbennoz.zbencityjobs.gui.MarketGUI;
import com.zbennoz.zbencityjobs.listeners.CoinAccountListener;
import com.zbennoz.zbencityjobs.listeners.InventoryListener;
import com.zbennoz.zbencityjobs.listeners.JobCreationListener;
import com.zbennoz.zbencityjobs.repository.*;
import com.zbennoz.zbencityjobs.service.*;
import com.zbennoz.zbencityjobs.storage.DatabaseManager;
import com.zbennoz.zbencityjobs.util.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public class ZBenCityJobs extends JavaPlugin {
    private DatabaseManager databaseManager;
    private MessageService messages;
    private CoinService coinService;
    private JobService jobService;
    private MarketService marketService;
    private CompanyService companyService;
    private CityService cityService;
    private JobCreationManager jobCreationManager;

    @Override
    public void onEnable() {
        // Ensure plugin data folder exists and provide a visible startup log.
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Could not create plugin data folder");
        }
        saveDefaultConfig();
        getLogger().info("ZBenCityJobs is enabling...");

        messages = new MessageService(this);
        messages.load();

        databaseManager = new DatabaseManager(this);
        coinService = new CoinService(this);
        if (!coinService.init()) {
            getLogger().severe(messages.get("errors.coins-missing"));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        JobRepository jobRepository = new JobRepository(databaseManager);
        ListingRepository listingRepository = new ListingRepository(databaseManager);
        CompanyRepository companyRepository = new CompanyRepository(databaseManager);
        CityRepository cityRepository = new CityRepository(databaseManager);
        AuditLogRepository auditLogRepository = new AuditLogRepository(databaseManager);
        try {
            jobRepository.init();
            listingRepository.init();
            companyRepository.init();
            cityRepository.init();
            auditLogRepository.init();
        } catch (SQLException e) {
            getLogger().severe("Failed to initialize database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        boolean asyncWrites = getConfig().getBoolean("storage.async-writes", true);
        boolean escrowRequired = getConfig().getBoolean("economy.escrow-required", true);
        boolean debug = getConfig().getBoolean("logging.debug", false);
        long companyCreationCost = getConfig().getLong("company.creation-cost", 0L);
        long cityCreationCost = getConfig().getLong("city.creation-cost", 0L);
        AuditService auditService = new AuditService(auditLogRepository, debug);
        jobService = new JobService(this, jobRepository, coinService, auditService, escrowRequired, asyncWrites);
        marketService = new MarketService(this, listingRepository, coinService, auditService, asyncWrites);
        companyService = new CompanyService(companyRepository, auditService, coinService, companyCreationCost);
        cityService = new CityService(cityRepository, auditService, coinService, cityCreationCost);
        jobCreationManager = new JobCreationManager();

        jobService.loadCache();
        marketService.loadCache();

        JobBoardGUI jobBoardGUI = new JobBoardGUI(jobService, messages, coinService, getConfig().getInt("gui.job-board-size", 54));
        MarketGUI marketGUI = new MarketGUI(marketService, coinService, getConfig().getInt("gui.market-size", 54));

        getCommand("jobs").setExecutor(new JobsCommand(jobBoardGUI, jobService, jobCreationManager, messages, coinService));
        getCommand("market").setExecutor(new MarketCommand(marketGUI, marketService, coinService, messages));
        getCommand("company").setExecutor(new CompanyCommand(companyService, messages));
        getCommand("city").setExecutor(new CityCommand(cityService, messages, getConfig().getDouble("economy.tax-default-percent", 5.0)));
        CoinsCommand coinsCommand = new CoinsCommand(coinService, messages);
        getCommand("coins").setExecutor(coinsCommand);
        getCommand("coins").setTabCompleter(coinsCommand);

        Bukkit.getPluginManager().registerEvents(new CoinAccountListener(coinService), this);
        Bukkit.getPluginManager().registerEvents(new InventoryListener(jobBoardGUI, marketGUI, jobService, marketService, coinService, messages), this);
        Bukkit.getPluginManager().registerEvents(new JobCreationListener(jobCreationManager, jobService, messages, coinService), this);

        // Ensure balances are available for players that are online during /reload.
        Bukkit.getOnlinePlayers().forEach(player -> coinService.loadAccount(player.getUniqueId()));

        getLogger().info("ZBenCityJobs enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (coinService != null) {
            coinService.shutdown();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("ZBenCityJobs disabled.");
    }
}
