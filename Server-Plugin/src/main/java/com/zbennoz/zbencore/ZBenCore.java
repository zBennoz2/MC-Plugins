package com.zbennoz.zbencore;

import com.zbennoz.zbencore.branding.BrandingService;
import com.zbennoz.zbencore.commands.AboutCommand;
import com.zbennoz.zbencore.commands.ZBenCommand;
import com.zbennoz.zbencore.listeners.PlayerJoinListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class ZBenCore extends JavaPlugin {

    private BrandingService brandingService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.brandingService = new BrandingService(this);

        // Commands
        var aboutCmd = getCommand("about");
        if (aboutCmd != null) {
            aboutCmd.setExecutor(new AboutCommand(this, brandingService));
        }

        var zbenCmd = getCommand("zben");
        if (zbenCmd != null) {
            var zben = new ZBenCommand(this);
            zbenCmd.setExecutor(zben);
            zbenCmd.setTabCompleter(zben);
        }

        // Listener
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, brandingService), this);

        // Apply tablist to online players on reload
        if (getConfig().getBoolean("features.tablistBranding", true)) {
            getServer().getOnlinePlayers().forEach(brandingService::applyTablist);
        }

        getLogger().info("ZBenCore enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ZBenCore disabled.");
    }
}
