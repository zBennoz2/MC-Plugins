package com.zbennoz.zbenenchants.listener;

import com.zbennoz.zbenenchants.core.ZBenEnchantsPlugin;
import com.zbennoz.zbenenchants.enchant.CustomEnchant;
import com.zbennoz.zbenenchants.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Optional: Custom-Enchant-Bücher als Librarian-Trade.
 */
public class VillagerTradeListener implements Listener {

    private final ZBenEnchantsPlugin plugin;
    private final Logger logger;

    public VillagerTradeListener(ZBenEnchantsPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAcquireTrade(VillagerAcquireTradeEvent event) {
        if (!plugin.getConfig().getBoolean("villagers.enabled", true)) {
            return;
        }
        AbstractVillager abstractVillager = event.getEntity();
        if (!(abstractVillager instanceof Villager)) {
            return;
        }
        Villager villager = (Villager) abstractVillager;
        List<String> professions = plugin.getConfig().getStringList("villagers.professions");
        if (!professions.isEmpty() && professions.stream().noneMatch(p -> p.equalsIgnoreCase(villager.getProfession().name()))) {
            return;
        }
        int minLevel = plugin.getConfig().getInt("villagers.minLevel", 1);
        int maxLevel = plugin.getConfig().getInt("villagers.maxLevel", 5);
        if (villager.getVillagerLevel() < minLevel || villager.getVillagerLevel() > maxLevel) {
            return;
        }
        double chance = plugin.getConfig().getDouble("villagers.chancePerTradeRefresh", 0.15);
        if (ThreadLocalRandom.current().nextDouble() > chance) {
            return;
        }
        CustomEnchant enchant = CustomEnchant.values()[ThreadLocalRandom.current().nextInt(CustomEnchant.values().length)];
        int level = ThreadLocalRandom.current().nextInt(enchant.getMaxLevel()) + 1;

        ItemStack book = ItemUtil.createEnchantBook(plugin, enchant, level);
        int base = plugin.getConfig().getInt("villagers.emeraldCost.base", 12);
        int perLevel = plugin.getConfig().getInt("villagers.emeraldCost.perEnchantLevel", 6);
        ItemStack emeralds = new ItemStack(Material.EMERALD, Math.max(1, base + perLevel * (level - 1)));
        ItemStack normalBook = new ItemStack(Material.BOOK, 1);

        MerchantRecipe recipe = new MerchantRecipe(book, 12);
        recipe.addIngredient(emeralds);
        recipe.addIngredient(normalBook);
        event.setRecipe(recipe);
        logger.info(String.format("[Villager] %s bietet %s %s (Kosten %d Smaragde + Buch)",
                villager.getProfession().name(), enchant.getDisplayName(), ItemUtil.roman(level), emeralds.getAmount()));
    }
}

