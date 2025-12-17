package com.zbennoz.zbenbackpack.listener;

import com.zbennoz.zbenbackpack.ZBenBackpackPlugin;
import com.zbennoz.zbenbackpack.command.BackpackCommand;
import com.zbennoz.zbenbackpack.data.BackpackDatabase;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;

public class BackpackListener implements Listener {

    private final ZBenBackpackPlugin plugin;
    private final BackpackDatabase database;

    public BackpackListener(ZBenBackpackPlugin plugin, BackpackDatabase database) {
        this.plugin = plugin;
        this.database = database;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getView().title().equals(Component.text("Backpack (" + event.getInventory().getSize() + ")"))) {
            new BackpackCommand(plugin, database).save((org.bukkit.entity.Player) event.getPlayer());
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
        // Backpacks shouldn't take durability; marker only
        if (event.getItem().getItemMeta() != null && event.getItem().getItemMeta().getPersistentDataContainer().has(new org.bukkit.NamespacedKey(plugin, "backpack"))) {
            event.setCancelled(true);
        }
    }
}
