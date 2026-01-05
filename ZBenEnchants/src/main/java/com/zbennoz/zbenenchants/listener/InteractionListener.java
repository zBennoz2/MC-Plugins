package com.zbennoz.zbenenchants.listener;

import com.zbennoz.zbenenchants.core.ZBenEnchantsPlugin;
import com.zbennoz.zbenenchants.enchant.CustomEnchant;
import com.zbennoz.zbenenchants.storage.PDCUtil;
import com.zbennoz.zbenenchants.util.CooldownManager;
import com.zbennoz.zbenenchants.util.ItemUtil;
import com.zbennoz.zbenenchants.util.MessageUtil;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Listener für rechte Klicks: Bücher anwenden, Grapple und Surveyor.
 */
public class InteractionListener implements Listener {

    private final ZBenEnchantsPlugin plugin;

    public InteractionListener(ZBenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();

        if (main.getType() == Material.ENCHANTED_BOOK) {
            tryApplyBook(event, player, main, off);
        } else if (off.getType() == Material.ENCHANTED_BOOK) {
            tryApplyBook(event, player, off, main);
        }

        handleGrapple(event, player);
        handleSurveyor(event, player);
    }

    private void tryApplyBook(PlayerInteractEvent event, Player player, ItemStack book, ItemStack target) {
        CustomEnchant enchant = ItemUtil.getEnchantFromBook(plugin, book);
        if (enchant == null) {
            return;
        }
        int level = PDCUtil.getEnchantLevel(plugin, book, enchant);
        if (target == null || target.getType() == Material.AIR || !enchant.isApplicable(target.getType())) {
            MessageUtil.send(plugin, player, "book-incompatible");
            return;
        }
        ItemUtil.applyEnchant(plugin, target, enchant, level);
        book.setAmount(book.getAmount() - 1);
        MessageUtil.send(plugin, player, "book-applied", enchant.getDisplayName());
        event.setCancelled(true);
    }

    private void handleGrapple(PlayerInteractEvent event, Player player) {
        ItemStack boots = player.getInventory().getBoots();
        int level = PDCUtil.getEnchantLevel(plugin, boots, CustomEnchant.GRAPPLE);
        if (level <= 0) {
            return;
        }
        boolean requireSneak = plugin.getConfig().getBoolean("enchants.grapple.require-sneak", false);
        if (requireSneak && !player.isSneaking()) {
            return;
        }
        CooldownManager manager = plugin.getCooldownManager(CustomEnchant.GRAPPLE);
        if (manager.isOnCooldown(player.getUniqueId())) {
            return;
        }
        if (plugin.getCombatCooldown().isOnCooldown(player.getUniqueId())
                && plugin.getConfig().getBoolean("enchants.grapple.block-during-combat", true)) {
            return;
        }
        double strength = plugin.getConfig().getDouble("enchants.grapple.strength", 1.0) + 0.2 * (level - 1);
        Vector direction = player.getLocation().getDirection().normalize().multiply(strength);
        direction.setY(Math.min(direction.getY() + 0.3, plugin.getConfig().getDouble("enchants.grapple.max-vertical", 0.8)));
        player.setVelocity(direction);
        manager.start(player.getUniqueId(), plugin.getConfig().getLong("enchants.grapple.cooldown", 6000L));
        event.setCancelled(true);
    }

    private void handleSurveyor(PlayerInteractEvent event, Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.COMPASS) {
            return;
        }
        if (!PDCUtil.hasEnchant(plugin, item, CustomEnchant.SURVEYOR)) {
            return;
        }
        if (!event.getAction().isRightClick()) {
            return;
        }
        Chunk chunk = player.getLocation().getChunk();
        Location loc = player.getLocation();
        StringBuilder sb = new StringBuilder();
        sb.append("Welt: ").append(loc.getWorld().getName()).append(" | Chunk: ").append(chunk.getX()).append(", ").append(chunk.getZ());
        sb.append(" | Block: ").append(loc.getBlockX()).append(", ").append(loc.getBlockY()).append(", ").append(loc.getBlockZ());
        if (plugin.isZBenClaimsEnabled()) {
            sb.append(" | Claim-Info: ").append(getClaimInfo(loc));
        }
        player.sendMessage(MessageUtil.parse(plugin, sb.toString()));
        event.setCancelled(true);
    }

    private String getClaimInfo(Location location) {
        try {
            // Soft Hook: falls ZBenClaims eine API bereitstellt, hier per Reflection abrufen.
            Class<?> apiClass = Class.forName("com.zbennoz.claims.api.ClaimsApi");
            Object api = apiClass.getMethod("get").invoke(null);
            Object claim = apiClass.getMethod("getClaimAt", Location.class).invoke(api, location);
            if (claim != null) {
                String owner = String.valueOf(claim.getClass().getMethod("getOwnerName").invoke(claim));
                String flags = String.valueOf(claim.getClass().getMethod("getFlagsInfo").invoke(claim));
                return owner + " | Flags: " + flags;
            }
        } catch (Exception ignored) {
            // Wenn kein Hook möglich ist, geben wir einen generischen Hinweis aus.
        }
        return "Keine Daten";
    }
}
