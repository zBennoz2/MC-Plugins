package com.zbennoz.zbenadmintool.command;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import com.zbennoz.zbenadmintool.rank.RankPermission;
import net.kyori.adventure.text.Component;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class BanCommand implements CommandExecutor {

    private final ZBenAdmintool plugin;

    public BanCommand(ZBenAdmintool plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender)) {
            sender.sendMessage(plugin.getMessages().raw("no_permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("/ban <spieler> [grund]");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Verstoss gegen Regeln";
        Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), reason, null, sender.getName());
        if (target.isOnline()) {
            target.getPlayer().kick(Component.text("§cDu wurdest gebannt: " + reason));
        }
        sender.sendMessage("§a" + target.getName() + " wurde gebannt. Grund: " + reason);
        return true;
    }

    private boolean hasPermission(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        return plugin.getPermissionResolver().has(player, RankPermission.BAN);
    }
}
