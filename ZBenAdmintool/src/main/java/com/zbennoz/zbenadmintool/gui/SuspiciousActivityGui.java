package com.zbennoz.zbenadmintool.gui;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import com.zbennoz.zbenadmintool.permission.PermissionResolver;
import com.zbennoz.zbenadmintool.rank.RankPermission;
import com.zbennoz.zbenadmintool.service.SuspiciousMiningService;
import com.zbennoz.zbenadmintool.service.SuspiciousMiningService.SuspiciousEntry;
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

import java.util.List;

public class SuspiciousActivityGui implements Listener {

    private static final NamespacedKey KEY_ACTION = NamespacedKey.minecraft("suspect_action");
    private final ZBenAdmintool plugin;
    private final SuspiciousMiningService miningService;

    public SuspiciousActivityGui(ZBenAdmintool plugin, SuspiciousMiningService miningService) {
        this.plugin = plugin;
        this.miningService = miningService;
    }

    public void open(Player viewer) {
        Inventory inv = Bukkit.createInventory(null, 54, "Verdächtige Aktivitäten");
        List<SuspiciousEntry> entries = miningService.getRecent(45);
        if (entries.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            meta.setDisplayName(plugin.getMessages().raw("suspicious.none"));
            empty.setItemMeta(meta);
            inv.setItem(22, empty);
            viewer.openInventory(inv);
            return;
        }
        int slot = 0;
        for (SuspiciousEntry entry : entries) {
            inv.setItem(slot++, buildItem(entry));
        }
        viewer.openInventory(inv);
    }

    private ItemStack buildItem(SuspiciousEntry entry) {
        ItemStack stack = new ItemStack(Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName("§c" + entry.player() + " §7- §b" + entry.material());
        meta.setLore(List.of(
                "§7Anzahl: §f" + entry.amount(),
                "§7Zeit: §f" + SuspiciousMiningService.formatTime(entry.time()),
                "§7Ort: §f" + entry.world() + " @ Y=" + entry.y(),
                entry.resolved() ? "§aErledigt" : "§cOffen",
                "§eLinks: Teleport | Rechts: Erledigt"
        ));
        meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING,
                "entry:" + entry.player() + ":" + entry.time());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
        return stack;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getInventory().getHolder() != null) return;
        if (!"Verdächtige Aktivitäten".equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        String action = item.getItemMeta().getPersistentDataContainer().get(KEY_ACTION, PersistentDataType.STRING);
        if (action == null || !action.startsWith("entry:")) return;
        String[] parts = action.split(":");
        if (parts.length < 3) return;
        String suspect = parts[1];
        long time = Long.parseLong(parts[2]);
        SuspiciousEntry entry = miningService.getRecent(100).stream()
                .filter(e -> e.player().equals(suspect) && e.time() == time)
                .findFirst()
                .orElse(null);
        if (entry == null) return;
        PermissionResolver resolver = plugin.getPermissionResolver();
        if (event.isLeftClick()) {
            if (!resolver.has(player, RankPermission.OBSERVE)) {
                player.sendMessage(plugin.getMessages().raw("no_permission"));
                return;
            }
            Player target = Bukkit.getPlayerExact(entry.player());
            if (target == null) {
                player.sendMessage("§cSpieler ist offline.");
                return;
            }
            player.teleport(target.getLocation().clone().add(0, 1.2, 0));
            player.sendMessage("§aTeleportiert zu §f" + target.getName());
        } else if (event.isRightClick()) {
            if (!resolver.has(player, RankPermission.OBSERVE)) {
                player.sendMessage(plugin.getMessages().raw("no_permission"));
                return;
            }
            miningService.markResolved(entry);
            player.sendMessage("§aVerdachtsfall als erledigt markiert.");
            open(player);
        }
    }
}
