package com.zbennoz.zbenbackpack;

import com.zbennoz.zbenbackpack.command.BackpackCommand;
import com.zbennoz.zbenbackpack.data.BackpackDatabase;
import com.zbennoz.zbenbackpack.listener.BackpackListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public class ZBenBackpackPlugin extends JavaPlugin {

    private BackpackDatabase database;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        database = new BackpackDatabase(new File(getDataFolder(), "backpacks.db"));
        database.init();
        BackpackListener listener = new BackpackListener(this, database);
        getServer().getPluginManager().registerEvents(listener, this);
        PluginCommand command = Objects.requireNonNull(getCommand("backpack"));
        BackpackCommand backpackCommand = new BackpackCommand(this, database);
        command.setExecutor(backpackCommand);
        getServer().getPluginManager().registerEvents(backpackCommand, this);
    }

    @Override
    public void onDisable() {
        if (database != null) database.shutdown();
    }
}
