package com.zbennoz.zbentimber;

import com.zbennoz.zbentimber.listener.TimberListener;
import org.bukkit.plugin.java.JavaPlugin;

public class ZBenTimberPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new TimberListener(this), this);
    }
}
