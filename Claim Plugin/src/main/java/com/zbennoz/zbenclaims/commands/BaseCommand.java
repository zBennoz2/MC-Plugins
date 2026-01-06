package com.zbennoz.zbenclaims.commands;

import com.zbennoz.zbenclaims.ZBenClaimsPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class BaseCommand {
    protected final ZBenClaimsPlugin plugin;

    protected BaseCommand(ZBenClaimsPlugin plugin) {
        this.plugin = plugin;
    }

    protected boolean ensurePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            plugin.getMessages().send(sender, "playerOnly");
            return false;
        }
        return true;
    }

    protected boolean ensurePerm(CommandSender sender, String perm) {
        if (!sender.hasPermission(perm)) {
            plugin.getMessages().send(sender, "noPermission");
            return false;
        }
        return true;
    }

    protected boolean ensureAdmin(CommandSender sender) {
        if (sender.isOp() || sender.hasPermission("claim.admin")
                || sender.hasPermission("zbenclaims.admin")) {
            return true;
        }
        plugin.getMessages().send(sender, "noPermission");
        return false;
    }
}
