package com.zbennoz.zbenlootr.commands;

import com.zbennoz.zbenlootr.ZBenLootrPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZBenLootrCommand implements CommandExecutor, TabCompleter {

    private final ZBenLootrPlugin plugin;

    public ZBenLootrCommand(ZBenLootrPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("zbenlootr.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadPlugin();
                sender.sendMessage(ChatColor.GREEN + "ZBenLootr configuration reloaded.");
            }
            case "info" -> {
                sender.sendMessage(ChatColor.GOLD + "ZBenLootr v" + plugin.getDescription().getVersion());
                sender.sendMessage(ChatColor.GRAY + "Storage: " + plugin.getConfig().getString("storage", "SQLITE"));
                sender.sendMessage(ChatColor.GRAY + "Cache size: " + plugin.getLootCache().toString());
            }
            default -> sendUsage(sender);
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Usage: /zbenlootr <reload|info>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "info");
        }
        return new ArrayList<>();
    }
}
