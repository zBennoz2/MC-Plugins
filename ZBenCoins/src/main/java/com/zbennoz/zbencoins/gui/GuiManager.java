package com.zbennoz.zbencoins.gui;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verwalten und schützen von GUIs.
 */
public class GuiManager implements Listener {

    private final Map<UUID, ManagedGui> openGuis = new ConcurrentHashMap<>();

    public void openGui(Player player, ManagedGui gui) {
        openGuis.put(player.getUniqueId(), gui);
        player.openInventory(gui.getInventory());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ManagedGui gui = openGuis.get(player.getUniqueId());
        if (gui == null || event.getInventory().getHolder() != gui) {
            return;
        }
        event.setCancelled(true);
        gui.handleClick(event);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        openGuis.remove(event.getPlayer().getUniqueId());
    }
}
