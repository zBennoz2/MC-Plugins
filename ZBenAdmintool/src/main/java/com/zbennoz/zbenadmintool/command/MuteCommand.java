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

public class MuteCommand implements CommandExecutor {

    private final ZBenAdmintool plugin;

    public MuteCommand(ZBenAdmintool plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender)) {
            sender.sendMessage(plugin.getMessages().raw("no_permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("/mute <spieler> [grund] - erneut für Entmute");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Stummgeschaltet";
        if (plugin.getModerationService().isMuted(target.getUniqueId())) {
            plugin.getModerationService().unmute(target.getUniqueId());
            sender.sendMessage("§a" + target.getName() + " wurde entmutet.");
        } else {
            plugin.getModerationService().mute(target.getUniqueId(), reason);
            sender.sendMessage("§c" + target.getName() + " wurde stummgeschaltet.");
            if (target.isOnline()) {
                target.getPlayer().sendMessage("§cDu bist stummgeschaltet: " + reason);
            }
        }
        return true;
    }

    private boolean hasPermission(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        return plugin.getPermissionResolver().has(player, RankPermission.MUTE);
    }
}
