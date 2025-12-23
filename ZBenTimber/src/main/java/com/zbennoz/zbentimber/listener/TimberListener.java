package com.zbennoz.zbentimber.listener;

import com.zbennoz.zbentimber.PlayerSettingsStorage;
import com.zbennoz.zbentimber.ZBenTimberPlugin;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class TimberListener implements Listener {

    private final ZBenTimberPlugin plugin;
    private final PlayerSettingsStorage storage;

    private static final Set<Material> LOGS = EnumSet.of(Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG, Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG);
    private static final Set<Material> LEAVES = EnumSet.of(Material.OAK_LEAVES, Material.SPRUCE_LEAVES, Material.BIRCH_LEAVES, Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES, Material.MANGROVE_LEAVES, Material.CHERRY_LEAVES, Material.AZALEA_LEAVES, Material.FLOWERING_AZALEA_LEAVES);
    private static final Set<Material> ORES = EnumSet.of(Material.IRON_ORE, Material.GOLD_ORE, Material.DIAMOND_ORE, Material.REDSTONE_ORE, Material.COAL_ORE, Material.LAPIS_ORE, Material.COPPER_ORE, Material.EMERALD_ORE, Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE, Material.DEEPSLATE_IRON_ORE, Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_REDSTONE_ORE, Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_LAPIS_ORE, Material.DEEPSLATE_COPPER_ORE, Material.DEEPSLATE_EMERALD_ORE);

    public TimberListener(ZBenTimberPlugin plugin, PlayerSettingsStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        boolean requireSneak = plugin.getConfig().getBoolean("requireSneak", true);
        if (requireSneak && !player.isSneaking()) return;
        Block start = event.getBlock();
        Material type = start.getType();
        ItemStack tool = player.getInventory().getItemInMainHand();
        int maxBlocks = plugin.getConfig().getInt("limits.max", 64);
        boolean includeLeaves = storage.isLeavesEnabled(player.getUniqueId(), plugin.getLeavesDefault());

        Set<Material> optionalLeaves = includeLeaves ? LEAVES : Set.of();
        int broken = 0;

        if (LOGS.contains(type) || optionalLeaves.contains(type)) {
            if (!player.hasPermission("zbentimber.tree")) return;
            broken = breakCluster(player, start, maxBlocks, LOGS, optionalLeaves);
        } else if (ORES.contains(type)) {
            if (!player.hasPermission("zbentimber.ore")) return;
            broken = breakCluster(player, start, maxBlocks, ORES, Set.of());
        } else if ((type == Material.COBBLESTONE || type == Material.DEEPSLATE) && plugin.getConfig().getBoolean("stone-vein", false)) {
            if (!player.hasPermission("zbentimber.stone")) return;
            broken = breakCluster(player, start, maxBlocks, Set.of(Material.COBBLESTONE, Material.DEEPSLATE), Set.of());
        }

        if (broken > 0 && player.getGameMode() != GameMode.CREATIVE) {
            applyDurability(player, tool, broken);
        }
    }

    private int breakCluster(Player player, Block start, int maxBlocks, Set<Material> targets, Set<Material> additional) {
        ArrayDeque<Block> queue = new ArrayDeque<>();
        Set<Block> visited = new HashSet<>();
        queue.add(start);
        int broken = 0;
        while (!queue.isEmpty() && visited.size() < maxBlocks) {
            Block block = queue.poll();
            if (block == null || visited.contains(block)) continue;
            if (!(targets.contains(block.getType()) || additional.contains(block.getType()))) continue;
            visited.add(block);
            block.breakNaturally(player.getInventory().getItemInMainHand());
            broken++;
            for (var face : org.bukkit.block.BlockFace.values()) {
                if (face == org.bukkit.block.BlockFace.SELF) continue;
                Block relative = block.getRelative(face);
                queue.add(relative);
            }
        }
        return broken;
    }

    private void applyDurability(Player player, ItemStack tool, int blocks) {
        if (!(tool.getItemMeta() instanceof Damageable damageable)) {
            return;
        }
        int unbreaking = tool.getEnchantmentLevel(Enchantment.UNBREAKING);
        int damage = 0;
        for (int i = 0; i < blocks; i++) {
            if (shouldDamage(unbreaking)) {
                damage++;
            }
        }
        int newDamage = damageable.getDamage() + damage;
        int max = tool.getType().getMaxDurability();
        if (newDamage >= max) {
            tool.setAmount(0);
            player.getInventory().setItemInMainHand(null);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1f, 1f);
        } else {
            damageable.setDamage(newDamage);
            tool.setItemMeta(damageable);
        }
    }

    private boolean shouldDamage(int unbreakingLevel) {
        if (unbreakingLevel <= 0) {
            return true;
        }
        return ThreadLocalRandom.current().nextInt(unbreakingLevel + 1) == 0;
    }
}
