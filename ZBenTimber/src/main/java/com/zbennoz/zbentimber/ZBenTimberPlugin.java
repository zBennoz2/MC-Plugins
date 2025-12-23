package com.zbennoz.zbentimber;

import com.zbennoz.zbentimber.command.TimberCommand;
import com.zbennoz.zbentimber.listener.TimberListener;
import org.bukkit.plugin.java.JavaPlugin;

public class ZBenTimberPlugin extends JavaPlugin {
    private MessageService messages;
    private PlayerSettingsStorage storage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        this.messages = new MessageService(this);
        this.storage = new PlayerSettingsStorage(this);

        TimberListener listener = new TimberListener(this, storage);
        getServer().getPluginManager().registerEvents(listener, this);

        TimberCommand command = new TimberCommand(this, storage);
        getCommand("timber").setExecutor(command);
        getCommand("timber").setTabCompleter(command);
    }

    public boolean getLeavesDefault() {
        return getConfig().getBoolean("break-leaves-default", true);
    }

    public void reloadConfiguration() {
        reloadConfig();
        this.messages = new MessageService(this);
    }

    public MessageService getMessages() {
        return messages;
    }
}
