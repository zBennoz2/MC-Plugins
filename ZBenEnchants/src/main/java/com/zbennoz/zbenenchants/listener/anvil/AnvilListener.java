package com.zbennoz.zbenenchants.listener.anvil;

import com.zbennoz.zbenenchants.core.ZBenEnchantsPlugin;
import com.zbennoz.zbenenchants.enchant.CustomEnchant;
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
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Sorgt dafür, dass Custom-Verzauberungsbücher im Amboss funktionieren.
 */
public class AnvilListener implements Listener {

    private final ZBenEnchantsPlugin plugin;
    private final Map<UUID, PendingAnvilEnchant> pending = new HashMap<>();
    private final Logger logger;

    public AnvilListener(ZBenEnchantsPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepare(PrepareAnvilEvent event) {
        if (!plugin.getConfig().getBoolean("anvil.enabled", true)) {
            return;
        }
        AnvilInventory inventory = event.getInventory();
        ItemStack base = inventory.getItem(0);
        ItemStack addition = inventory.getItem(1);
        Player player = (Player) event.getView().getPlayer();

        pending.remove(player.getUniqueId());

        ItemUtil.EnchantData enchantData = ItemUtil.getEnchantFromBook(plugin, addition);
        boolean allowStack = plugin.getConfig().getBoolean("anvil.allowStackedBooks", false);
        DebugInfo debug = new DebugInfo(base, addition, enchantData);

        if (!allowStack && addition != null && addition.getAmount() > 1) {
            debug.reason = "gestapeltes Buch nicht erlaubt";
            finalizeResult(event, null, debug);
            return;
        }

        if (!canApplyTo(base, enchantData, debug)) {
            finalizeResult(event, null, debug);
            return;
        }

        ItemStack result = base.clone();
        ItemUtil.applyEnchant(plugin, result, enchantData.enchant(), enchantData.level());

        int cost = plugin.getConfig().getInt("anvil.baseCost", 3) + plugin.getConfig().getInt("anvil.costPerLevel", 2) * enchantData.level();
        inventory.setRepairCost(cost);
        event.setResult(result);
        pending.put(player.getUniqueId(), new PendingAnvilEnchant(enchantData.enchant(), enchantData.level(), cost));
        debug.resultSet = true;
        finalizeResult(event, result, debug);
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

    private boolean canApplyTo(ItemStack base, ItemUtil.EnchantData enchantData, DebugInfo debug) {
        if (base == null || base.getType() == Material.AIR) {
            debug.reason = "kein Basis-Item";
            return false;
        }
        if (enchantData == null) {
            debug.reason = "rechtes Item kein Custom-Buch";
            return false;
        }
        if (!enchantData.enchant().isApplicable(base.getType())) {
            debug.reason = "Material nicht kompatibel";
            return false;
        }
        debug.compatible = true;
        return true;
    }

    private void finalizeResult(PrepareAnvilEvent event, ItemStack result, DebugInfo debug) {
        event.setResult(result);
        String enchantId = debug.enchantData != null ? debug.enchantData.enchant().getKey() : "-";
        String enchantLevel = debug.enchantData != null ? String.valueOf(debug.enchantData.level()) : "-";
        logger.info(String.format("[Amboss] Links=%s | Rechts=%s | Enchant=%s | Level=%s | kompatibel=%s | Ergebnis=%s%s",
                describe(debug.left),
                describe(debug.right),
                enchantId,
                enchantLevel,
                debug.compatible,
                debug.resultSet,
                debug.reason.isEmpty() ? "" : " | Grund: " + debug.reason));
    }

    private String describe(ItemStack stack) {
        if (stack == null) {
            return "leer";
        }
        return stack.getType().name() + " x" + stack.getAmount();
    }

    private static class DebugInfo {
        private final ItemStack left;
        private final ItemStack right;
        private final ItemUtil.EnchantData enchantData;
        private boolean compatible = false;
        private boolean resultSet = false;
        private String reason = "";

        private DebugInfo(ItemStack left, ItemStack right, ItemUtil.EnchantData enchantData) {
            this.left = left;
            this.right = right;
            this.enchantData = enchantData;
        }
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

