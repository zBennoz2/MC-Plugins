package com.zbennoz.zbenadmintool;

import com.zbennoz.zbenadmintool.command.AdminCommand;
import com.zbennoz.zbenadmintool.command.AdminModeCommand;
import com.zbennoz.zbenadmintool.command.LogsCommand;
import com.zbennoz.zbenadmintool.command.OfflineEnderCommand;
import com.zbennoz.zbenadmintool.command.OfflineInventoryCommand;
import com.zbennoz.zbenadmintool.command.RankCommand;
import com.zbennoz.zbenadmintool.command.VanishCommand;
import com.zbennoz.zbenadmintool.command.InspectCommand;
import com.zbennoz.zbenadmintool.gui.AdminMenuListener;
import com.zbennoz.zbenadmintool.logging.LogManager;
import com.zbennoz.zbenadmintool.logging.InspectorListener;
import com.zbennoz.zbenadmintool.permission.PermissionResolver;
import com.zbennoz.zbenadmintool.player.AdminModeManager;
import com.zbennoz.zbenadmintool.player.TabBrandingListener;
import com.zbennoz.zbenadmintool.player.VanishManager;
import com.zbennoz.zbenadmintool.rank.RankManager;
import com.zbennoz.zbenadmintool.storage.Database;
import com.zbennoz.zbenadmintool.text.MessageService;
import com.zbennoz.zbenadmintool.util.OfflineInventoryService;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class ZBenAdmintool extends JavaPlugin {

    private MessageService messages;
    private Database database;
    private RankManager rankManager;
    private PermissionResolver permissionResolver;
    private VanishManager vanishManager;
    private AdminModeManager adminModeManager;
    private LogManager logManager;
    private OfflineInventoryService offlineInventoryService;
    private InspectorListener inspectorListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        this.messages = new MessageService(this);
        this.database = new Database(this);
        this.rankManager = new RankManager(this, database);
        this.permissionResolver = new PermissionResolver(rankManager);
        this.vanishManager = new VanishManager(this, permissionResolver);
        this.adminModeManager = new AdminModeManager(this, vanishManager, messages);
        this.logManager = new LogManager(this, messages);
        this.offlineInventoryService = new OfflineInventoryService(this, messages);
        this.inspectorListener = new InspectorListener(this, logManager);

        database.init();
        rankManager.init();
        logManager.init();

        registerCommands();
        registerListeners();

        Bukkit.getOnlinePlayers().forEach(player -> {
            TabBrandingListener.applyBranding(this, player);
            vanishManager.refreshVisibility(player);
            rankManager.refreshPlayerTeam(player);
        });
    }

    private void registerCommands() {
        getCommand("admin").setExecutor(new AdminCommand(this));
        getCommand("admintool").setExecutor(new AdminCommand(this));
        getCommand("adminmode").setExecutor(new AdminModeCommand(this));
        getCommand("vanish").setExecutor(new VanishCommand(this));
        getCommand("rank").setExecutor(new RankCommand(this));
        getCommand("inspect").setExecutor(new InspectCommand(this));
        getCommand("logs").setExecutor(new LogsCommand(this));
        getCommand("offinv").setExecutor(new OfflineInventoryCommand(this));
        getCommand("offec").setExecutor(new OfflineEnderCommand(this));
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new TabBrandingListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AdminMenuListener(this), this);
        Bukkit.getPluginManager().registerEvents(inspectorListener, this);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        adminModeManager.disableAll();
        database.close();
    }

    public MessageService getMessages() {
        return messages;
    }

    public Database getDatabase() {
        return database;
    }

    public RankManager getRankManager() {
        return rankManager;
    }

    public PermissionResolver getPermissionResolver() {
        return permissionResolver;
    }

    public VanishManager getVanishManager() {
        return vanishManager;
    }

    public AdminModeManager getAdminModeManager() {
        return adminModeManager;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public OfflineInventoryService getOfflineInventoryService() {
        return offlineInventoryService;
    }

    public InspectorListener getInspectorListener() {
        return inspectorListener;
    }
}
