package com.zbennoz.zbenclaims.listeners;

import com.zbennoz.zbenclaims.Claim;
import com.zbennoz.zbenclaims.ZBenClaimsPlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.Optional;

public class ProtectionListener implements Listener {

    private final ZBenClaimsPlugin plugin;

    public ProtectionListener(ZBenClaimsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean bypass(Player p) {
        return p.hasPermission("zbenclaims.admin.bypass");
    }

    private Optional<Claim> claimAt(Block b) {
        if (b == null) return Optional.empty();
        return plugin.getClaimService().getClaim(b.getWorld(), b.getChunk().getX(), b.getChunk().getZ());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (bypass(p)) return;

        var claimOpt = claimAt(e.getBlock());
        if (claimOpt.isEmpty()) return;

        Claim claim = claimOpt.get();
        if (plugin.getClaimService().isMember(claim, p.getUniqueId())) return;

        e.setCancelled(true);
        plugin.getMessages().sendProtection(p, "protectedBreak");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (bypass(p)) return;

        var claimOpt = claimAt(e.getBlockPlaced());
        if (claimOpt.isEmpty()) return;

        Claim claim = claimOpt.get();
        if (plugin.getClaimService().isMember(claim, p.getUniqueId())) return;

        e.setCancelled(true);
        plugin.getMessages().sendProtection(p, "protectedPlace");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (bypass(p)) return;

        InventoryHolder holder = e.getInventory().getHolder();
        if (!(holder instanceof Container)) return;

        Block b = ((Container) holder).getBlock();
        var claimOpt = claimAt(b);
        if (claimOpt.isEmpty()) return;

        Claim claim = claimOpt.get();
        if (plugin.getClaimService().isMember(claim, p.getUniqueId())) return;

        e.setCancelled(true);
        plugin.getMessages().sendProtection(p, "protectedOpen");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        Player p = e.getPlayer();
        if (bypass(p)) return;

        Block b = e.getClickedBlock();
        var claimOpt = claimAt(b);
        if (claimOpt.isEmpty()) return;

        Claim claim = claimOpt.get();
        if (plugin.getClaimService().isMember(claim, p.getUniqueId())) return;

        if (isContainerBlock(b)) {
            e.setCancelled(true);
            plugin.getMessages().sendProtection(p, "protectedOpen");
            return;
        }

        boolean interactProtectedDefault = plugin.getConfig().getBoolean("flags.default.interact_protected", false);
        boolean interactProtected = plugin.getClaimService().getFlag(claim.id(), "interact_protected", interactProtectedDefault);

        if (interactProtected && isRedstoneInteractable(b.getType())) {
            e.setCancelled(true);
            plugin.getMessages().sendProtection(p, "protectedInteract");
        }
    }

    private boolean isContainerBlock(Block b) {
        BlockState state = b.getState(false);
        return state instanceof Container || state instanceof InventoryHolder;
    }

    private boolean isRedstoneInteractable(Material m) {
        String name = m.name();
        return name.endsWith("_DOOR")
                || name.endsWith("_TRAPDOOR")
                || name.endsWith("_FENCE_GATE")
                || name.endsWith("_BUTTON")
                || name.endsWith("_PRESSURE_PLATE")
                || name.equals("LEVER");
    }
}
