package com.zbennoz.zbenenchants.listener;

import com.zbennoz.zbenenchants.core.ZBenEnchantsPlugin;
import com.zbennoz.zbenenchants.enchant.CustomEnchant;
import com.zbennoz.zbenenchants.storage.PDCUtil;
import com.zbennoz.zbenenchants.util.CooldownManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

/**
 * Schadensbezogene Effekte wie Guardian, Stability, Second Wind.
 */
public class DamageListener implements Listener {

    private final ZBenEnchantsPlugin plugin;

    public DamageListener(ZBenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        handleGuardian(event, player);
        handleSecondWind(event, player);
        registerCombat(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            registerCombat(((Player) event.getDamager()).getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVelocity(PlayerVelocityEvent event) {
        Player player = event.getPlayer();
        ItemStack boots = player.getInventory().getBoots();
        int stability = PDCUtil.getEnchantLevel(plugin, boots, CustomEnchant.STABILITY);
        if (stability <= 0) {
            return;
        }
        double reduction = plugin.getConfig().getDouble("enchants.stability.reduction-per-level", 0.1) * stability;
        reduction = Math.min(reduction, plugin.getConfig().getDouble("enchants.stability.max-reduction", 0.6));
        event.setVelocity(event.getVelocity().multiply(1 - reduction));
    }

    private void handleGuardian(EntityDamageEvent event, Player player) {
        ItemStack chest = player.getInventory().getChestplate();
        int level = PDCUtil.getEnchantLevel(plugin, chest, CustomEnchant.GUARDIAN);
        if (level <= 0) {
            return;
        }
        CooldownManager manager = plugin.getCooldownManager(CustomEnchant.GUARDIAN);
        if (manager.isOnCooldown(player.getUniqueId())) {
            return;
        }
        double chance = plugin.getConfig().getDouble("enchants.guardian.base-chance", 0.05)
                + plugin.getConfig().getDouble("enchants.guardian.per-level", 0.025) * (level - 1);
        if (Math.random() <= chance) {
            int duration = plugin.getConfig().getInt("enchants.guardian.duration-ticks", 80);
            int amplifier = Math.max(0, plugin.getConfig().getInt("enchants.guardian.amplifier", 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, duration, amplifier, false, true));
            manager.start(player.getUniqueId(), plugin.getConfig().getLong("enchants.guardian.cooldown", 10000L));
        }
    }

    private void handleSecondWind(EntityDamageEvent event, Player player) {
        ItemStack chest = player.getInventory().getChestplate();
        int level = PDCUtil.getEnchantLevel(plugin, chest, CustomEnchant.SECOND_WIND);
        if (level <= 0) {
            return;
        }
        CooldownManager manager = plugin.getCooldownManager(CustomEnchant.SECOND_WIND);
        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        double threshold = maxHealth * plugin.getConfig().getDouble("enchants.secondwind.health-threshold", 0.2);
        double finalHealth = player.getHealth() - event.getFinalDamage();
        if (finalHealth >= threshold) {
            return;
        }
        if (manager.isOnCooldown(player.getUniqueId())) {
            return;
        }
        int regenDuration = plugin.getConfig().getInt("enchants.secondwind.regen-duration", 100);
        int speedDuration = plugin.getConfig().getInt("enchants.secondwind.speed-duration", 60);
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, regenDuration, 0, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, speedDuration, 0, false, true));
        manager.start(player.getUniqueId(), plugin.getConfig().getLong("enchants.secondwind.cooldown", 60000L));
    }

    private void registerCombat(UUID playerId) {
        long combatMs = plugin.getConfig().getLong("general.combat-tag-ms", 8000L);
        plugin.getCombatCooldown().start(playerId, combatMs);
    }
}
