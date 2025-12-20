package com.zbennoz.zbenenchants.listener;

import com.zbennoz.zbenenchants.core.ZBenEnchantsPlugin;
import com.zbennoz.zbenenchants.enchant.CustomEnchant;
import com.zbennoz.zbenenchants.storage.PDCUtil;
import com.zbennoz.zbenenchants.util.CooldownManager;
import com.zbennoz.zbenenchants.util.ItemUtil;
import com.zbennoz.zbenenchants.util.MessageUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Listener für Block-Interaktionen und Abbau-Effekte.
 */
public class BlockListener implements Listener {

    private final ZBenEnchantsPlugin plugin;

    public BlockListener(ZBenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        handleExcavator(event, player, tool);
        handleLumberjack(event, player, tool);
        handleReplant(event, player, tool);
        handleTelekinesisAndSmelt(event, player, tool);
        handleLuckyFind(event, player, tool);
    }

    private void handleTelekinesisAndSmelt(BlockBreakEvent event, Player player, ItemStack tool) {
        int telekinesis = PDCUtil.getEnchantLevel(plugin, tool, CustomEnchant.TELEKINESIS);
        int smelt = PDCUtil.getEnchantLevel(plugin, tool, CustomEnchant.SMELT);
        if (telekinesis <= 0 && smelt <= 0) {
            return;
        }
        boolean disableWhileSneaking = plugin.getConfig().getBoolean("enchants.telekinesis.disable-while-sneaking", false);
        if (disableWhileSneaking && player.isSneaking()) {
            return;
        }
        Set<String> teleBlacklist = new HashSet<>(plugin.getConfig().getStringList("enchants.telekinesis.block-blacklist"));
        if (teleBlacklist.contains(event.getBlock().getType().name())) {
            return;
        }
        event.setDropItems(false);
        Collection<ItemStack> drops = event.getBlock().getDrops(tool, player);
        List<ItemStack> toGive = new ArrayList<>();
        for (ItemStack drop : drops) {
            ItemStack processed = drop;
            if (smelt > 0 && shouldSmelt(event.getBlock().getType())) {
                ItemStack smelted = getSmelted(drop.getType());
                if (smelted != null) {
                    processed = smelted;
                }
            }
            toGive.add(processed);
        }
        for (ItemStack stack : toGive) {
            HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(event.getBlock().getLocation(), leftover);
            }
        }
    }

    private boolean shouldSmelt(Material type) {
        List<String> whitelist = plugin.getConfig().getStringList("enchants.smelt.whitelist-blocks");
        List<String> blacklist = plugin.getConfig().getStringList("enchants.smelt.blacklist-blocks");
        String name = type.name();
        if (!whitelist.isEmpty() && !whitelist.contains(name)) {
            return false;
        }
        return !blacklist.contains(name);
    }

    private ItemStack getSmelted(Material input) {
        Iterator<Recipe> recipes = Bukkit.recipeIterator();
        while (recipes.hasNext()) {
            Recipe recipe = recipes.next();
            if (recipe instanceof FurnaceRecipe) {
                FurnaceRecipe furnace = (FurnaceRecipe) recipe;
                if (furnace.getInput().getType() == input) {
                    return new ItemStack(furnace.getResult().getType(), furnace.getResult().getAmount());
                }
            }
        }
        return null;
    }

    private void handleExcavator(BlockBreakEvent event, Player player, ItemStack tool) {
        int level = PDCUtil.getEnchantLevel(plugin, tool, CustomEnchant.EXCAVATOR);
        if (level <= 0) {
            return;
        }
        if (!player.isSneaking()) {
            return;
        }
        long cooldownMillis = plugin.getConfig().getLong("enchants.excavator.cooldown", 5000L);
        if (plugin.getCooldownManager(CustomEnchant.EXCAVATOR).isOnCooldown(player.getUniqueId())) {
            return;
        }
        plugin.getCooldownManager(CustomEnchant.EXCAVATOR).start(player.getUniqueId(), cooldownMillis);
        int radius = plugin.getConfig().getInt("enchants.excavator.radius", 1);
        int durabilityCost = plugin.getConfig().getInt("enchants.excavator.durability-multiplier", 2);
        int maxBlocks = plugin.getConfig().getInt("enchants.excavator.max-blocks", 9);
        Set<String> blacklist = new HashSet<>(plugin.getConfig().getStringList("enchants.excavator.blacklist"));

        List<Block> blocks = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (blocks.size() >= maxBlocks) {
                        break;
                    }
                    Block relative = event.getBlock().getRelative(x, y, z);
                    if (relative.equals(event.getBlock())) {
                        continue;
                    }
                    if (relative.isEmpty() || blacklist.contains(relative.getType().name())) {
                        continue;
                    }
                    if (relative.getState() instanceof org.bukkit.inventory.InventoryHolder) {
                        continue;
                    }
                    blocks.add(relative);
                }
            }
        }
        // breche Blöcke schonend
        for (Block block : blocks) {
            if (block.getType() == Material.AIR) {
                continue;
            }
            block.breakNaturally(tool, true);
        }
        damageTool(tool, durabilityCost * blocks.size());
    }

    private void handleReplant(BlockBreakEvent event, Player player, ItemStack tool) {
        int level = PDCUtil.getEnchantLevel(plugin, tool, CustomEnchant.REPLANT);
        if (level <= 0) {
            return;
        }
        Block block = event.getBlock();
        BlockState state = block.getState();
        if (!(state.getBlockData() instanceof Ageable)) {
            return;
        }
        Ageable ageable = (Ageable) state.getBlockData();
        if (ageable.getAge() < ageable.getMaximumAge()) {
            return;
        }
        Material seed = seedForCrop(block.getType());
        if (seed == null) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        if (!inventory.contains(seed)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (inventory.removeItem(new ItemStack(seed, 1)).isEmpty()) {
                block.setType(block.getType());
            }
        });
    }

    private Material seedForCrop(Material crop) {
        switch (crop) {
            case WHEAT:
                return Material.WHEAT_SEEDS;
            case CARROTS:
                return Material.CARROT;
            case POTATOES:
                return Material.POTATO;
            case BEETROOTS:
                return Material.BEETROOT_SEEDS;
            case NETHER_WART:
                if (plugin.getConfig().getBoolean("enchants.replant.include-nether-wart", true)) {
                    return Material.NETHER_WART;
                }
            default:
                return null;
        }
    }

    private void handleLumberjack(BlockBreakEvent event, Player player, ItemStack tool) {
        int level = PDCUtil.getEnchantLevel(plugin, tool, CustomEnchant.LUMBERJACK);
        if (level <= 0) {
            return;
        }
        Material type = event.getBlock().getType();
        if (!Tag.LOGS.isTagged(type)) {
            return;
        }
        long cooldown = plugin.getConfig().getLong("enchants.lumberjack.cooldown", 3000L);
        CooldownManager manager = plugin.getCooldownManager(CustomEnchant.LUMBERJACK);
        if (manager.isOnCooldown(player.getUniqueId())) {
            return;
        }
        manager.start(player.getUniqueId(), cooldown);
        int maxBlocks = plugin.getConfig().getInt("enchants.lumberjack.max-blocks", 16 + level * 4);
        int radius = plugin.getConfig().getInt("enchants.lumberjack.radius", 4);
        boolean naturalOnly = plugin.getConfig().getBoolean("enchants.lumberjack.natural-only", false);
        int durabilityCost = plugin.getConfig().getInt("enchants.lumberjack.durability-multiplier", 2);

        Queue<Block> queue = new ArrayDeque<>();
        Set<Block> visited = new HashSet<>();
        queue.add(event.getBlock());
        visited.add(event.getBlock());

        int broken = 0;
        while (!queue.isEmpty() && broken < maxBlocks) {
            Block current = queue.poll();
            if (!Tag.LOGS.isTagged(current.getType())) {
                continue;
            }
            if (naturalOnly && !hasLeavesNearby(current, radius)) {
                continue;
            }
            broken++;
            current.breakNaturally(tool, true);
            for (Block relative : getNearbyLogs(current, radius)) {
                if (!visited.contains(relative)) {
                    visited.add(relative);
                    queue.add(relative);
                }
            }
        }
        damageTool(tool, broken * durabilityCost);
    }

    private boolean hasLeavesNearby(Block block, int radius) {
        int r = Math.max(1, radius / 2);
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    Block relative = block.getRelative(x, y, z);
                    if (Tag.LEAVES.isTagged(relative.getType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<Block> getNearbyLogs(Block center, int radius) {
        List<Block> result = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block relative = center.getRelative(x, y, z);
                    if (center.getLocation().distanceSquared(relative.getLocation()) <= radius * radius) {
                        result.add(relative);
                    }
                }
            }
        }
        return result;
    }

    private void handleLuckyFind(BlockBreakEvent event, Player player, ItemStack tool) {
        int level = PDCUtil.getEnchantLevel(plugin, tool, CustomEnchant.LUCKY_FIND);
        if (level <= 0) {
            return;
        }
        double baseChance = plugin.getConfig().getDouble("enchants.luckyfind.base-chance", 0.05);
        double perLevel = plugin.getConfig().getDouble("enchants.luckyfind.per-level", 0.02);
        double chance = baseChance + perLevel * (level - 1);
        List<String> blacklist = plugin.getConfig().getStringList("enchants.luckyfind.blacklist");
        if (blacklist.contains(event.getBlock().getType().name())) {
            return;
        }
        if (Math.random() <= chance) {
            Collection<ItemStack> drops = event.getBlock().getDrops(tool, player);
            drops.stream().findFirst().ifPresent(drop -> {
                ItemStack bonus = drop.clone();
                bonus.setAmount(1);
                player.getWorld().dropItemNaturally(event.getBlock().getLocation(), bonus);
            });
        }
    }

    private void damageTool(ItemStack tool, int amount) {
        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable)) {
            return;
        }
        Damageable damageable = (Damageable) meta;
        damageable.setDamage(damageable.getDamage() + amount);
        tool.setItemMeta((ItemMeta) damageable);
    }
}
