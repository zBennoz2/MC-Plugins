package com.zbennoz.zbenteleport.listener;

import com.zbennoz.zbenteleport.ZBenTeleportPlugin;
import com.zbennoz.zbenteleport.data.TeleportDatabase;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class BackListener implements Listener {

    private final TeleportDatabase database;
    private final ZBenTeleportPlugin plugin;

    public BackListener(ZBenTeleportPlugin plugin, TeleportDatabase database) {
        this.plugin = plugin;
        this.database = database;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        database.saveLastLocationAsync(player.getUniqueId(), player.getLocation(), "death");
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.UNKNOWN) {
            return;
        }
        Player player = event.getPlayer();
        database.saveLastLocationAsync(player.getUniqueId(), event.getFrom(), "teleport");
    }
}
