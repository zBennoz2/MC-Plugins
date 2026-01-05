package com.zbennoz.zbenenchants.listener.anvil;

import com.zbennoz.zbenenchants.core.ZBenEnchantsPlugin;
import com.zbennoz.zbenenchants.enchant.CustomEnchant;
import com.zbennoz.zbenenchants.storage.PDCUtil;
import com.zbennoz.zbenenchants.util.ItemUtil;
import com.zbennoz.zbenenchants.util.MessageUtil;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sorgt dafür, dass Custom-Verzauberungsbücher im Amboss funktionieren.
 */
public class AnvilListener implements Listener {

    private final ZBenEnchantsPlugin plugin;
    private final Map<UUID, PendingAnvilEnchant> pending = new HashMap<>();

    public AnvilListener(ZBenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepare(PrepareAnvilEvent event) {
        if (!plugin.getConfig().getBoolean("anvil.enabled", true)) {
            return;
        }
        AnvilInventory inventory = event.getInventory();
        ItemStack base = inventory.getFirstItem();
        ItemStack addition = inventory.getSecondItem();
        Player player = (Player) event.getView().getPlayer();

        pending.remove(player.getUniqueId());

        if (base == null || base.getType() == Material.AIR || addition == null || addition.getType() != Material.ENCHANTED_BOOK) {
            inventory.setResult(null);
            return;
        }

        CustomEnchant enchant = ItemUtil.getEnchantFromBook(plugin, addition);
        if (enchant == null) {
            inventory.setResult(null);
            return;
        }

        if (!plugin.getConfig().getBoolean("anvil.allowStackedBooks", false) && addition.getAmount() > 1) {
            inventory.setResult(null);
            return;
        }

        if (!enchant.isApplicable(base.getType())) {
            inventory.setResult(null);
            return;
        }

        int bookLevel = PDCUtil.getEnchantLevel(plugin, addition, enchant);
        if (bookLevel <= 0) {
            inventory.setResult(null);
            return;
        }

        ItemStack result = base.clone();
        ItemUtil.applyEnchant(plugin, result, enchant, bookLevel);

        int cost = plugin.getConfig().getInt("anvil.baseCost", 3) + plugin.getConfig().getInt("anvil.costPerLevel", 2) * bookLevel;
        inventory.setRepairCost(cost);
        inventory.setResult(result);
        pending.put(player.getUniqueId(), new PendingAnvilEnchant(enchant, bookLevel, cost));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.ANVIL) {
            return;
        }
        if (event.getSlot() != 2) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        PendingAnvilEnchant info = pending.get(player.getUniqueId());
        if (info == null) {
            return;
        }
        AnvilInventory inventory = (AnvilInventory) event.getInventory();
        ItemStack result = inventory.getResult();
        if (result == null) {
            return;
        }
        if (player.getGameMode() != GameMode.CREATIVE && player.getLevel() < info.cost) {
            event.setCancelled(true);
            MessageUtil.send(plugin, player, "anvil-not-enough-levels");
            return;
        }

        event.setCancelled(true);
        if (player.getGameMode() != GameMode.CREATIVE) {
            player.setLevel(player.getLevel() - info.cost);
        }
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(result);
        leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));

        inventory.setFirstItem(null);
        inventory.setSecondItem(null);
        inventory.setResult(null);
        pending.remove(player.getUniqueId());
    }

    private static class PendingAnvilEnchant {
        private final CustomEnchant enchant;
        private final int level;
        private final int cost;

        private PendingAnvilEnchant(CustomEnchant enchant, int level, int cost) {
            this.enchant = enchant;
            this.level = level;
            this.cost = cost;
        }
    }
}

