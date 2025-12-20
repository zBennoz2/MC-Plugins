package com.zbennoz.zbencore.listeners;

import com.zbennoz.zbencore.teams.Team;
import com.zbennoz.zbencore.teams.TeamService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class TeamChatListener implements Listener {

    private final TeamService teamService;

    public TeamChatListener(TeamService teamService) {
        this.teamService = teamService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Team team = teamService.getTeamFor(event.getPlayer().getUniqueId());
        if (team == null) return;

        teamService.applyTeamDecorations(event.getPlayer());
        event.setFormat("%1$s §7» §f%2$s");
    }
}
