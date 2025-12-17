package com.zbennoz.zbenambient;

import com.zbennoz.zbenambient.task.AmbientTask;
import org.bukkit.plugin.java.JavaPlugin;

public class ZBenAmbientPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getScheduler().runTaskTimer(this, new AmbientTask(this), 20L, 20L);
    }
}
