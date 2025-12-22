package com.zbennoz.zbencityjobs.commands;

import com.zbennoz.zbencityjobs.gui.MarketGUI;
import com.zbennoz.zbencityjobs.service.MarketService;
import com.zbennoz.zbencityjobs.service.CoinService;
import com.zbennoz.zbencityjobs.util.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class MarketCommand implements CommandExecutor {
    private final MarketGUI marketGUI;
    private final MarketService marketService;
    private final CoinService coinService;
    private final MessageService messages;

    public MarketCommand(MarketGUI marketGUI, MarketService marketService, CoinService coinService, MessageService messages) {
        this.marketGUI = marketGUI;
        this.marketService = marketService;
        this.coinService = coinService;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only");
            return true;
        }

        if (args.length == 0) {
            marketGUI.open(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("sell") && args.length >= 2) {
            long price = parseAmount(args[1]);
            if (price <= 0) {
                player.sendMessage(messages.get("errors.invalid-amount", Map.of("max", String.valueOf(coinService.getMaxAmount()))));
                return true;
            }
            ItemStack inHand = player.getInventory().getItemInMainHand();
            if (inHand == null || inHand.getType().isAir()) {
                return false;
            }
            ItemStack copy = inHand.clone();
            marketService.createListing(player, copy, price).ifPresent(listing -> {
                player.getInventory().setItemInMainHand(null);
                player.sendMessage(messages.get("info.listing-created", Map.of("id", String.valueOf(listing.getId()))));
            });
            return true;
        }

        return false;
    }

    private long parseAmount(String raw) {
        try {
            long value = Long.parseLong(raw);
            if (value <= 0) return -1;
            return Math.min(value, coinService.getMaxAmount());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
