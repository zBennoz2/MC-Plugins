package com.zbennoz.zbentimber.listener;

import com.zbennoz.zbentimber.ZBenTimberPlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class TimberListener implements Listener {

    private final ZBenTimberPlugin plugin;

    public TimberListener(ZBenTimberPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        Block start = event.getBlock();
        Material type = start.getType();
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        int maxBlocks = plugin.getConfig().getInt("limits.max", 64);

        Set<Material> logs = Set.of(Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG, Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG);
        Set<Material> leaves = Set.of(Material.OAK_LEAVES, Material.SPRUCE_LEAVES, Material.BIRCH_LEAVES, Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES, Material.MANGROVE_LEAVES, Material.CHERRY_LEAVES, Material.AZALEA_LEAVES, Material.FLOWERING_AZALEA_LEAVES);
        Set<Material> ores = Set.of(Material.IRON_ORE, Material.GOLD_ORE, Material.DIAMOND_ORE, Material.REDSTONE_ORE, Material.COAL_ORE, Material.LAPIS_ORE, Material.COPPER_ORE, Material.EMERALD_ORE, Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE, Material.DEEPSLATE_IRON_ORE, Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_REDSTONE_ORE, Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_LAPIS_ORE, Material.DEEPSLATE_COPPER_ORE, Material.DEEPSLATE_EMERALD_ORE);

        if (logs.contains(type) || leaves.contains(type)) {
            if (!event.getPlayer().hasPermission("zbentimber.tree")) return;
            breakCluster(start, maxBlocks, logs, leaves);
        } else if (ores.contains(type)) {
            if (!event.getPlayer().hasPermission("zbentimber.ore")) return;
            breakCluster(start, maxBlocks, ores, ores);
        } else if ((type == Material.COBBLESTONE || type == Material.DEEPSLATE) && plugin.getConfig().getBoolean("stone-vein", false)) {
            if (!event.getPlayer().hasPermission("zbentimber.stone")) return;
            breakCluster(start, maxBlocks, Set.of(Material.COBBLESTONE, Material.DEEPSLATE), Set.of(Material.COBBLESTONE, Material.DEEPSLATE));
        }

        // Simple durability cost
        if (tool.getItemMeta() instanceof Damageable damageable) {
            damageable.setDamage(damageable.getDamage() + 1);
            tool.setItemMeta(damageable);
        }
    }

    private void breakCluster(Block start, int maxBlocks, Set<Material> targets, Set<Material> additional) {
        ArrayDeque<Block> queue = new ArrayDeque<>();
        Set<Block> visited = new HashSet<>();
        queue.add(start);
        while (!queue.isEmpty() && visited.size() < maxBlocks) {
            Block block = queue.poll();
            if (block == null || visited.contains(block)) continue;
            if (!(targets.contains(block.getType()) || additional.contains(block.getType()))) continue;
            visited.add(block);
            block.breakNaturally();
            for (var face : org.bukkit.block.BlockFace.values()) {
                if (face == org.bukkit.block.BlockFace.SELF) continue;
                Block relative = block.getRelative(face);
                queue.add(relative);
            }
        }
    }
}
