package com.zbennoz.zbenadmintool.player;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import com.zbennoz.zbenadmintool.text.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AdminModeManager {

    private final ZBenAdmintool plugin;
    private final VanishManager vanishManager;
    private final MessageService messages;
    private final Map<UUID, AdminState> states = new HashMap<>();

    public AdminModeManager(ZBenAdmintool plugin, VanishManager vanishManager, MessageService messages) {
        this.plugin = plugin;
        this.vanishManager = vanishManager;
        this.messages = messages;
    }

    public boolean toggle(Player player) {
        if (states.containsKey(player.getUniqueId())) {
            disable(player);
            return false;
        } else {
            enable(player);
            return true;
        }
    }

    public void enable(Player player) {
        AdminState state = new AdminState(player.getGameMode(), player.getAllowFlight(), player.isFlying(), player.getActivePotionEffects());
        states.put(player.getUniqueId(), state);
        player.setGameMode(GameMode.SPECTATOR);
        player.setAllowFlight(true);
        player.setFlying(true);
        if (plugin.getConfig().getBoolean("adminmode.nightVision", true)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, false));
        }
        if (plugin.getConfig().getBoolean("adminmode.invisibility", true)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
        }
        vanishManager.setVanish(player, true);
        player.sendActionBar(stateMessage(true));
        plugin.getLogger().info(player.getName() + " hat den Admin-Mode aktiviert.");
    }

    public void disable(Player player) {
        AdminState state = states.remove(player.getUniqueId());
        if (state != null) {
            player.setGameMode(state.previousMode());
            player.setAllowFlight(state.hadFlight());
            player.setFlying(state.hadFlight() && state.wasFlying());
            player.getActivePotionEffects().stream()
                    .map(PotionEffect::getType)
                    .filter(type -> type == PotionEffectType.NIGHT_VISION || type == PotionEffectType.INVISIBILITY)
                    .forEach(player::removePotionEffect);
            state.effects().forEach(player::addPotionEffect);
        }
        vanishManager.setVanish(player, false);
        player.sendActionBar(stateMessage(false));
        plugin.getLogger().info(player.getName() + " hat den Admin-Mode deaktiviert.");
    }

    public void disableAll() {
        Bukkit.getOnlinePlayers().forEach(this::disable);
    }

    private String stateMessage(boolean enabled) {
        return messages.raw(enabled ? "adminmode.enabled" : "adminmode.disabled");
    }

    private record AdminState(GameMode previousMode, boolean hadFlight, boolean wasFlying, Iterable<PotionEffect> effects) {}
}
