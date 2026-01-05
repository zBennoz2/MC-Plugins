package com.zbennoz.zbenadmintool.command;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import com.zbennoz.zbenadmintool.rank.RankPermission;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VanishCommand implements CommandExecutor {

    private final ZBenAdmintool plugin;

    public VanishCommand(ZBenAdmintool plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur Spieler können das nutzen.");
            return true;
        }
        if (!plugin.getPermissionResolver().has(player, RankPermission.VANISH)) {
            player.sendMessage(plugin.getMessages().raw("no_permission"));
            return true;
        }
        boolean enabled = plugin.getVanishManager().toggle(player);
        player.sendMessage(plugin.getMessages().raw(enabled ? "vanish.enabled" : "vanish.disabled"));
        return true;
    }
}
