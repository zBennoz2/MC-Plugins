package com.zbennoz.zbencore.listeners;

import com.zbennoz.zbencore.branding.BrandingService;
import com.zbennoz.zbencore.teams.TeamService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class PlayerJoinListener implements Listener {

    private final JavaPlugin plugin;
    private final BrandingService branding;
    private final TeamService teamService;

    public PlayerJoinListener(JavaPlugin plugin, BrandingService branding, TeamService teamService) {
        this.plugin = plugin;
        this.branding = branding;
        this.teamService = teamService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();

        // Tablist branding
        branding.applyTablist(p);
        teamService.applyTeamDecorations(p);

        // Optional join hint
        var cfg = plugin.getConfig();
        if (!cfg.getBoolean("features.joinHint.enabled", true)) return;

        boolean firstJoinOnly = cfg.getBoolean("features.joinHint.firstJoinOnly", true);
        if (firstJoinOnly && p.hasPlayedBefore()) return;

        int duration = cfg.getInt("features.joinHint.durationSeconds", 5);
        String msg = branding.joinHint();

        if (duration <= 0) {
            p.sendMessage(msg);
            return;
        }

        // Actionbar for X seconds, then stop (properly cancels the repeating task)
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (p.isOnline()) p.sendActionBar(msg);
        }, 0L, 20L);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            task.cancel();
            if (p.isOnline()) p.sendActionBar("");
        }, (long) duration * 20L);
    }
}
