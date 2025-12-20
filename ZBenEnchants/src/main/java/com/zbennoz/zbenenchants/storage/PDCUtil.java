package com.zbennoz.zbenenchants.storage;

import com.zbennoz.zbenenchants.core.ZBenEnchantsPlugin;
import com.zbennoz.zbenenchants.enchant.CustomEnchant;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Hilfsmethoden für den Zugriff auf PDC-Daten der Items.
 */
public final class PDCUtil {

    private PDCUtil() {
    }

    public static int getEnchantLevel(ZBenEnchantsPlugin plugin, ItemStack itemStack, CustomEnchant enchant) {
        if (itemStack == null) {
            return 0;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return 0;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Integer level = container.get(plugin.getEnchantKey(enchant), PersistentDataType.INTEGER);
        return level != null ? level : 0;
    }

    public static void setEnchantLevel(ZBenEnchantsPlugin plugin, ItemStack itemStack, CustomEnchant enchant, int level) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(plugin.getEnchantKey(enchant), PersistentDataType.INTEGER, level);
        itemStack.setItemMeta(meta);
    }

    public static boolean hasEnchant(ZBenEnchantsPlugin plugin, ItemStack stack, CustomEnchant enchant) {
        return getEnchantLevel(plugin, stack, enchant) > 0;
    }
}
