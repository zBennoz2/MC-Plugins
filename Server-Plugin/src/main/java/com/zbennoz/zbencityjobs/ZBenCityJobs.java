package com.zbennoz.zbencityjobs;

import com.zbennoz.zbencityjobs.commands.CityCommand;
import com.zbennoz.zbencityjobs.commands.CompanyCommand;
import com.zbennoz.zbencityjobs.commands.JobsCommand;
import com.zbennoz.zbencityjobs.commands.MarketCommand;
import com.zbennoz.zbencityjobs.gui.JobBoardGUI;
import com.zbennoz.zbencityjobs.gui.MarketGUI;
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
    private EconomyService economyService;
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
        economyService = new EconomyService();
        if (!economyService.setup()) {
            getLogger().warning("Vault not found - economy features disabled");
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
        AuditService auditService = new AuditService(auditLogRepository, debug);
        jobService = new JobService(this, jobRepository, economyService, auditService, escrowRequired, asyncWrites);
        marketService = new MarketService(this, listingRepository, economyService, auditService, asyncWrites);
        companyService = new CompanyService(companyRepository, auditService);
        cityService = new CityService(cityRepository, auditService);
        jobCreationManager = new JobCreationManager();

        jobService.loadCache();
        marketService.loadCache();

        JobBoardGUI jobBoardGUI = new JobBoardGUI(jobService, messages, getConfig().getInt("gui.job-board-size", 54));
        MarketGUI marketGUI = new MarketGUI(marketService, getConfig().getInt("gui.market-size", 54));

        getCommand("jobs").setExecutor(new JobsCommand(jobBoardGUI, jobService, jobCreationManager, messages));
        getCommand("market").setExecutor(new MarketCommand(marketGUI, marketService, messages));
        getCommand("company").setExecutor(new CompanyCommand(companyService, messages));
        getCommand("city").setExecutor(new CityCommand(cityService, messages, getConfig().getDouble("economy.tax-default-percent", 5.0)));

        Bukkit.getPluginManager().registerEvents(new InventoryListener(jobBoardGUI, marketGUI, jobService, marketService, messages), this);
        Bukkit.getPluginManager().registerEvents(new JobCreationListener(jobCreationManager, jobService, messages), this);

        getLogger().info("ZBenCityJobs enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("ZBenCityJobs disabled.");
    }
}
