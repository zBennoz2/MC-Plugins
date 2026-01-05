package com.zbennoz.zbencoins.command;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.serveroffer.ServerOfferService;
import com.zbennoz.zbencoins.serveroffer.ServerOfferType;
import com.zbennoz.zbencoins.util.Text;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Schnelle Verwaltung per Command.
 */
public class ServerOfferCommand implements CommandExecutor {

    private final ZBenCoinsPlugin plugin;
    private final ServerOfferService service;

    public ServerOfferCommand(ZBenCoinsPlugin plugin, ServerOfferService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().message("player-only"));
            return true;
        }
        if (!player.hasPermission("zbencoins.admin.serveroffers")) {
            player.sendMessage(plugin.getConfigManager().message("server-offer-no-permission"));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(Text.colorize("&e/serveroffer create <buy|sell> <preis> [min] [max]"));
            player.sendMessage(Text.colorize("&e/serveroffer delete <id>"));
            player.sendMessage(Text.colorize("&e/serveroffer toggle <id>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player, args);
            case "delete" -> handleDelete(player, args);
            case "toggle" -> handleToggle(player, args);
            default -> player.sendMessage(Text.colorize("&cUnbekannter Unterbefehl."));
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Text.colorize("&cNutze: /serveroffer create <buy|sell> <preis> [min] [max]"));
            return;
        }
        ServerOfferType type = args[1].equalsIgnoreCase("buy") ? ServerOfferType.BUY_FROM_PLAYER : ServerOfferType.SELL_TO_PLAYER;
        long price;
        try {
            price = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getConfigManager().message("server-offer-invalid"));
            return;
        }
        Integer min = null;
        Integer max = null;
        try {
            if (args.length > 3) min = Integer.parseInt(args[3]);
            if (args.length > 4) max = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getConfigManager().message("server-offer-invalid"));
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            player.sendMessage(plugin.getConfigManager().message("server-offer-select-item"));
            return;
        }
        service.requestDraft(player, type);
        service.setDraftPrice(player, price);
        service.setDraftMin(player, min);
        service.setDraftMax(player, max);
        service.publishDraft(player);
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Text.colorize("&cNutze: /serveroffer delete <id>"));
            return;
        }
        try {
            int id = Integer.parseInt(args[1]);
            service.deleteOffer(id);
            player.sendMessage(plugin.getConfigManager().message("server-offer-deleted"));
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getConfigManager().message("server-offer-invalid"));
        }
    }

    private void handleToggle(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Text.colorize("&cNutze: /serveroffer toggle <id>"));
            return;
        }
        try {
            int id = Integer.parseInt(args[1]);
            service.toggleOffer(id);
            player.sendMessage(plugin.getConfigManager().message("server-offer-updated"));
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getConfigManager().message("server-offer-invalid"));
        }
    }
}
