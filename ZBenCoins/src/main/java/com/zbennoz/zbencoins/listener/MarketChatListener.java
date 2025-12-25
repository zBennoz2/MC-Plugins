package com.zbennoz.zbencoins.listener;

import com.zbennoz.zbencoins.service.MarketService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Liest Preiseingaben aus dem Chat während der Angebotserstellung.
 */
public class MarketChatListener implements Listener {

    private final MarketService marketService;

    public MarketChatListener(MarketService marketService) {
        this.marketService = marketService;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        boolean handled = marketService.handleChat(player, event.getMessage());
        if (handled) {
            event.setCancelled(true);
        }
    }
}
