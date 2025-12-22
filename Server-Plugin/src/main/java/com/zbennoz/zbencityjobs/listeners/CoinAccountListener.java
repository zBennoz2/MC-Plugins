package com.zbennoz.zbencityjobs.listeners;

import com.zbennoz.zbencityjobs.service.CoinService;
import com.zbennoz.zbencityjobs.util.DisplayManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class CoinAccountListener implements Listener {
    private final CoinService coinService;
    private final DisplayManager displayManager;

    public CoinAccountListener(CoinService coinService, DisplayManager displayManager) {
        this.coinService = coinService;
        this.displayManager = displayManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        coinService.loadAccount(uuid);
        displayManager.updateDisplays(event.getPlayer());
    }
}
