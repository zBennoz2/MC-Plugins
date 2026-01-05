package com.zbennoz.zbenadmintool.gui;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import com.zbennoz.zbenadmintool.permission.PermissionResolver;
import com.zbennoz.zbenadmintool.rank.RankPermission;
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

import java.util.Arrays;

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
        inv.setItem(22, button(Material.PAPER, "Rang erstellen", "rank_create",
                "§7Erstellt einen neuen Rang",
                "§7Eingabe erfolgt im Chat",
                "§7Tippe 'abbrechen' zum Stoppen"));
        player.openInventory(inv);
    }

    private static ItemStack button(Material material, String name, String action, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.getPersistentDataContainer().set(BUTTON_KEY, PersistentDataType.STRING, action);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        if (lore != null && lore.length > 0) {
            meta.setLore(Arrays.asList(lore));
        }
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
                if (resolver.has(player, RankPermission.ADMIN_MODE)) {
                    plugin.getAdminModeManager().toggle(player);
                }
            }
            case "vanish" -> {
                if (resolver.has(player, RankPermission.VANISH)) {
                    plugin.getVanishManager().toggle(player);
                }
            }
            case "gm1" -> {
                if (resolver.has(player, RankPermission.ADMIN_MODE)) {
                    player.setGameMode(org.bukkit.GameMode.CREATIVE);
                } else {
                    player.sendMessage(plugin.getMessages().raw("no_permission"));
                }
            }
            case "gm0" -> {
                if (resolver.has(player, RankPermission.ADMIN_MODE)) {
                    player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                } else {
                    player.sendMessage(plugin.getMessages().raw("no_permission"));
                }
            }
            case "offinv" -> player.performCommand("offinv " + player.getName());
            case "offec" -> player.performCommand("offec " + player.getName());
            case "rank" -> player.performCommand("rank list");
            case "rank_create" -> {
                if (resolver.has(player, RankPermission.RANK_MANAGE)) {
                    plugin.getChatInputListener().startSession(player);
                } else {
                    player.sendMessage(plugin.getMessages().raw("no_permission"));
                }
            }
            default -> {
            }
        }
    }
}
