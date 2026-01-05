package com.zbennoz.zbenadmintool.command;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import com.zbennoz.zbenadmintool.rank.RankPermission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class WarnCommand implements CommandExecutor {

    private final ZBenAdmintool plugin;

    public WarnCommand(ZBenAdmintool plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender)) {
            sender.sendMessage(plugin.getMessages().raw("no_permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("/warn <spieler> <grund>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        plugin.getModerationService().addWarning(target.getUniqueId(), sender.getName(), reason);
        if (target.isOnline()) {
            target.getPlayer().sendMessage("§eVerwarnung: " + reason);
        }
        sender.sendMessage("§aVerwarnung für " + target.getName() + " gespeichert.");
        return true;
    }

    private boolean hasPermission(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        return plugin.getPermissionResolver().has(player, RankPermission.WARN);
    }
}
