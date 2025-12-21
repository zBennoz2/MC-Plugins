package com.zbennoz.zbenlootr.container;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.inventory.DoubleChestInventory;

public final class ContainerIdUtil {

    private ContainerIdUtil() {}

    public static boolean isSupported(Block block, Iterable<ContainerType> enabled) {
        Material type = block.getType();
        for (ContainerType containerType : enabled) {
            if (containerType.matches(type)) {
                return true;
            }
        }
        return false;
    }

    public static String getContainerId(Block block, boolean detectDoubleChest) {
        Location location = canonicalLocation(block, detectDoubleChest);
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private static Location canonicalLocation(Block block, boolean detectDoubleChest) {
        if (!detectDoubleChest || !(block.getState() instanceof Chest chestState)) {
            return block.getLocation();
        }

        DoubleChestInventory doubleChestInventory;
        try {
            doubleChestInventory = chestState.getInventory() instanceof DoubleChestInventory dci ? dci : null;
        } catch (NoSuchMethodError ignored) {
            doubleChestInventory = null;
        }

        if (doubleChestInventory == null) {
            return block.getLocation();
        }

        BlockState left = (BlockState) doubleChestInventory.getLeftSide();
        BlockState right = (BlockState) doubleChestInventory.getRightSide();
        Location leftLoc = left.getBlock().getLocation();
        Location rightLoc = right.getBlock().getLocation();

        return chooseLower(leftLoc, rightLoc);
    }

    private static Location chooseLower(Location a, Location b) {
        if (!a.getWorld().equals(b.getWorld())) {
            return a;
        }
        if (a.getBlockY() != b.getBlockY()) {
            return a.getBlockY() < b.getBlockY() ? a : b;
        }
        if (a.getBlockZ() != b.getBlockZ()) {
            return a.getBlockZ() < b.getBlockZ() ? a : b;
        }
        return a.getBlockX() <= b.getBlockX() ? a : b;
    }
}
