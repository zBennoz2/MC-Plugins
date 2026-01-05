package com.zbennoz.zbenenchants.core;

import com.zbennoz.zbenenchants.command.ZBenEnchantsCommand;
import com.zbennoz.zbenenchants.enchant.CustomEnchant;
import com.zbennoz.zbenenchants.listener.BlockListener;
import com.zbennoz.zbenenchants.listener.DamageListener;
import com.zbennoz.zbenenchants.listener.EnchantingTableListener;
import com.zbennoz.zbenenchants.listener.InteractionListener;
import com.zbennoz.zbenenchants.listener.VillagerTradeListener;
import com.zbennoz.zbenenchants.listener.anvil.AnvilListener;
import com.zbennoz.zbenenchants.util.CooldownManager;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hauptklasse des Plugins. Registriert Befehle, Listener und stellt Hilfsmethoden bereit.
 */
public class ZBenEnchantsPlugin extends JavaPlugin {

    private final Map<CustomEnchant, CooldownManager> cooldowns = new EnumMap<>(CustomEnchant.class);
    private CooldownManager combatCooldown;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfiguration();

        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new EnchantingTableListener(this), this);
        getServer().getPluginManager().registerEvents(new AnvilListener(this), this);
        getServer().getPluginManager().registerEvents(new VillagerTradeListener(this), this);

        ZBenEnchantsCommand command = new ZBenEnchantsCommand(this);
        getCommand("zbenenchants").setExecutor(command);
        getCommand("zbenenchants").setTabCompleter(command);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        reloadConfiguration();
    }

    private void reloadConfiguration() {
        for (CustomEnchant enchant : CustomEnchant.values()) {
            cooldowns.put(enchant, new CooldownManager());
        }
        combatCooldown = new CooldownManager();
    }

    public CooldownManager getCooldownManager(CustomEnchant enchant) {
        return cooldowns.get(enchant);
    }

    public CooldownManager getCombatCooldown() {
        return combatCooldown;
    }

    public NamespacedKey getKey(String key) {
        return new NamespacedKey(this, key);
    }

    public NamespacedKey getEnchantKey(CustomEnchant enchant) {
        return getKey(enchant.getKey());
    }

    public String getMessage(String path) {
        return getConfig().getString("messages." + path, "");
    }

    public String getPrefix() {
        return getConfig().getString("messages.prefix", "[ZBenEnchants] ");
    }

    public boolean isZBenClaimsEnabled() {
        return getServer().getPluginManager().getPlugin("ZBenClaims") != null;
    }

    public boolean isOnCooldown(UUID playerId, CustomEnchant enchant) {
        return getCooldownManager(enchant).isOnCooldown(playerId);
    }
}
