package com.zbennoz.zbenadmintool.command;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import com.zbennoz.zbenadmintool.gui.AdminMenuListener;
import com.zbennoz.zbenadmintool.permission.PermissionResolver;
import com.zbennoz.zbenadmintool.rank.RankPermission;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminCommand implements CommandExecutor {

    private final ZBenAdmintool plugin;

    public AdminCommand(ZBenAdmintool plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur Spieler können das nutzen.");
            return true;
        }
        PermissionResolver resolver = plugin.getPermissionResolver();
        if (!resolver.has(player, RankPermission.ADMIN_MENU)) {
            player.sendMessage(plugin.getMessages().raw("no_permission"));
            return true;
        }
        AdminMenuListener.openMenu(plugin, player);
        return true;
    }
}
