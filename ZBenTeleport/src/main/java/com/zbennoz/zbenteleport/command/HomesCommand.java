package com.zbennoz.zbenteleport.command;

import com.zbennoz.zbenteleport.ZBenTeleportPlugin;
import com.zbennoz.zbenteleport.util.HomeManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Map;

public class HomesCommand implements CommandExecutor, Listener {

    private final ZBenTeleportPlugin plugin;
    private final HomeManager homeManager;

    public HomesCommand(ZBenTeleportPlugin plugin, HomeManager homeManager) {
        this.plugin = plugin;
        this.homeManager = homeManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }
        Map<String, org.bukkit.Location> homes = homeManager.loadHomes(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(player, Math.max(9, ((homes.size() / 9) + 1) * 9), Component.text("Homes"));
        homes.forEach((name, location) -> {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(name));
            meta.lore(java.util.List.of(Component.text(location.getWorld().getName()), Component.text(location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ())));
            item.setItemMeta(meta);
            inventory.addItem(item);
        });
        player.openInventory(inventory);
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!Component.text("Homes").equals(event.getView().title())) return;
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        String name = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        var location = homeManager.loadHomes(player.getUniqueId()).get(name);
        if (location == null) return;
        if (event.getClick() == ClickType.SHIFT_RIGHT) {
            homeManager.deleteHome(player.getUniqueId(), name);
            player.sendMessage(Component.text("Home deleted."));
            player.closeInventory();
            return;
        }
        player.teleportAsync(location);
        plugin.database().saveLastLocationAsync(player.getUniqueId(), player.getLocation(), "teleport");
        player.sendMessage(Component.text("Teleported to home."));
        player.closeInventory();
    }
}
