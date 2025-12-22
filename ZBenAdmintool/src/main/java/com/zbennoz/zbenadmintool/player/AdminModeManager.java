package com.zbennoz.zbenadmintool.player;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import com.zbennoz.zbenadmintool.text.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

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
        AdminState state = new AdminState(player.getGameMode(), player.getAllowFlight());
        states.put(player.getUniqueId(), state);
        player.setGameMode(GameMode.CREATIVE);
        if (plugin.getConfig().getBoolean("adminmode.enableFly", true)) {
            player.setAllowFlight(true);
        }
        vanishManager.setVanish(player, true);
        player.sendActionBar(stateMessage(true));
    }

    public void disable(Player player) {
        AdminState state = states.remove(player.getUniqueId());
        if (state != null) {
            player.setGameMode(state.previousMode());
            player.setAllowFlight(state.hadFlight());
        }
        vanishManager.setVanish(player, false);
        player.sendActionBar(stateMessage(false));
    }

    public void disableAll() {
        Bukkit.getOnlinePlayers().forEach(this::disable);
    }

    private String stateMessage(boolean enabled) {
        return messages.raw(enabled ? "adminmode.enabled" : "adminmode.disabled");
    }

    private record AdminState(GameMode previousMode, boolean hadFlight) {}
}
