package com.zbennoz.zbencore;

import com.zbennoz.zbencore.branding.BrandingService;
import com.zbennoz.zbencore.commands.AboutCommand;
import com.zbennoz.zbencore.commands.TeamCommand;
import com.zbennoz.zbencore.commands.ZBenCommand;
import com.zbennoz.zbencore.listeners.PlayerJoinListener;
import com.zbennoz.zbencore.listeners.TeamChatListener;
import com.zbennoz.zbencore.teams.TeamConversationManager;
import com.zbennoz.zbencore.teams.TeamService;
import org.bukkit.plugin.java.JavaPlugin;

public final class ZBenCore extends JavaPlugin {

    private BrandingService brandingService;
    private TeamService teamService;
    private TeamConversationManager teamConversations;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.brandingService = new BrandingService(this);
        this.teamService = new TeamService(this);
        this.teamService.load();
        this.teamConversations = new TeamConversationManager(this, teamService);

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

        var teamCmd = getCommand("team");
        if (teamCmd != null) {
            var tc = new TeamCommand(this, teamService, teamConversations);
            teamCmd.setExecutor(tc);
            teamCmd.setTabCompleter(tc);
        }

        // Listener
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, brandingService, teamService), this);
        getServer().getPluginManager().registerEvents(teamConversations, this);
        getServer().getPluginManager().registerEvents(new TeamChatListener(teamService), this);

        // Apply tablist to online players on reload
        if (getConfig().getBoolean("features.tablistBranding", true)) {
            getServer().getOnlinePlayers().forEach(player -> {
                brandingService.applyTablist(player);
                teamService.applyTeamDecorations(player);
            });
        }

        getLogger().info("ZBenCore enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ZBenCore disabled.");
    }
}
