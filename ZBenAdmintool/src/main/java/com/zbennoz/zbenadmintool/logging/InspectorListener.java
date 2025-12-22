package com.zbennoz.zbenadmintool.logging;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class InspectorListener implements Listener {

    private final ZBenAdmintool plugin;
    private final LogManager logManager;
    private final Map<UUID, Map<String, Snapshot>> snapshots = new HashMap<>();
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
        if (!plugin.getConfig().getBoolean("logging.containers.enabled", true)) return;
        Inventory inventory = event.getInventory();
        if (!isLoggableContainer(inventory)) return;
        Location location = getLocation(inventory);
        if (location == null) return;

        Map<Material, Integer> counts = countItems(inventory);
        String containerType = resolveContainerType(inventory, location);
        Snapshot snapshot = new Snapshot(counts, location, containerType, System.currentTimeMillis());
        String key = createSnapshotKey(event.getPlayer().getUniqueId(), location, containerType);
        snapshots.computeIfAbsent(event.getPlayer().getUniqueId(), id -> new HashMap<>()).put(key, snapshot);
        if (plugin.getConfig().getBoolean("logging.debug", false)) {
            plugin.getLogger().info("Snapshot gespeichert für " + key);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (!isLoggableContainer(inventory)) return;
        Location location = getLocation(inventory);
        if (location == null) return;
        String containerType = resolveContainerType(inventory, location);
        String key = createSnapshotKey(event.getPlayer().getUniqueId(), location, containerType);
        Snapshot before = snapshots.computeIfAbsent(event.getPlayer().getUniqueId(), id -> new HashMap<>()).remove(key);
        if (before == null) return;

        Map<Material, Integer> afterCounts = countItems(inventory);
        Map<Material, Integer> beforeCounts = before.counts();
        for (Material material : mergeKeys(beforeCounts, afterCounts)) {
            int beforeAmount = beforeCounts.getOrDefault(material, 0);
            int afterAmount = afterCounts.getOrDefault(material, 0);
            int delta = afterAmount - beforeAmount;
            if (delta == 0) continue;
            String action = delta > 0 ? "ADD" : "REMOVE";
            int amount = Math.abs(delta);
            logManager.logContainer(location, (Player) event.getPlayer(), material, amount, action, containerType);
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
            if (!plugin.getConfig().getBoolean("logging.containers.enabled", true)) {
                event.getPlayer().sendMessage(plugin.getMessages().raw("inspect.containers_disabled"));
                return;
            }
            logManager.sendContainerLogs(event.getPlayer(), loc, 1);
        } else {
            logManager.sendBlockLogs(event.getPlayer(), loc, 1);
        }
    }

    public void toggleInspector(Player player) {
        inspector.put(player.getUniqueId(), !inspector.getOrDefault(player.getUniqueId(), false));
        player.sendMessage(plugin.getMessages().raw(inspector.get(player.getUniqueId()) ? "inspect.enabled" : "inspect.disabled"));
    }

    private Map<Material, Integer> countItems(Inventory inventory) {
        Map<Material, Integer> counts = new HashMap<>();
        for (ItemStack stack : inventory.getContents()) {
            if (stack == null || stack.getType() == Material.AIR) continue;
            counts.merge(stack.getType(), stack.getAmount(), Integer::sum);
        }
        return counts;
    }

    private Location getLocation(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof DoubleChest doubleChest) {
            InventoryHolder left = doubleChest.getLeftSide();
            if (left instanceof Chest chest) {
                return chest.getLocation();
            }
        }
        if (holder instanceof Chest chest) {
            return chest.getLocation();
        }
        if (holder instanceof Container container) {
            return container.getLocation();
        }
        if (holder instanceof BlockState state) {
            return state.getLocation();
        }
        return inventory.getLocation();
    }

    private boolean isLoggableContainer(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();
        if (holder == null || holder instanceof Player) return false;
        InventoryType type = inventory.getType();
        if (type == InventoryType.PLAYER || type == InventoryType.ENDER_CHEST) {
            return false;
        }
        return holder instanceof Container || holder instanceof Chest || holder instanceof DoubleChest || holder instanceof BlockState;
    }

    private String createSnapshotKey(UUID player, Location location, String containerType) {
        return player + "|" + location.getWorld().getName() + "|" + location.getBlockX() + "|" + location.getBlockY() + "|" + location.getBlockZ() + "|" + containerType.toLowerCase(Locale.ROOT);
    }

    private String resolveContainerType(Inventory inventory, Location location) {
        if (inventory.getType() != null) {
            return inventory.getType().name();
        }
        return location.getBlock().getType().name();
    }

    private Iterable<Material> mergeKeys(Map<Material, Integer> before, Map<Material, Integer> after) {
        Map<Material, Integer> all = new HashMap<>(before);
        after.keySet().forEach(mat -> all.putIfAbsent(mat, 0));
        return all.keySet();
    }

    private record Snapshot(Map<Material, Integer> counts, Location location, String containerType, long timestamp) {
    }
}
