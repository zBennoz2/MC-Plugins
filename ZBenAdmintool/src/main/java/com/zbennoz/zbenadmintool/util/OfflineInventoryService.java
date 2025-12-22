package com.zbennoz.zbenadmintool.util;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import com.zbennoz.zbenadmintool.text.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.inventory.InventoryType;

import java.io.File;
import java.util.UUID;

public class OfflineInventoryService {

    private final ZBenAdmintool plugin;
    private final MessageService messages;

    public OfflineInventoryService(ZBenAdmintool plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void openInventory(CommandSender sender, OfflinePlayer target, boolean enderChest) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage("Nur Spieler können dies nutzen.");
            return;
        }
        if (target.isOnline()) {
            Player online = target.getPlayer();
            Inventory inv = enderChest ? online.getEnderChest() : online.getInventory();
            viewer.openInventory(inv);
            return;
        }
        Inventory inv = enderChest ? Bukkit.createInventory(null, InventoryType.ENDER_CHEST, "Enderchest von " + target.getName()) : Bukkit.createInventory(null, 36, "Inventar von " + target.getName());
        readOfflineInventory(target.getUniqueId(), inv, enderChest);
        viewer.openInventory(inv);
        viewer.sendMessage(messages.raw("offline.read_only"));
    }

    private void readOfflineInventory(UUID uuid, Inventory inv, boolean enderChest) {
        File worldDir = new File(plugin.getServer().getWorldContainer(), plugin.getServer().getWorlds().get(0).getName());
        File playerData = new File(worldDir, "playerdata" + File.separator + uuid.toString() + ".dat");
        if (!playerData.exists()) {
            return;
        }
        // Ohne externe NBT-Abhängigkeit ist ein echtes Auslesen nicht möglich.
        // Zur Transparenz wird ein leeres Inventar angezeigt.
    }
}
