package com.zbennoz.zbenadmintool.gui;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import com.zbennoz.zbenadmintool.permission.PermissionResolver;
import com.zbennoz.zbenadmintool.rank.RankPermission;
import com.zbennoz.zbenadmintool.service.TeleportLogService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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

import java.util.ArrayList;
import java.util.List;

public class ObserveGui implements Listener {

    private static final NamespacedKey KEY_ACTION = NamespacedKey.minecraft("observe_action");
    private final ZBenAdmintool plugin;
    private final TeleportLogService teleportLogService;

    public ObserveGui(ZBenAdmintool plugin, TeleportLogService teleportLogService) {
        this.plugin = plugin;
        this.teleportLogService = teleportLogService;
    }

    public void open(Player viewer, int page) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        int size = 54;
        int start = page * 45;
        Inventory inv = Bukkit.createInventory(null, size, "Spieler beobachten | Seite " + (page + 1));
        int slot = 0;
        for (int i = start; i < Math.min(players.size(), start + 45); i++) {
            Player target = players.get(i);
            inv.setItem(slot++, playerItem(target));
        }
        if (page > 0) {
            inv.setItem(45, navItem(Material.ARROW, "§eVorherige Seite", "prev:" + (page - 1)));
        }
        if (players.size() > start + 45) {
            inv.setItem(53, navItem(Material.ARROW, "§eNächste Seite", "next:" + (page + 1)));
        }
        viewer.openInventory(inv);
    }

    private ItemStack playerItem(Player target) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = stack.getItemMeta();
        if (meta instanceof org.bukkit.inventory.meta.SkullMeta skull) {
            skull.setOwningPlayer(target);
            meta = skull;
        }
        meta.setDisplayName("§b" + target.getName());
        meta.setLore(List.of(
                "§7Welt: §f" + target.getWorld().getName(),
                "§7Position: §f" + target.getLocation().getBlockX() + ", " + target.getLocation().getBlockY() + ", " + target.getLocation().getBlockZ(),
                "§aKlicke zum Teleport"));
        meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, "tp:" + target.getUniqueId());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack navItem(Material material, String name, String action) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, action);
        stack.setItemMeta(meta);
        return stack;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getInventory().getHolder() != null) return;
        if (!event.getView().getTitle().startsWith("Spieler beobachten")) return;
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        String action = item.getItemMeta().getPersistentDataContainer().get(KEY_ACTION, PersistentDataType.STRING);
        if (action == null) return;
        if (action.startsWith("prev:")) {
            int page = Integer.parseInt(action.split(":")[1]);
            open(player, page);
        } else if (action.startsWith("next:")) {
            int page = Integer.parseInt(action.split(":")[1]);
            open(player, page);
        } else if (action.startsWith("tp:")) {
            PermissionResolver resolver = plugin.getPermissionResolver();
            if (!resolver.has(player, RankPermission.OBSERVE) && !resolver.has(player, RankPermission.TELEPORT)) {
                player.sendMessage(plugin.getMessages().raw("no_permission"));
                return;
            }
            Player target = Bukkit.getPlayer(java.util.UUID.fromString(action.split(":")[1]));
            if (target == null) {
                player.sendMessage("§cSpieler ist nicht mehr online.");
                return;
            }
            Location destination = target.getLocation().clone().add(0, 1.2, 0);
            destination.setDirection(target.getLocation().getDirection());
            player.teleport(destination);
            teleportLogService.logTeleport(player.getName(), target.getName(), destination);
            player.sendMessage("§aDu wurdest zu §f" + target.getName() + " §ateleportiert.");
        }
    }
}
