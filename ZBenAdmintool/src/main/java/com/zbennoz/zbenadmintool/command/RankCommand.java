package com.zbennoz.zbenadmintool.command;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import com.zbennoz.zbenadmintool.rank.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RankCommand implements CommandExecutor {

    private final ZBenAdmintool plugin;

    public RankCommand(ZBenAdmintool plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur Spieler können das nutzen.");
            return true;
        }
        if (!plugin.getPermissionResolver().has(player, "zbenadmintool.rank")) {
            player.sendMessage(plugin.getMessages().raw("no_permission"));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage("/rank set <spieler> <rang> | remove <spieler> | create <rang> <farbe> <prio> | delete <rang> | list | perm add/remove <rang> <perm>");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "set" -> handleSet(player, args);
            case "remove" -> handleRemove(player, args);
            case "create" -> handleCreate(player, args);
            case "delete" -> handleDelete(player, args);
            case "list" -> handleList(player);
            case "perm" -> handlePerm(player, args);
            default -> player.sendMessage("Unbekannter Befehl.");
        }
        return true;
    }

    private void handleSet(Player sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("/rank set <spieler> <rang>");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String rank = args[2];
        if (plugin.getRankManager().getRank(rank) == null) {
            sender.sendMessage("Rang nicht gefunden.");
            return;
        }
        plugin.getRankManager().setPlayerRank(target.getUniqueId(), rank);
        sender.sendMessage("Rang gesetzt.");
    }

    private void handleRemove(Player sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("/rank remove <spieler>");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        plugin.getRankManager().removePlayerRank(target.getUniqueId());
        sender.sendMessage("Rang entfernt.");
    }

    private void handleCreate(Player sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("/rank create <rang> <farbe> <prio>");
            return;
        }
        String name = args[1];
        ChatColor color = ChatColor.valueOf(args[2].toUpperCase());
        int prio = Integer.parseInt(args[3]);
        plugin.getRankManager().createRank(name, color, prio, color + "[" + name + "] ", "");
        sender.sendMessage("Rang erstellt.");
    }

    private void handleDelete(Player sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("/rank delete <rang>");
            return;
        }
        plugin.getRankManager().deleteRank(args[1]);
        sender.sendMessage("Rang gelöscht.");
    }

    private void handleList(Player sender) {
        StringBuilder sb = new StringBuilder("Ränge: ");
        for (Rank rank : plugin.getRankManager().getRanks()) {
            sb.append(rank.getName()).append(" ");
        }
        sender.sendMessage(sb.toString());
    }

    private void handlePerm(Player sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("/rank perm <add|remove> <rang> <permission>");
            return;
        }
        String action = args[1];
        String rank = args[2];
        String perm = args[3];
        if (action.equalsIgnoreCase("add")) {
            plugin.getRankManager().addPermission(rank, perm);
            sender.sendMessage("Permission hinzugefügt.");
        } else if (action.equalsIgnoreCase("remove")) {
            plugin.getRankManager().removePermission(rank, perm);
            sender.sendMessage("Permission entfernt.");
        } else {
            sender.sendMessage("Unbekannte Aktion.");
        }
    }
}
