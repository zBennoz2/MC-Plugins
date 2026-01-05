package com.zbennoz.zbenenchants.util;

import com.zbennoz.zbenenchants.core.ZBenEnchantsPlugin;
import com.zbennoz.zbenenchants.enchant.CustomEnchant;
import com.zbennoz.zbenenchants.storage.PDCUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Item-Helfer für Bücher und Lore.
 */
public final class ItemUtil {

    private ItemUtil() {
    }

    public static ItemStack createEnchantBook(ZBenEnchantsPlugin plugin, CustomEnchant enchant, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Sonderverzauberung: " + enchant.getDisplayName() + " " + roman(level));
        lore.add(ChatColor.DARK_GRAY + "Nutze einen Amboss, um sie anzuwenden.");
        meta.setLore(lore);
        book.setItemMeta(meta);
        PDCUtil.setEnchantLevel(plugin, book, enchant, level);
        return book;
    }

    public static CustomEnchant getEnchantFromBook(ZBenEnchantsPlugin plugin, ItemStack book) {
        if (book == null || book.getType() != Material.ENCHANTED_BOOK) {
            return null;
        }
        for (CustomEnchant enchant : CustomEnchant.values()) {
            if (PDCUtil.getEnchantLevel(plugin, book, enchant) > 0) {
                return enchant;
            }
        }
        return null;
    }

    public static void applyEnchant(ZBenEnchantsPlugin plugin, ItemStack target, CustomEnchant enchant, int level) {
        int capped = Math.min(level, enchant.getMaxLevel());
        PDCUtil.setEnchantLevel(plugin, target, enchant, capped);
        updateLore(plugin, target);
    }

    public static void updateLore(ZBenEnchantsPlugin plugin, ItemStack item) {
        if (item == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        List<String> lore = new ArrayList<>();
        for (CustomEnchant enchant : CustomEnchant.values()) {
            int level = PDCUtil.getEnchantLevel(plugin, item, enchant);
            if (level > 0) {
                lore.add(ChatColor.GRAY + enchant.getDisplayName() + " " + roman(level));
            }
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    public static String roman(int number) {
        switch (number) {
            case 1:
                return "I";
            case 2:
                return "II";
            case 3:
                return "III";
            case 4:
                return "IV";
            case 5:
                return "V";
            default:
                return String.valueOf(number);
        }
    }
}
