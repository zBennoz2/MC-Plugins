package com.zbennoz.zbencoins.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;

/**
 * Verwalten und schützen von GUIs.
 */
public class GuiManager implements Listener {

    public void openGui(Player player, ManagedGui gui) {
        player.openInventory(gui.getInventory());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ManagedGui gui = resolveGui(event);
        if (gui == null) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            gui.handleClick(event);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        ManagedGui gui = resolveGui(event);
        if (gui == null) {
            return;
        }
        if (event.getRawSlots().stream().anyMatch(raw -> raw < event.getView().getTopInventory().getSize())) {
            event.setCancelled(true);
        }
    }

    private ManagedGui resolveGui(InventoryInteractEvent event) {
        InventoryView view = event.getView();
        InventoryHolder holder = view.getTopInventory().getHolder();
        if (holder instanceof ManagedGui gui) {
            return gui;
        }
        return null;
    }
}
