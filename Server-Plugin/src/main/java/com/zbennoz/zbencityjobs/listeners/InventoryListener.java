package com.zbennoz.zbencityjobs.listeners;

import com.zbennoz.zbencityjobs.gui.JobBoardGUI;
import com.zbennoz.zbencityjobs.gui.MarketGUI;
import com.zbennoz.zbencityjobs.service.CoinService;
import com.zbennoz.zbencityjobs.service.JobService;
import com.zbennoz.zbencityjobs.service.MarketService;
import com.zbennoz.zbencityjobs.util.MessageService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.entity.Player;

public class InventoryListener implements Listener {
    private final JobBoardGUI jobBoardGUI;
    private final MarketGUI marketGUI;
    private final JobService jobService;
    private final MarketService marketService;
    private final CoinService coinService;
    private final MessageService messages;

    public InventoryListener(JobBoardGUI jobBoardGUI, MarketGUI marketGUI, JobService jobService, MarketService marketService, CoinService coinService, MessageService messages) {
        this.jobBoardGUI = jobBoardGUI;
        this.marketGUI = marketGUI;
        this.jobService = jobService;
        this.marketService = marketService;
        this.coinService = coinService;
        this.messages = messages;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (title.equals("Jobs")) {
            event.setCancelled(true);
            jobBoardGUI.resolveJob(player, event.getSlot()).ifPresent(id -> {
                if (jobService.takeJob(id, player)) {
                    player.sendMessage(messages.get("info.job-taken", java.util.Map.of("id", String.valueOf(id))));
                }
            });
        }
        if (title.equals("Markt")) {
            event.setCancelled(true);
            marketGUI.resolveListing(player, event.getSlot()).ifPresent(id -> {
                marketService.getListing(id).ifPresent(listing -> {
                    if (marketService.purchase(id, player)) {
                        player.sendMessage(messages.get("info.listing-bought"));
                    } else {
                        player.sendMessage(messages.get("errors.not-enough-coins", java.util.Map.of(
                                "amount", coinService.formatAmount(listing.getPrice()),
                                "currency", coinService.getCurrencyName())));
                    }
                });
            });
        }
    }
}
