package com.zbennoz.zbenenchants;

import com.zbennoz.zbenenchants.listener.EnchantListener;
import org.bukkit.plugin.java.JavaPlugin;

public class ZBenEnchantsPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new EnchantListener(this), this);
    }
}
