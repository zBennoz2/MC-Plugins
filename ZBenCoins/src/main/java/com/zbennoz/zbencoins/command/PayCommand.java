package com.zbennoz.zbencoins.command;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.database.PlayerRecord;
import com.zbennoz.zbencoins.service.CoinService;
import com.zbennoz.zbencoins.service.PlayerService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * /pay Befehl.
 */
public class PayCommand implements CommandExecutor {

    private final ZBenCoinsPlugin plugin;
    private final CoinService coinService;
    private final PlayerService playerService;

    public PayCommand(ZBenCoinsPlugin plugin, CoinService coinService, PlayerService playerService) {
        this.plugin = plugin;
        this.coinService = coinService;
        this.playerService = playerService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().message("player-only"));
            return true;
        }
        if (!player.hasPermission("zbencoins.pay.use")) {
            player.sendMessage(plugin.getConfigManager().message("no-permission"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().message("pay-usage"));
            return true;
        }

        PlayerRecord targetRecord = playerService.findByName(args[0]);
        if (targetRecord == null) {
            OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(args[0]);
            if (offline != null) {
                playerService.ensurePlayer(offline.getUniqueId(), offline.getName());
                targetRecord = playerService.findByName(args[0]);
            }
        }

        if (targetRecord == null) {
            player.sendMessage(plugin.getConfigManager().message("unknown-player"));
            return true;
        }
        if (targetRecord.getUuid().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().message("self-pay"));
            return true;
        }

        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getConfigManager().message("invalid-amount"));
            return true;
        }
        if (amount < plugin.getConfig().getLong("pay.min-amount", 1)) {
            player.sendMessage(plugin.getConfigManager().message("invalid-amount"));
            return true;
        }

        long balance = coinService.getBalance(player.getUniqueId());
        if (balance < amount) {
            player.sendMessage(plugin.getConfigManager().message("not-enough-coins"));
            return true;
        }

        if (coinService.transfer(player.getUniqueId(), targetRecord.getUuid(), amount)) {
            player.sendMessage(plugin.getConfigManager().message("pay-success-sender", Map.of(
                    "player", targetRecord.getName(),
                    "amount", String.valueOf(amount),
                    "currency", plugin.getConfig().getString("currency-name", "Coins")
            )));
            Player targetOnline = Bukkit.getPlayer(targetRecord.getUuid());
            if (targetOnline != null && targetOnline.isOnline()) {
                targetOnline.sendMessage(plugin.getConfigManager().message("pay-success-target", Map.of(
                        "player", player.getName(),
                        "amount", String.valueOf(amount),
                        "currency", plugin.getConfig().getString("currency-name", "Coins")
                )));
            }
        }
        return true;
    }
}
