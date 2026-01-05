package com.zbennoz.zbenclaims.commands;

import com.zbennoz.zbenclaims.ZBenClaimsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClaimBordersCommand extends BaseCommand implements CommandExecutor {

    public ClaimBordersCommand(ZBenClaimsPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!ensurePlayer(sender)) return true;
        if (!ensurePerm(sender, "claim.borders.toggle")) return true;

        if (!plugin.getConfig().getBoolean("borders.enabled", true)) {
            plugin.getMessages().send(sender, "borders.disabled");
            return true;
        }

        Player player = (Player) sender;
        boolean enabled = plugin.getBordersService().toggle(player);
        if (enabled) {
            plugin.getMessages().send(sender, "borders.toggledOn");
        } else {
            plugin.getMessages().send(sender, "borders.toggledOff");
        }
        return true;
    }
}
