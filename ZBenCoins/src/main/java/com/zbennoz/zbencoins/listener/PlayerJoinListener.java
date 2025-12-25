package com.zbennoz.zbencoins.listener;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.database.PlayerRecord;
import com.zbennoz.zbencoins.service.MarketService;
import com.zbennoz.zbencoins.service.PlayerService;
import com.zbennoz.zbencoins.util.Text;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;

/**
 * Legt Spieler beim Join an und aktualisiert den Namen.
 */
public class PlayerJoinListener implements Listener {

    private final PlayerService playerService;
    private final ZBenCoinsPlugin plugin;
    private final MarketService marketService;

    public PlayerJoinListener(PlayerService playerService, MarketService marketService, ZBenCoinsPlugin plugin) {
        this.playerService = playerService;
        this.marketService = marketService;
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerRecord before = playerService.find(event.getPlayer().getUniqueId());
        PlayerRecord record = playerService.ensurePlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        if (before == null && record != null) {
            event.getPlayer().sendMessage(Text.format(plugin.getConfigManager().message("joined"), Map.of(
                    "amount", String.valueOf(record.getCoins())
            )));
        }
        marketService.deliverPending(event.getPlayer());
    }
}
