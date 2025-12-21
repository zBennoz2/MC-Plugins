package com.zbennoz.zbenlootr.listeners;

import com.zbennoz.zbenlootr.ZBenLootrPlugin;
import com.zbennoz.zbenlootr.container.ContainerIdUtil;
import com.zbennoz.zbenlootr.container.ContainerType;
import com.zbennoz.zbenlootr.database.Database;
import com.zbennoz.zbenlootr.loot.LootGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ContainerOpenListener implements Listener {

    private final ZBenLootrPlugin plugin;
    private final Map<UUID, String> openInventories = new ConcurrentHashMap<>();

    public ContainerOpenListener(ZBenLootrPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        InventoryHolder holder = event.getInventory().getHolder();
        Block block = event.getInventory().getLocation() != null ? event.getInventory().getLocation().getBlock() : null;
        if (block == null || !isContainerHolder(holder)) {
            return;
        }

        Set<ContainerType> enabled = plugin.getEnabledContainerTypes();
        if (!ContainerIdUtil.isSupported(block, enabled)) {
            return;
        }

        event.setCancelled(true);
        String containerId = ContainerIdUtil.getContainerId(block, plugin.detectDoubleChest());
        int size = event.getInventory().getSize();
        Inventory personalInventory = plugin.getLootCache().get(containerId + ":" + player.getUniqueId())
                .map(ContainerOpenListener::cloneInventory)
                .orElseGet(() -> loadOrGenerate(block.getLocation(), containerId, size, player));

        openInventories.put(player.getUniqueId(), containerId);
        player.openInventory(personalInventory);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        HumanEntity human = event.getPlayer();
        if (!(human instanceof Player player)) {
            return;
        }
        String containerId = openInventories.remove(player.getUniqueId());
        if (containerId == null) {
            return;
        }
        Inventory inventory = event.getInventory();
        plugin.getLootCache().put(containerId + ":" + player.getUniqueId(), cloneInventory(inventory));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getDatabase().savePlayerInventory(containerId, player.getUniqueId(), inventory);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save inventory for " + player.getName() + ": " + e.getMessage());
            }
        });
    }

    private Inventory loadOrGenerate(Location location, String containerId, int size, Player player) {
        Database db = plugin.getDatabase();
        try {
            Optional<Inventory> existing = db.loadPlayerInventory(containerId, player.getUniqueId());
            if (existing.isPresent()) {
                return cloneInventory(existing.get());
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load inventory: " + e.getMessage());
        }

        Inventory generated = Bukkit.createInventory(null, size, player.getName() + "'s Loot");
        LootGenerator.fillInventory(plugin, generated, player, location);

        try {
            db.saveContainer(containerId, location, location.getBlock().getType().name());
            db.savePlayerInventory(containerId, player.getUniqueId(), generated);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save generated loot: " + e.getMessage());
        }
        return generated;
    }

    private static boolean isContainerHolder(InventoryHolder holder) {
        String name = holder != null ? holder.getClass().getSimpleName().toLowerCase() : "";
        return name.contains("chest") || name.contains("barrel") || name.contains("shulker");
    }

    private static Inventory cloneInventory(Inventory original) {
        Inventory clone = Bukkit.createInventory(null, original.getSize(), original.getTitle());
        for (int i = 0; i < original.getSize(); i++) {
            ItemStack item = original.getItem(i);
            if (item != null) {
                clone.setItem(i, item.clone());
            }
        }
        return clone;
    }
}
