package com.zbennoz.zbenadmintool.util;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import com.zbennoz.zbenadmintool.text.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OfflineInventoryService implements Listener {

    private final ZBenAdmintool plugin;
    private final MessageService messages;
    private final File dataFolder;
    private final Map<UUID, OfflineInventorySession> sessionsByViewer = new HashMap<>();
    private final Map<UUID, OfflineInventorySession> sessionsByTarget = new HashMap<>();

    public OfflineInventoryService(ZBenAdmintool plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.dataFolder = new File(plugin.getDataFolder(), "offline-inventories");
        //noinspection ResultOfMethodCallIgnored
        dataFolder.mkdirs();
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
        OfflineInventoryHolder holder = new OfflineInventoryHolder(target.getUniqueId(), enderChest);
        Inventory inv = Bukkit.createInventory(holder, enderChest ? 27 : 36,
                enderChest ? "Enderchest von " + target.getName() : "Inventar von " + target.getName());
        readStoredInventory(target.getUniqueId(), inv, enderChest);

        OfflineInventorySession session = new OfflineInventorySession(holder, viewer.getUniqueId());
        sessionsByViewer.put(viewer.getUniqueId(), session);
        sessionsByTarget.put(target.getUniqueId(), session);
        plugin.getLogger().info(viewer.getName() + " öffnet Offline-Inventar von " + target.getName());
        viewer.openInventory(inv);
        viewer.sendMessage(messages.raw("offline.read_only"));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof OfflineInventoryHolder holder)) {
            return;
        }
        OfflineInventorySession session = sessionsByViewer.remove(event.getPlayer().getUniqueId());
        if (session == null || session.isCancelled()) {
            return;
        }
        sessionsByTarget.remove(holder.target);
        saveInventory(holder.target, event.getInventory(), holder.enderChest, true);
        event.getPlayer().sendMessage(messages.raw("offline.saved"));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        saveInventory(player.getUniqueId(), player.getInventory(), false, false);
        saveInventory(player.getUniqueId(), player.getEnderChest(), true, false);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        OfflineInventorySession session = sessionsByTarget.remove(player.getUniqueId());
        if (session != null) {
            session.setCancelled(true);
            Player viewer = Bukkit.getPlayer(session.viewer);
            if (viewer != null) {
                viewer.closeInventory();
                viewer.sendMessage(messages.raw("offline.locked"));
            }
        }
        sessionsByViewer.values().removeIf(s -> s.getTarget().equals(player.getUniqueId()));
        applyPendingInventory(player, false);
        applyPendingInventory(player, true);
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        OfflineInventorySession session = sessionsByTarget.get(event.getUniqueId());
        if (session != null) {
            session.setCancelled(true);
        }
    }

    private void applyPendingInventory(Player player, boolean enderChest) {
        File file = fileFor(player.getUniqueId());
        if (!file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String path = enderChest ? "enderchest" : "inventory";
        boolean pending = config.getBoolean(path + ".pending", false);
        if (!pending) {
            return;
        }
        List<ItemStack> stored = (List<ItemStack>) config.getList(path + ".items");
        if (stored == null) {
            return;
        }
        ItemStack[] items = stored.toArray(new ItemStack[0]);
        Inventory targetInv = enderChest ? player.getEnderChest() : player.getInventory();
        int size = targetInv.getSize();
        for (int i = 0; i < size && i < items.length; i++) {
            targetInv.setItem(i, items[i]);
        }
        for (int i = size; i < items.length; i++) {
            if (items[i] != null) {
                player.getWorld().dropItemNaturally(player.getLocation(), items[i]);
            }
        }
        config.set(path + ".pending", false);
        saveConfig(file, config);
    }

    private void readStoredInventory(UUID uuid, Inventory inv, boolean enderChest) {
        File file = fileFor(uuid);
        if (!file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String basePath = enderChest ? "enderchest" : "inventory";
        List<ItemStack> items = (List<ItemStack>) config.getList(basePath + ".items");
        if (items == null) {
            return;
        }
        for (int i = 0; i < inv.getSize() && i < items.size(); i++) {
            inv.setItem(i, items.get(i));
        }
    }

    private void saveInventory(UUID uuid, Inventory inv, boolean enderChest, boolean pendingUpdate) {
        File file = fileFor(uuid);
        FileConfiguration config = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        String basePath = enderChest ? "enderchest" : "inventory";
        config.set(basePath + ".items", inv.getContents());
        config.set(basePath + ".pending", pendingUpdate);
        config.set(basePath + ".updated", Instant.now().toEpochMilli());
        saveConfig(file, config);
    }

    private void saveConfig(File file, FileConfiguration config) {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Konnte Offline-Inventar nicht speichern: " + e.getMessage());
        }
    }

    private File fileFor(UUID uuid) {
        return new File(dataFolder, uuid.toString() + ".yml");
    }

    private static class OfflineInventoryHolder implements InventoryHolder {
        private final UUID target;
        private final boolean enderChest;

        private OfflineInventoryHolder(UUID target, boolean enderChest) {
            this.target = target;
            this.enderChest = enderChest;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static class OfflineInventorySession {
        private final OfflineInventoryHolder holder;
        private final UUID viewer;
        private boolean cancelled;

        private OfflineInventorySession(OfflineInventoryHolder holder, UUID viewer) {
            this.holder = holder;
            this.viewer = viewer;
        }

        public UUID getViewer() {
            return viewer;
        }

        public UUID getTarget() {
            return holder.target;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }
}
