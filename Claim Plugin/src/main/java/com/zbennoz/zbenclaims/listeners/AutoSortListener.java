package com.zbennoz.zbenclaims.listeners;

import com.zbennoz.zbenclaims.ZBenClaimsPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.block.Lockable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

public class AutoSortListener implements Listener {

    private final ZBenClaimsPlugin plugin;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();

    public AutoSortListener(ZBenClaimsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClick() != ClickType.MIDDLE) return;
        if (!plugin.getConfig().getBoolean("autosort.enabled", true)) return;

        boolean requiresPerm = plugin.getConfig().getBoolean("autosort.requires_permission", false);
        if (requiresPerm && !player.hasPermission("claim.autosort") && !player.isOp()) {
            plugin.getMessages().send(player, "autosort.noPermission");
            return;
        }

        Inventory clicked = event.getClickedInventory();
        if (clicked == null) return;

        boolean isPlayerInventory = clicked.getType() == InventoryType.PLAYER || clicked.equals(player.getInventory());
        if (isPlayerInventory && !plugin.getConfig().getBoolean("autosort.player_inventory", true)) return;
        if (!isPlayerInventory && !plugin.getConfig().getBoolean("autosort.containers", true)) return;

        if (!canSort(clicked)) {
            plugin.getMessages().send(player, "autosort.locked");
            return;
        }

        event.setCancelled(true);
        if (isPlayerInventory) {
            sortPlayerInventory(player);
        } else {
            sortInventory(clicked);
        }

        String actionbar = plugin.getMessages().get("autosort.sorted");
        if (actionbar != null && !actionbar.isBlank()) {
            player.sendActionBar(serializer.deserialize(actionbar));
        }
    }

    private boolean canSort(Inventory inventory) {
        if (inventory.getHolder() instanceof Lockable lockable) {
            return !lockable.isLocked();
        }
        return true;
    }

    private void sortPlayerInventory(Player player) {
        PlayerInventory inventory = player.getInventory();
        boolean sortHotbar = plugin.getConfig().getBoolean("autosort.sort_hotbar", false);
        ItemStack[] storage = inventory.getStorageContents();

        List<Integer> targetSlots = new ArrayList<>();
        for (int i = 0; i < storage.length; i++) {
            if (!sortHotbar && i < 9) continue;
            targetSlots.add(i);
        }

        List<ItemStack> collected = new ArrayList<>();
        for (int slot : targetSlots) {
            ItemStack stack = storage[slot];
            if (stack != null && stack.getType() != org.bukkit.Material.AIR) {
                collected.add(stack.clone());
            }
            storage[slot] = null;
        }

        List<ItemStack> merged = mergeAndSort(collected);
        for (int i = 0; i < targetSlots.size() && i < merged.size(); i++) {
            storage[targetSlots.get(i)] = merged.get(i);
        }

        inventory.setStorageContents(storage);
        player.updateInventory();
    }

    private void sortInventory(Inventory inventory) {
        ItemStack[] contents = inventory.getContents();
        List<ItemStack> collected = new ArrayList<>();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack != null && stack.getType() != org.bukkit.Material.AIR) {
                collected.add(stack.clone());
            }
            contents[i] = null;
        }
        List<ItemStack> merged = mergeAndSort(collected);
        for (int i = 0; i < contents.length && i < merged.size(); i++) {
            contents[i] = merged.get(i);
        }
        inventory.setContents(contents);
    }

    private List<ItemStack> mergeAndSort(List<ItemStack> stacks) {
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack stack : stacks) {
            addToMerged(merged, stack.clone());
        }
        merged.sort(Comparator
                .comparing((ItemStack s) -> s.getType().name())
                .thenComparing(Comparator.comparingInt(ItemStack::getAmount).reversed()));
        return merged;
    }

    private void addToMerged(List<ItemStack> merged, ItemStack stack) {
        for (ItemStack existing : merged) {
            if (existing.isSimilar(stack) && existing.getAmount() < existing.getMaxStackSize()) {
                int space = existing.getMaxStackSize() - existing.getAmount();
                int transfer = Math.min(space, stack.getAmount());
                existing.setAmount(existing.getAmount() + transfer);
                stack.setAmount(stack.getAmount() - transfer);
                if (stack.getAmount() <= 0) return;
            }
        }
        merged.add(stack);
    }
}
