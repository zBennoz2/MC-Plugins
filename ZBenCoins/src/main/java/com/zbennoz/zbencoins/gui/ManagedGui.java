package com.zbennoz.zbencoins.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Basis-Interface für verwaltete GUIs.
 */
public interface ManagedGui extends InventoryHolder {

    Inventory getInventory();

    void handleClick(InventoryClickEvent event);
}
