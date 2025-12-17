package com.zbennoz.zbenteleport;

import com.zbennoz.zbenteleport.command.BackCommand;
import com.zbennoz.zbenteleport.command.HomeCommand;
import com.zbennoz.zbenteleport.command.HomesCommand;
import com.zbennoz.zbenteleport.command.TpaCommand;
import com.zbennoz.zbenteleport.command.TpaResponseCommand;
import com.zbennoz.zbenteleport.data.CooldownManager;
import com.zbennoz.zbenteleport.data.TeleportDatabase;
import com.zbennoz.zbenteleport.listener.BackListener;
import com.zbennoz.zbenteleport.util.HomeManager;
import com.zbennoz.zbenteleport.util.TpaManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public class ZBenTeleportPlugin extends JavaPlugin {

    private TeleportDatabase database;
    private HomeManager homeManager;
    private TpaManager tpaManager;
    private CooldownManager cooldownManager;
    private MiniMessage miniMessage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        miniMessage = MiniMessage.miniMessage();
        database = new TeleportDatabase(new File(getDataFolder(), "data.db"));
        database.init();

        homeManager = new HomeManager(this, database);
        tpaManager = new TpaManager(this);
        cooldownManager = new CooldownManager(this);

        registerCommands();
        getServer().getPluginManager().registerEvents(new BackListener(this, database), this);
        getLogger().info("ZBenTeleport enabled");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.shutdown();
        }
    }

    private void registerCommands() {
        register("tpa", new TpaCommand(this, tpaManager));
        register("tpaccept", new TpaResponseCommand(this, tpaManager, true));
        register("tpdeny", new TpaResponseCommand(this, tpaManager, false));
        register("tpacancel", new TpaResponseCommand(this, tpaManager, null));
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
}
