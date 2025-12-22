package com.zbennoz.zbenadmintool.logging;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InspectorListener implements Listener {

    private final ZBenAdmintool plugin;
    private final LogManager logManager;
    private final Map<UUID, Map<Integer, ItemStack>> snapshots = new HashMap<>();
    private final Map<UUID, Boolean> inspector = new HashMap<>();

    public InspectorListener(ZBenAdmintool plugin, LogManager logManager) {
        this.plugin = plugin;
        this.logManager = logManager;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        logManager.logBlock(event.getBlockPlaced().getLocation(), event.getPlayer(), event.getBlockPlaced().getType(), "PLACE");
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        logManager.logBlock(event.getBlock().getLocation(), event.getPlayer(), event.getBlock().getType(), "BREAK");
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof org.bukkit.block.Container)) return;
        if (!plugin.getConfig().getBoolean("logging.containers.enabled", true)) return;
        snapshots.put(event.getPlayer().getUniqueId(), cloneInventory(event.getInventory()));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Map<Integer, ItemStack> old = snapshots.remove(event.getPlayer().getUniqueId());
        if (old == null) return;
        Inventory inventory = event.getInventory();
        Location location = getLocation(inventory);
        if (location == null) return;
        Map<Integer, ItemStack> now = cloneInventory(inventory);
        int size = inventory.getSize();
        for (int i = 0; i < size; i++) {
            ItemStack before = old.get(i);
            ItemStack after = now.get(i);
            int beforeAmount = before == null ? 0 : before.getAmount();
            int nowAmount = after == null ? 0 : after.getAmount();
            if (nowAmount > beforeAmount && after != null) {
                logManager.logContainer(location, (Player) event.getPlayer(), after.getType(), nowAmount - beforeAmount, "ADD", inventory.getType().name());
            } else if (beforeAmount > nowAmount && before != null) {
                logManager.logContainer(location, (Player) event.getPlayer(), before.getType(), beforeAmount - nowAmount, "REMOVE", inventory.getType().name());
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!Boolean.TRUE.equals(inspector.get(event.getPlayer().getUniqueId()))) return;
        if (event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        Location loc = block.getLocation();
        event.setCancelled(true);
        if (block.getState() instanceof InventoryHolder) {
            logManager.sendContainerLogs(event.getPlayer(), loc, 1);
        } else {
            logManager.sendBlockLogs(event.getPlayer(), loc, 1);
        }
    }

    public void toggleInspector(Player player) {
        inspector.put(player.getUniqueId(), !inspector.getOrDefault(player.getUniqueId(), false));
        player.sendMessage(plugin.getMessages().raw(inspector.get(player.getUniqueId()) ? "inspect.enabled" : "inspect.disabled"));
    }

    private Map<Integer, ItemStack> cloneInventory(Inventory inventory) {
        Map<Integer, ItemStack> map = new HashMap<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null) {
                map.put(i, item.clone());
            }
        }
        return map;
    }

    private Location getLocation(Inventory inventory) {
        if (inventory.getLocation() != null) {
            return inventory.getLocation();
        }
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof org.bukkit.block.Container container) {
            return container.getLocation();
        }
        return null;
    }
}
