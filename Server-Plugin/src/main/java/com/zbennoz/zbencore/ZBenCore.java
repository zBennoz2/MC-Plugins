package com.zbennoz.zbencore;

import com.zbennoz.zbencore.branding.BrandingService;
import com.zbennoz.zbencore.commands.AboutCommand;
import com.zbennoz.zbencore.commands.RankCommand;
import com.zbennoz.zbencore.commands.ZBenCommand;
import com.zbennoz.zbencore.listeners.PlayerJoinListener;
import com.zbennoz.zbencore.ranks.RankConversationManager;
import com.zbennoz.zbencore.ranks.RankService;
import org.bukkit.plugin.java.JavaPlugin;

public final class ZBenCore extends JavaPlugin {

    private BrandingService brandingService;
    private RankService rankService;
    private RankConversationManager rankConversations;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.brandingService = new BrandingService(this);
        this.rankService = new RankService(this);
        this.rankService.load();
        this.rankConversations = new RankConversationManager(this, rankService);

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

        var rankCmd = getCommand("rank");
        if (rankCmd != null) {
            var rc = new RankCommand(this, rankService, rankConversations);
            rankCmd.setExecutor(rc);
            rankCmd.setTabCompleter(rc);
        }

        // Listener
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, brandingService), this);
        getServer().getPluginManager().registerEvents(rankConversations, this);

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
