package com.zbennoz.zbencoins.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Hilfsfunktionen für sichere Inventaroperationen.
 */
public final class InventoryUtil {

    private InventoryUtil() {
    }

    public static boolean hasEnough(Player player, ItemStack template, int amount) {
        int remaining = amount;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || !stack.isSimilar(template)) {
                continue;
            }
            remaining -= stack.getAmount();
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean remove(Player player, ItemStack template, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack == null || !stack.isSimilar(template)) {
                continue;
            }
            int take = Math.min(stack.getAmount(), remaining);
            stack.setAmount(stack.getAmount() - take);
            if (stack.getAmount() <= 0) {
                player.getInventory().setItem(i, null);
            } else {
                player.getInventory().setItem(i, stack);
            }
            remaining -= take;
            if (remaining <= 0) {
                player.updateInventory();
                return true;
            }
        }
        player.updateInventory();
        return false;
    }

    public static void giveItem(Player player, ItemStack itemStack) {
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(itemStack);
        for (Map.Entry<Integer, ItemStack> entry : leftover.entrySet()) {
            player.getWorld().dropItemNaturally(player.getLocation(), entry.getValue());
        }
    }

    public static boolean canFit(Player player, ItemStack stack) {
        int remaining = stack.getAmount();
        ItemStack template = stack.clone();
        template.setAmount(1);

        for (ItemStack content : player.getInventory().getStorageContents()) {
            if (content == null || content.getType().isAir()) {
                remaining -= template.getMaxStackSize();
            } else if (content.isSimilar(template) && content.getAmount() < content.getMaxStackSize()) {
                remaining -= (content.getMaxStackSize() - content.getAmount());
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return remaining <= 0;
    }
}
