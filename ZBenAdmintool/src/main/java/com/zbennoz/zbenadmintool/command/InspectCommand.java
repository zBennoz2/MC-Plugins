package com.zbennoz.zbenadmintool.command;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import com.zbennoz.zbenadmintool.rank.RankPermission;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InspectCommand implements CommandExecutor {

    private final ZBenAdmintool plugin;

    public InspectCommand(ZBenAdmintool plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur Spieler können das nutzen.");
            return true;
        }
        if (!plugin.getPermissionResolver().has(player, RankPermission.INSPECT)) {
            player.sendMessage(plugin.getMessages().raw("no_permission"));
            return true;
        }
        plugin.getInspectorListener().toggleInspector(player);
        return true;
    }
}
