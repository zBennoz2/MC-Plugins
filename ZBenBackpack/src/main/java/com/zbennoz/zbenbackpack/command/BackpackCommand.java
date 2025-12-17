package com.zbennoz.zbenbackpack.command;

import com.zbennoz.zbenbackpack.ZBenBackpackPlugin;
import com.zbennoz.zbenbackpack.data.BackpackDatabase;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;

public class BackpackCommand implements CommandExecutor, Listener {

    private final ZBenBackpackPlugin plugin;
    private final BackpackDatabase database;
    private final NamespacedKey key;

    public BackpackCommand(ZBenBackpackPlugin plugin, BackpackDatabase database) {
        this.plugin = plugin;
        this.database = database;
        this.key = new NamespacedKey(plugin, "backpack");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }
        openBackpack(player);
        return true;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) return;
        if (!isBackpack(item)) return;
        event.setCancelled(true);
        openBackpack(event.getPlayer());
    }

    public ItemStack createBackpackItem(Player player) {
        ItemStack stack = new ItemStack(Material.BUNDLE);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text("Backpack"));
        meta.lore(java.util.List.of(Component.text("Right click to open")));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    private boolean isBackpack(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(key, PersistentDataType.BYTE);
    }

    private int sizeFor(Player player) {
        int size = 9;
        for (int i : new int[]{54, 45, 36, 27, 18, 9}) {
            if (player.hasPermission("zbenbackpack.size." + i)) {
                size = i;
                break;
            }
        }
        return size;
    }

    private void openBackpack(Player player) {
        int size = sizeFor(player);
        Inventory inventory = Bukkit.createInventory(player, size, Component.text("Backpack (" + size + ")"));
        String data = database.loadBackpack(player.getUniqueId().toString());
        if (data != null) {
            ItemStack[] contents = deserialize(data, size);
            inventory.setContents(contents);
        }
        player.openInventory(inventory);
        player.sendMessage(Component.text("Backpack opened."));
    }

    public void save(Player player) {
        Inventory open = player.getOpenInventory().getTopInventory();
        String data = serialize(open.getContents());
        database.saveBackpackAsync(player.getUniqueId().toString(), data);
    }

    private String serialize(ItemStack[] items) {
        try {
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
            config.set("i", items);
            outputStream.write(config.saveToString().getBytes());
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    private ItemStack[] deserialize(String data, int size) {
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            String yaml = new String(bytes);
            org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
            config.loadFromString(yaml);
            ItemStack[] items = ((java.util.List<ItemStack>) config.getList("i", java.util.List.of())).toArray(new ItemStack[0]);
            ItemStack[] limited = new ItemStack[size];
            System.arraycopy(items, 0, limited, 0, Math.min(items.length, size));
            return limited;
        } catch (Exception e) {
            return new ItemStack[size];
        }
    }
}
