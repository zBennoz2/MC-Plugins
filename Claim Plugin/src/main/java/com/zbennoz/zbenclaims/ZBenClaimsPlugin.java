package com.zbennoz.zbenclaims;

import com.zbennoz.zbenclaims.commands.*;
import com.zbennoz.zbenclaims.db.Database;
import com.zbennoz.zbenclaims.db.SQLiteDatabase;
import com.zbennoz.zbenclaims.listeners.ProtectionListener;
import com.zbennoz.zbenclaims.ranks.RankManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class ZBenClaimsPlugin extends JavaPlugin {

    private Database database;
    private ClaimService claimService;
    private MessageService messages;
    private RankManager rankManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.messages = new MessageService(this);
        this.database = new SQLiteDatabase(this);
        this.database.init();

        this.rankManager = new RankManager(this, database);

        this.claimService = new ClaimService(this, database, messages, rankManager);
        this.claimService.loadCache();

        registerCommands();
        registerListeners();

        getServer().getOnlinePlayers().forEach(rankManager::applyVisuals);
        getLogger().info("ZBenClaims enabled.");
    }

    @Override
    public void onDisable() {
        if (claimService != null) claimService.shutdown();
        if (database != null) database.close();
    }

    public Database getDatabase() { return database; }
    public ClaimService getClaimService() { return claimService; }
    public MessageService getMessages() { return messages; }
    public RankManager getRankManager() { return rankManager; }

    public void reloadAll() {
        reloadConfig();
        messages.reload();
        rankManager.reload();
        claimService.reload();
    }

    private void registerCommands() {
        setExec("claim", new ClaimCommand(this));
        setExec("unclaim", new UnclaimCommand(this));
        setExec("trust", new TrustCommand(this));
        setExec("untrust", new UntrustCommand(this));
        setExec("claims", new ClaimsCommand(this));
        setExec("zbenclaims", new AdminCommand(this));
        setExec("rank", new RankCommand(this));
    }

    private void setExec(String name, Object executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) {
            getLogger().warning("Command missing in plugin.yml: " + name);
            return;
        }
        if (executor instanceof org.bukkit.command.CommandExecutor ce) cmd.setExecutor(ce);
        if (executor instanceof org.bukkit.command.TabCompleter tc) cmd.setTabCompleter(tc);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(rankManager, this);
    }
}
