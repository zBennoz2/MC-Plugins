package com.zbennoz.zbencoins.listener;

import com.zbennoz.zbencoins.serveroffer.ServerOfferService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Chat-Liste für Admin-Eingaben bei Server-Angeboten.
 */
public class ServerOfferChatListener implements Listener {

    private final ServerOfferService service;

    public ServerOfferChatListener(ServerOfferService service) {
        this.service = service;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        boolean handled = service.handleChat(player, event.getMessage());
        if (handled) {
            event.setCancelled(true);
        }
    }
}
