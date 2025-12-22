package com.zbennoz.zbenadmintool.gui;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import com.zbennoz.zbenadmintool.permission.PermissionResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class AdminMenuListener implements Listener {

    private static final NamespacedKey BUTTON_KEY = NamespacedKey.minecraft("admintool_button");
    private final ZBenAdmintool plugin;

    public AdminMenuListener(ZBenAdmintool plugin) {
        this.plugin = plugin;
    }

    public static void openMenu(ZBenAdmintool plugin, Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "Admin-Tool");
        inv.setItem(10, button(Material.BEACON, "Admin-Mode", "adminmode"));
        inv.setItem(11, button(Material.ENDER_EYE, "Vanish", "vanish"));
        inv.setItem(12, button(Material.DIAMOND_SWORD, "Gamemode 1", "gm1"));
        inv.setItem(13, button(Material.GRASS_BLOCK, "Gamemode 0", "gm0"));
        inv.setItem(14, button(Material.CHEST, "Offline Inventar", "offinv"));
        inv.setItem(15, button(Material.ENDER_CHEST, "Offline Enderchest", "offec"));
        inv.setItem(16, button(Material.NAME_TAG, "Ränge", "rank"));
        player.openInventory(inv);
    }

    private static ItemStack button(Material material, String name, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.getPersistentDataContainer().set(BUTTON_KEY, PersistentDataType.STRING, action);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getInventory().getHolder() != null) return;
        if (!"Admin-Tool".equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        String action = item.getItemMeta().getPersistentDataContainer().get(BUTTON_KEY, PersistentDataType.STRING);
        if (action == null) return;
        PermissionResolver resolver = plugin.getPermissionResolver();
        switch (action) {
            case "adminmode" -> {
                if (resolver.has(player, "zbenadmintool.adminmode")) {
                    plugin.getAdminModeManager().toggle(player);
                }
            }
            case "vanish" -> {
                if (resolver.has(player, "zbenadmintool.vanish")) {
                    plugin.getVanishManager().toggle(player);
                }
            }
            case "gm1" -> {
                player.setGameMode(org.bukkit.GameMode.CREATIVE);
            }
            case "gm0" -> {
                player.setGameMode(org.bukkit.GameMode.SURVIVAL);
            }
            case "offinv" -> player.performCommand("offinv " + player.getName());
            case "offec" -> player.performCommand("offec " + player.getName());
            case "rank" -> player.performCommand("rank list");
            default -> {
            }
        }
    }
}
