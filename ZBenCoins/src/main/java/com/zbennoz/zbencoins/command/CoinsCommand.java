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
 * /coins Befehl.
 */
public class CoinsCommand implements CommandExecutor {

    private final ZBenCoinsPlugin plugin;
    private final CoinService coinService;
    private final PlayerService playerService;

    public CoinsCommand(ZBenCoinsPlugin plugin, CoinService coinService, PlayerService playerService) {
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
        if (!player.hasPermission("zbencoins.coins.use")) {
            player.sendMessage(plugin.getConfigManager().message("no-permission"));
            return true;
        }

        if (args.length == 0) {
            long balance = coinService.getBalance(player.getUniqueId());
            player.sendMessage(plugin.getConfigManager().message("balance-self", Map.of(
                    "amount", String.valueOf(balance),
                    "currency", plugin.getConfig().getString("currency-name", "Coins")
            )));
            return true;
        }

        if (!player.hasPermission("zbencoins.coins.others")) {
            player.sendMessage(plugin.getConfigManager().message("no-permission"));
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

        player.sendMessage(plugin.getConfigManager().message("balance-other", Map.of(
                "player", targetRecord.getName(),
                "amount", String.valueOf(targetRecord.getCoins()),
                "currency", plugin.getConfig().getString("currency-name", "Coins")
        )));
        return true;
    }
}
