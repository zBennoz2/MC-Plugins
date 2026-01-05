package com.zbennoz.zbenadmintool.command;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import com.zbennoz.zbenadmintool.rank.RankPermission;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class KickCommand implements CommandExecutor {

    private final ZBenAdmintool plugin;

    public KickCommand(ZBenAdmintool plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender)) {
            sender.sendMessage(plugin.getMessages().raw("no_permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("/kick <spieler> [grund]");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cSpieler ist nicht online.");
            return true;
        }
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Rausgeworfen";
        target.kick(Component.text("§cDu wurdest gekickt: " + reason));
        sender.sendMessage("§e" + target.getName() + " wurde gekickt.");
        return true;
    }

    private boolean hasPermission(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        return plugin.getPermissionResolver().has(player, RankPermission.KICK);
    }
}
