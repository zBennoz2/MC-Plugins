package com.zbennoz.zbencoins.command;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.database.PlayerRecord;
import com.zbennoz.zbencoins.service.CoinService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * /baltop Befehl.
 */
public class BaltopCommand implements CommandExecutor {

    private final ZBenCoinsPlugin plugin;
    private final CoinService coinService;

    public BaltopCommand(ZBenCoinsPlugin plugin, CoinService coinService) {
        this.plugin = plugin;
        this.coinService = coinService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().message("player-only"));
            return true;
        }
        if (!player.hasPermission("zbencoins.baltop.use")) {
            player.sendMessage(plugin.getConfigManager().message("no-permission"));
            return true;
        }

        List<PlayerRecord> top = coinService.topBalances(10);
        if (top.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().message("baltop-empty"));
            return true;
        }
        player.sendMessage(plugin.getConfigManager().message("baltop-header", Map.of(
                "count", String.valueOf(top.size())
        )));
        String currency = plugin.getConfig().getString("currency-name", "Coins");
        int position = 1;
        for (PlayerRecord record : top) {
            player.sendMessage(plugin.getConfigManager().message("baltop-entry", Map.of(
                    "position", String.valueOf(position++),
                    "player", record.getName(),
                    "amount", String.valueOf(record.getCoins()),
                    "currency", currency
            )));
        }
        return true;
    }
}
