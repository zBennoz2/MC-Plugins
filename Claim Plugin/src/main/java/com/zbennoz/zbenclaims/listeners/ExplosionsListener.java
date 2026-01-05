package com.zbennoz.zbenclaims.listeners;

import com.zbennoz.zbenclaims.ClaimService;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class ExplosionsListener implements Listener {

    private final ClaimService claimService;
    private final org.bukkit.plugin.Plugin plugin;

    public ExplosionsListener(org.bukkit.plugin.Plugin plugin, ClaimService claimService) {
        this.plugin = plugin;
        this.claimService = claimService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (entity == null || entity.getType() != EntityType.CREEPER) return;

        Location loc = event.getLocation();
        if (!isClaimed(loc)) return;

        boolean cancelExplosion = plugin.getConfig().getBoolean("explosions.creeper.cancelExplosion", false);
        boolean preventBlocks = plugin.getConfig().getBoolean("explosions.creeper.preventBlockDamageInClaims", true);

        if (cancelExplosion) {
            event.setCancelled(true);
            return;
        }

        if (preventBlocks) {
            event.blockList().clear();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        boolean enabled = plugin.getConfig().getBoolean("explosions.alsoProtectAgainstOtherExplosions.enabled", false);
        if (!enabled) return;

        Location loc = event.getBlock().getLocation();
        if (!isClaimed(loc)) return;

        boolean cancelExplosion = plugin.getConfig().getBoolean("explosions.alsoProtectAgainstOtherExplosions.cancelExplosion", false);
        if (cancelExplosion) {
            event.setCancelled(true);
            return;
        }

        event.blockList().clear();
    }

    private boolean isClaimed(Location loc) {
        Chunk chunk = loc.getChunk();
        return claimService.isChunkClaimed(loc.getWorld(), chunk.getX(), chunk.getZ());
    }
}
