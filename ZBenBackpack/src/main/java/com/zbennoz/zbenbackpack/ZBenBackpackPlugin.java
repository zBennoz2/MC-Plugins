package com.zbennoz.zbenbackpack;

import com.zbennoz.zbenbackpack.api.BackpackService;
import com.zbennoz.zbenbackpack.command.BackpackCommand;
import com.zbennoz.zbenbackpack.data.BackpackDatabase;
import com.zbennoz.zbenbackpack.listener.BackpackListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public class ZBenBackpackPlugin extends JavaPlugin {

    private BackpackDatabase database;
    private BackpackService backpackService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        database = new BackpackDatabase(new File(getDataFolder(), "backpacks.db"));
        database.init();
        backpackService = new BackpackService(this, database);
        BackpackListener listener = new BackpackListener(backpackService);
        getServer().getPluginManager().registerEvents(listener, this);
        PluginCommand command = Objects.requireNonNull(getCommand("backpack"));
        BackpackCommand backpackCommand = new BackpackCommand(backpackService);
        command.setExecutor(backpackCommand);
        getServer().getPluginManager().registerEvents(backpackCommand, this);
    }

    @Override
    public void onDisable() {
        if (database != null) database.shutdown();
    }

    public BackpackService getBackpackService() {
        return backpackService;
    }

    public void applyBackpackSize(java.util.UUID playerId, int newSize) {
        if (backpackService != null) {
            backpackService.applyBackpackSize(playerId, newSize);
        }
    }
}
