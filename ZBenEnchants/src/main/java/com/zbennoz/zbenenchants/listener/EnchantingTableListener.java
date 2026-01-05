package com.zbennoz.zbenenchants.listener;

import com.zbennoz.zbenenchants.core.ZBenEnchantsPlugin;
import com.zbennoz.zbenenchants.enchant.CustomEnchant;
import com.zbennoz.zbenenchants.util.ItemUtil;
import com.zbennoz.zbenenchants.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Integration für den Verzauberungstisch.
 */
public class EnchantingTableListener implements Listener {

    private final ZBenEnchantsPlugin plugin;
    private final Map<UUID, Map<Integer, CustomSelection>> pendingSelections = new HashMap<>();

    public EnchantingTableListener(ZBenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepare(PrepareItemEnchantEvent event) {
        if (!plugin.getConfig().getBoolean("enchantingTable.enabled", true)) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        if (plugin.getConfig().getBoolean("enchantingTable.restrictToSupportedItems", true)
                && Arrays.stream(CustomEnchant.values()).noneMatch(enchant -> enchant.isApplicable(item.getType()))) {
            pendingSelections.remove(event.getEnchanter().getUniqueId());
            return;
        }
        int minBooks = plugin.getConfig().getInt("enchantingTable.minBookshelfPower", 0);
        if (event.getEnchantmentBonus() < minBooks) {
            pendingSelections.remove(event.getEnchanter().getUniqueId());
            return;
        }
        double chance = plugin.getConfig().getDouble("enchantingTable.chanceToAddCustomEnchant", 0.0);
        if (ThreadLocalRandom.current().nextDouble() > chance) {
            pendingSelections.remove(event.getEnchanter().getUniqueId());
            return;
        }

        List<CustomEnchant> applicable = new ArrayList<>();
        for (CustomEnchant enchant : CustomEnchant.values()) {
            if (enchant.isApplicable(item.getType())) {
                applicable.add(enchant);
            }
        }
        if (applicable.isEmpty()) {
            return;
        }

        boolean allowMultiple = plugin.getConfig().getBoolean("enchantingTable.allowMultipleCustomEnchants", false);
        Map<Integer, CustomSelection> current = new HashMap<>();
        EnchantmentOffer[] offers = event.getOffers();
        List<Integer> slots = Arrays.asList(0, 1, 2);
        Collections.shuffle(slots);

        for (Integer slot : slots) {
            if (slot >= offers.length) {
                continue;
            }
            EnchantmentOffer baseOffer = offers[slot];
            if (baseOffer == null) {
                continue;
            }
            CustomEnchant enchant = applicable.get(ThreadLocalRandom.current().nextInt(applicable.size()));
            Enchantment display = Enchantment.LURE;
            int level = Math.max(1, ThreadLocalRandom.current().nextInt(enchant.getMaxLevel()) + 1);
            EnchantmentOffer offer = new EnchantmentOffer(display, level, baseOffer.getCost());
            offers[slot] = offer;
            current.put(slot, new CustomSelection(enchant, level));
            String message = plugin.getMessage("table-offer");
            if (message != null && !message.isEmpty()) {
                String replaced = message.replace("{slot}", String.valueOf(slot + 1))
                        .replace("{value}", enchant.getDisplayName() + " " + ItemUtil.roman(level));
                event.getEnchanter().sendMessage(MessageUtil.parse(plugin, replaced));
            }
            if (!allowMultiple) {
                break;
            }
        }
        if (!current.isEmpty()) {
            pendingSelections.put(event.getEnchanter().getUniqueId(), current);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        Map<Integer, CustomSelection> selections = pendingSelections.remove(event.getEnchanter().getUniqueId());
        if (selections == null) {
            return;
        }
        CustomSelection selection = selections.get(event.whichButton());
        if (selection == null) {
            return;
        }
        ItemUtil.applyEnchant(plugin, event.getItem(), selection.enchant, selection.level);
        event.getEnchantsToAdd().clear();
    }

    private static class CustomSelection {
        private final CustomEnchant enchant;
        private final int level;

        private CustomSelection(CustomEnchant enchant, int level) {
            this.enchant = enchant;
            this.level = level;
        }
    }
}

