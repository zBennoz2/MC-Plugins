package com.zbennoz.zbenteleport;

import com.zbennoz.zbenteleport.command.*;
import com.zbennoz.zbenteleport.data.CooldownManager;
import com.zbennoz.zbenteleport.data.TeleportDatabase;
import com.zbennoz.zbenteleport.listener.BackListener;
import com.zbennoz.zbenteleport.util.HomeManager;
import com.zbennoz.zbenteleport.util.MessageService;
import com.zbennoz.zbenteleport.util.PlayerSettingsManager;
import com.zbennoz.zbenteleport.util.TpaManager;
import com.zbennoz.zbenteleport.util.WarpManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.Objects;

public class ZBenTeleportPlugin extends JavaPlugin {

    private TeleportDatabase database;
    private HomeManager homeManager;
    private TpaManager tpaManager;
    private CooldownManager cooldownManager;
    private PlayerSettingsManager playerSettingsManager;
    private MessageService messageService;
    private WarpManager warpManager;
    private MiniMessage miniMessage;
    private BukkitTask tpaCleanup;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        miniMessage = MiniMessage.miniMessage();
        messageService = new MessageService(this, miniMessage);
        messageService.reload();
        database = new TeleportDatabase(new File(getDataFolder(), "data.db"));
        database.init();

        homeManager = new HomeManager(this, database);
        tpaManager = new TpaManager(this);
        cooldownManager = new CooldownManager(this);
        playerSettingsManager = new PlayerSettingsManager(database);
        warpManager = new WarpManager(database);

        registerCommands();
        getServer().getPluginManager().registerEvents(new BackListener(this, database), this);
        tpaCleanup = getServer().getScheduler().runTaskTimer(this, tpaManager::expireRequests, 20L, 20L);
        getLogger().info("ZBenTeleport enabled");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.shutdown();
        }
        if (tpaCleanup != null) {
            tpaCleanup.cancel();
        }
    }

    private void registerCommands() {
        register("tpa", new TpaCommand(this, tpaManager));
        register("tpaccept", new TpaResponseCommand(this, tpaManager, true));
        register("tpdeny", new TpaResponseCommand(this, tpaManager, false));
        register("tpacancel", new TpaResponseCommand(this, tpaManager, null));
        register("tptoggle", new TptoggleCommand(this, playerSettingsManager));
        register("tpblock", new TpBlockCommand(this, playerSettingsManager, TpBlockCommand.Mode.BLOCK));
        register("tpunblock", new TpBlockCommand(this, playerSettingsManager, TpBlockCommand.Mode.UNBLOCK));
        register("tpblocklist", new TpBlockCommand(this, playerSettingsManager, TpBlockCommand.Mode.LIST));
        register("tphere", new TphereCommand(this, tpaManager));
        register("rtp", new RtpCommand(this));
        register("setwarp", new WarpCommand(this, warpManager, WarpCommand.Mode.SET));
        register("delwarp", new WarpCommand(this, warpManager, WarpCommand.Mode.DELETE));
        register("warp", new WarpCommand(this, warpManager, WarpCommand.Mode.TELEPORT));
        register("warps", new WarpCommand(this, warpManager, WarpCommand.Mode.LIST));
        register("sethome", new HomeCommand(this, homeManager, HomeCommand.Mode.SET));
        register("home", new HomeCommand(this, homeManager, HomeCommand.Mode.TELEPORT));
        register("delhome", new HomeCommand(this, homeManager, HomeCommand.Mode.DELETE));
        register("homes", new HomesCommand(this, homeManager));
        register("back", new BackCommand(this, database));
    }

    private void register(String name, Object executor) {
        PluginCommand command = Objects.requireNonNull(getCommand(name), name + " missing from plugin.yml");
        command.setExecutor((org.bukkit.command.CommandExecutor) executor);
        if (executor instanceof org.bukkit.command.TabCompleter tabCompleter) {
            command.setTabCompleter(tabCompleter);
        }
    }

    public TeleportDatabase database() {
        return database;
    }

    public HomeManager homeManager() {
        return homeManager;
    }

    public TpaManager tpaManager() {
        return tpaManager;
    }

    public CooldownManager cooldownManager() {
        return cooldownManager;
    }

    public MiniMessage miniMessage() {
        return miniMessage;
    }

    public MessageService messages() {
        return messageService;
    }

    public PlayerSettingsManager playerSettingsManager() {
        return playerSettingsManager;
    }

    public WarpManager warpManager() {
        return warpManager;
    }
}
