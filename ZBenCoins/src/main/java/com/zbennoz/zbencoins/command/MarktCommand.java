package com.zbennoz.zbencoins.command;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.gui.MarktMainGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Öffnet das Markt-GUI.
 */
public class MarktCommand implements CommandExecutor {

    private final ZBenCoinsPlugin plugin;

    public MarktCommand(ZBenCoinsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().message("player-only"));
            return true;
        }
        if (!player.hasPermission("zbencoins.market.use")) {
            player.sendMessage(plugin.getConfigManager().message("no-permission"));
            return true;
        }
        plugin.getGuiManager().openGui(player, new MarktMainGui(plugin, plugin.getCoinService(), player));
        player.sendMessage(plugin.getConfigManager().message("market-open"));
        return true;
    }
}
