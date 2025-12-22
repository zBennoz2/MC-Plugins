package com.zbennoz.zbencityjobs.listeners;

import com.zbennoz.zbencityjobs.service.CoinService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class CoinAccountListener implements Listener {
    private final CoinService coinService;

    public CoinAccountListener(CoinService coinService) {
        this.coinService = coinService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        coinService.loadAccount(uuid);
    }
}
