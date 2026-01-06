package com.zbennoz.zbenenchants.util;

import com.zbennoz.zbenenchants.core.ZBenEnchantsPlugin;
import com.zbennoz.zbenenchants.enchant.CustomEnchant;
import com.zbennoz.zbenenchants.storage.PDCUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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
        PersistentDataContainer container = Objects.requireNonNull(book.getItemMeta()).getPersistentDataContainer();
        container.set(plugin.getKey("id"), PersistentDataType.STRING, enchant.getKey());
        container.set(plugin.getKey("level"), PersistentDataType.INTEGER, level);
        book.setItemMeta(meta);
        PDCUtil.setEnchantLevel(plugin, book, enchant, level);
        return book;
    }

    public static EnchantData getEnchantFromBook(ZBenEnchantsPlugin plugin, ItemStack book) {
        if (book == null || book.getType() != Material.ENCHANTED_BOOK) {
            return null;
        }
        EnchantData data = fromDedicatedKeys(plugin, book);
        if (data != null) {
            return data;
        }
        for (CustomEnchant enchant : CustomEnchant.values()) {
            int level = PDCUtil.getEnchantLevel(plugin, book, enchant);
            if (level > 0) {
                return new EnchantData(enchant, level);
            }
        }
        return fromLore(book);
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

    public static int parseRomanOrNumber(String value) {
        if (value == null) {
            return 0;
        }
        String upper = value.trim().toUpperCase(Locale.ROOT);
        switch (upper) {
            case "I":
                return 1;
            case "II":
                return 2;
            case "III":
                return 3;
            case "IV":
                return 4;
            case "V":
                return 5;
            default:
                try {
                    return Integer.parseInt(upper);
                } catch (NumberFormatException ignored) {
                    return 0;
                }
        }
    }

    private static EnchantData fromDedicatedKeys(ZBenEnchantsPlugin plugin, ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String id = container.get(plugin.getKey("id"), PersistentDataType.STRING);
        Integer level = container.get(plugin.getKey("level"), PersistentDataType.INTEGER);
        if (id == null || level == null || level <= 0) {
            return null;
        }
        CustomEnchant enchant = resolveEnchant(id);
        return enchant != null ? new EnchantData(enchant, level) : null;
    }

    private static EnchantData fromLore(ItemStack book) {
        ItemMeta meta = book.getItemMeta();
        if (meta == null || meta.getLore() == null) {
            return null;
        }
        for (String line : meta.getLore()) {
            String stripped = ChatColor.stripColor(line);
            if (stripped == null) {
                continue;
            }
            String lower = stripped.toLowerCase(Locale.ROOT);
            if (!lower.startsWith("sonderverzauberung:")) {
                continue;
            }
            String payload = stripped.substring("Sonderverzauberung:".length()).trim();
            for (CustomEnchant enchant : CustomEnchant.values()) {
                String name = enchant.getDisplayName();
                if (payload.toLowerCase(Locale.ROOT).startsWith(name.toLowerCase(Locale.ROOT))) {
                    String[] parts = payload.split(" ");
                    int parsedLevel = parts.length > 1 ? parseRomanOrNumber(parts[parts.length - 1]) : 1;
                    return new EnchantData(enchant, Math.max(1, parsedLevel));
                }
            }
        }
        return null;
    }

    private static CustomEnchant resolveEnchant(String id) {
        if (id == null) {
            return null;
        }
        for (CustomEnchant enchant : CustomEnchant.values()) {
            if (enchant.getKey().equalsIgnoreCase(id) || enchant.name().equalsIgnoreCase(id)) {
                return enchant;
            }
        }
        return null;
    }

    public static class EnchantData {
        private final CustomEnchant enchant;
        private final int level;

        public EnchantData(CustomEnchant enchant, int level) {
            this.enchant = enchant;
            this.level = level;
        }

        public CustomEnchant enchant() {
            return enchant;
        }

        public int level() {
            return level;
        }
    }
}
