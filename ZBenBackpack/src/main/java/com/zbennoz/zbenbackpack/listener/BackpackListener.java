package com.zbennoz.zbenbackpack.listener;

import com.zbennoz.zbenbackpack.api.BackpackService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;

public class BackpackListener implements Listener {

    private final BackpackService service;

    public BackpackListener(BackpackService service) {
        this.service = service;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (service.isBackpackView(event.getView())) {
            service.saveBackpack((org.bukkit.entity.Player) event.getPlayer());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getOldCursor() != null && event.getCursor() != null && event.getOldCursor().isSimilar(event.getCursor())) {
            // prevent nesting not fully implemented
        }
    }

    @EventHandler
    public void onDamage(PlayerItemDamageEvent event) {
        if (service.isBackpack(event.getItem())) {
            event.setCancelled(true);
        }
    }
}
