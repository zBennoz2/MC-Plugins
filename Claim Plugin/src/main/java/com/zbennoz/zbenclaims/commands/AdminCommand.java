package com.zbennoz.zbenclaims.commands;

import com.zbennoz.zbenclaims.ZBenClaimsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AdminCommand extends BaseCommand implements CommandExecutor {

    public AdminCommand(ZBenClaimsPlugin plugin) { super(plugin); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!ensurePerm(sender, "zbenclaims.admin")) return true;

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadAll();
            plugin.getMessages().send(sender, "reload");
            return true;
        }
        return false;
    }
}
