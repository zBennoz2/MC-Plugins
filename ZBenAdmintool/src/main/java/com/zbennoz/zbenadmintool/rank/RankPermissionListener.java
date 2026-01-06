package com.zbennoz.zbenadmintool.rank;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class RankPermissionListener implements Listener {

    private final ZBenAdmintool plugin;

    public RankPermissionListener(ZBenAdmintool plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getRankManager().ensureDefaultRank(event.getPlayer().getUniqueId());
        plugin.getRankPermissionBridge().applyPermissions(event.getPlayer());
        plugin.getRankManager().refreshPlayerTeam(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getRankPermissionBridge().clear(event.getPlayer());
    }
}
