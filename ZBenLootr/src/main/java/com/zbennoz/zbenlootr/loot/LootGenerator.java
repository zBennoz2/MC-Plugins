package com.zbennoz.zbenlootr.loot;

import com.zbennoz.zbenlootr.ZBenLootrPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;

import java.util.Random;

public final class LootGenerator {

    private LootGenerator() {}

    public static void fillInventory(ZBenLootrPlugin plugin, Inventory inventory, Player player, Location location) {
        String mode = plugin.getLootMode();
        if ("VANILLA_LOOTTABLE".equalsIgnoreCase(mode)) {
            fillFromVanilla(plugin, inventory, player, location);
        }
    }

    private static void fillFromVanilla(ZBenLootrPlugin plugin, Inventory inventory, Player player, Location location) {
        String tableKey = plugin.getVanillaLootTable();
        NamespacedKey key = NamespacedKey.fromString(tableKey);
        if (key == null) {
            plugin.getLogger().warning("Invalid loot table key: " + tableKey);
            return;
        }
        LootTable table = Bukkit.getLootTable(key);
        if (table == null) {
            plugin.getLogger().warning("Loot table not found: " + key);
            return;
        }
        LootContext.Builder builder = new LootContext.Builder(location);
        builder.luck(player.getLuck());
        Random random = createRandom(plugin, player, location);
        for (ItemStack item : table.populateLoot(random, builder.build())) {
            inventory.addItem(item);
        }
    }

    private static Random createRandom(ZBenLootrPlugin plugin, Player player, Location location) {
        String seedMode = plugin.getSeedMode();
        if ("PER_CONTAINER".equalsIgnoreCase(seedMode)) {
            long seed = location.getBlockX() * 31L + location.getBlockY() * 17L + location.getBlockZ() * 13L;
            seed ^= location.getWorld().getUID().getLeastSignificantBits();
            return new Random(seed);
        }
        long seed = player.getUniqueId().getMostSignificantBits() ^ player.getUniqueId().getLeastSignificantBits();
        seed ^= System.nanoTime();
        return new Random(seed);
    }
}
