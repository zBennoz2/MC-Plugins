package com.zbennoz.zbenadmintool;

import com.zbennoz.zbenadmintool.command.AdminCommand;
import com.zbennoz.zbenadmintool.command.AdminModeCommand;
import com.zbennoz.zbenadmintool.command.BanCommand;
import com.zbennoz.zbenadmintool.command.InspectCommand;
import com.zbennoz.zbenadmintool.command.KickCommand;
import com.zbennoz.zbenadmintool.command.LogsCommand;
import com.zbennoz.zbenadmintool.command.MuteCommand;
import com.zbennoz.zbenadmintool.command.OfflineEnderCommand;
import com.zbennoz.zbenadmintool.command.OfflineInventoryCommand;
import com.zbennoz.zbenadmintool.command.RankCommand;
import com.zbennoz.zbenadmintool.command.VanishCommand;
import com.zbennoz.zbenadmintool.command.WarnCommand;
import com.zbennoz.zbenadmintool.gui.AdminMenuListener;
import com.zbennoz.zbenadmintool.gui.ChatInputListener;
import com.zbennoz.zbenadmintool.gui.ObserveGui;
import com.zbennoz.zbenadmintool.gui.SuspiciousActivityGui;
import com.zbennoz.zbenadmintool.hook.ProtocolLibHook;
import com.zbennoz.zbenadmintool.logging.LogManager;
import com.zbennoz.zbenadmintool.logging.InspectorListener;
import com.zbennoz.zbenadmintool.permission.PermissionResolver;
import com.zbennoz.zbenadmintool.player.AdminModeManager;
import com.zbennoz.zbenadmintool.player.TabBrandingListener;
import com.zbennoz.zbenadmintool.player.VanishManager;
import com.zbennoz.zbenadmintool.rank.RankManager;
import com.zbennoz.zbenadmintool.rank.RankPermissionBridge;
import com.zbennoz.zbenadmintool.rank.RankPermissionListener;
import com.zbennoz.zbenadmintool.service.OreVisionService;
import com.zbennoz.zbenadmintool.service.SuspiciousMiningService;
import com.zbennoz.zbenadmintool.service.TeleportLogService;
import com.zbennoz.zbenadmintool.storage.Database;
import com.zbennoz.zbenadmintool.text.MessageService;
import com.zbennoz.zbenadmintool.util.BackpackIntegration;
import com.zbennoz.zbenadmintool.util.ModerationService;
import com.zbennoz.zbenadmintool.util.OfflineInventoryService;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class ZBenAdmintool extends JavaPlugin {

    private MessageService messages;
    private Database database;
    private RankManager rankManager;
    private RankPermissionBridge rankPermissionBridge;
    private PermissionResolver permissionResolver;
    private VanishManager vanishManager;
    private AdminModeManager adminModeManager;
    private LogManager logManager;
    private OfflineInventoryService offlineInventoryService;
    private InspectorListener inspectorListener;
    private ChatInputListener chatInputListener;
    private BackpackIntegration backpackIntegration;
    private ModerationService moderationService;
    private SuspiciousMiningService suspiciousMiningService;
    private SuspiciousActivityGui suspiciousActivityGui;
    private ObserveGui observeGui;
    private OreVisionService oreVisionService;
    private ProtocolLibHook protocolLibHook;
    private TeleportLogService teleportLogService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        this.messages = new MessageService(this);
        this.database = new Database(this);
        this.rankManager = new RankManager(this, database);
        this.rankPermissionBridge = new RankPermissionBridge(this, rankManager);
        this.rankManager.setPermissionBridge(rankPermissionBridge);
        this.permissionResolver = new PermissionResolver(rankManager);
        this.vanishManager = new VanishManager(this, permissionResolver);
        this.adminModeManager = new AdminModeManager(this, vanishManager, messages);
        this.logManager = new LogManager(this, messages);
        this.offlineInventoryService = new OfflineInventoryService(this, messages);
        this.inspectorListener = new InspectorListener(this, logManager);
        this.chatInputListener = new ChatInputListener(this);
        this.backpackIntegration = new BackpackIntegration(this);
        this.moderationService = new ModerationService(this);
        this.protocolLibHook = new ProtocolLibHook(this);
        this.teleportLogService = new TeleportLogService(this);
        this.suspiciousMiningService = new SuspiciousMiningService(this);
        this.suspiciousActivityGui = new SuspiciousActivityGui(this, suspiciousMiningService);
        this.observeGui = new ObserveGui(this, teleportLogService);
        this.oreVisionService = new OreVisionService(this, protocolLibHook);

        database.initSchema();
        rankManager.init();

        registerCommands();
        registerListeners();

        Bukkit.getOnlinePlayers().forEach(player -> {
            TabBrandingListener.applyBranding(this, player);
            vanishManager.refreshVisibility(player);
            rankManager.refreshPlayerTeam(player);
            rankPermissionBridge.applyPermissions(player);
            if (rankManager.getPlayerRank(player) != null) {
                backpackIntegration.applyBackpackSize(player.getUniqueId(), rankManager.getPlayerRank(player).getBackpackSlots());
            }
        });
    }

    private void registerCommands() {
        getCommand("admin").setExecutor(new AdminCommand(this));
        getCommand("admintool").setExecutor(new AdminCommand(this));
        getCommand("adminmode").setExecutor(new AdminModeCommand(this));
        getCommand("vanish").setExecutor(new VanishCommand(this));
        RankCommand rankCommand = new RankCommand(this);
        getCommand("rank").setExecutor(rankCommand);
        getCommand("rank").setTabCompleter(rankCommand);
        getCommand("inspect").setExecutor(new InspectCommand(this));
        getCommand("logs").setExecutor(new LogsCommand(this));
        getCommand("offinv").setExecutor(new OfflineInventoryCommand(this));
        getCommand("offec").setExecutor(new OfflineEnderCommand(this));
        getCommand("ban").setExecutor(new BanCommand(this));
        getCommand("kick").setExecutor(new KickCommand(this));
        getCommand("mute").setExecutor(new MuteCommand(this));
        getCommand("warn").setExecutor(new WarnCommand(this));
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new TabBrandingListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AdminMenuListener(this), this);
        Bukkit.getPluginManager().registerEvents(inspectorListener, this);
        Bukkit.getPluginManager().registerEvents(chatInputListener, this);
        Bukkit.getPluginManager().registerEvents(new RankPermissionListener(this), this);
        Bukkit.getPluginManager().registerEvents(offlineInventoryService, this);
        Bukkit.getPluginManager().registerEvents(moderationService, this);
        Bukkit.getPluginManager().registerEvents(suspiciousMiningService, this);
        Bukkit.getPluginManager().registerEvents(suspiciousActivityGui, this);
        Bukkit.getPluginManager().registerEvents(observeGui, this);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        adminModeManager.disableAll();
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (oreVisionService != null) {
                oreVisionService.clear(player);
            }
        });
        Bukkit.getOnlinePlayers().forEach(rankPermissionBridge::clear);
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

    public RankPermissionBridge getRankPermissionBridge() {
        return rankPermissionBridge;
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

    public ChatInputListener getChatInputListener() {
        return chatInputListener;
    }

    public BackpackIntegration getBackpackIntegration() {
        return backpackIntegration;
    }

    public ModerationService getModerationService() {
        return moderationService;
    }

    public SuspiciousMiningService getSuspiciousMiningService() {
        return suspiciousMiningService;
    }

    public SuspiciousActivityGui getSuspiciousActivityGui() {
        return suspiciousActivityGui;
    }

    public ObserveGui getObserveGui() {
        return observeGui;
    }

    public OreVisionService getOreVisionService() {
        return oreVisionService;
    }

    public ProtocolLibHook getProtocolLibHook() {
        return protocolLibHook;
    }

    public TeleportLogService getTeleportLogService() {
        return teleportLogService;
    }
}
