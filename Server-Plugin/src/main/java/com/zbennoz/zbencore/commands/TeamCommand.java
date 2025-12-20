package com.zbennoz.zbencore.commands;

import com.zbennoz.zbencore.teams.Team;
import com.zbennoz.zbencore.teams.TeamConversationManager;
import com.zbennoz.zbencore.teams.TeamService;
import com.zbennoz.zbencore.util.Msg;
import com.zbennoz.zbencore.util.Perm;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class TeamCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = List.of("create", "edit", "list", "reload", "set", "clear");

    private final JavaPlugin plugin;
    private final TeamService teamService;
    private final TeamConversationManager conversations;

    public TeamCommand(JavaPlugin plugin, TeamService teamService, TeamConversationManager conversations) {
        this.plugin = plugin;
        this.teamService = teamService;
        this.conversations = conversations;
    }

    private boolean canManage(CommandSender sender) {
        return sender.isOp() || Perm.has(sender, "zben.teams.manage");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Msg.pref(plugin, "&7Nutzung: /team <create|edit|list|reload|set|clear>"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> handleCreate(sender);
            case "edit" -> handleEdit(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            case "set" -> handleSet(sender, args);
            case "clear" -> handleClear(sender, args);
            default -> sender.sendMessage(Msg.pref(plugin, "&cUnbekannter Unterbefehl."));
        }
        return true;
    }

    private void handleCreate(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Msg.pref(plugin, plugin.getConfig().getString("messages.onlyPlayers")));
            return;
        }
        if (!canManage(player)) {
            sender.sendMessage(Msg.pref(plugin, plugin.getConfig().getString("messages.noPermission")));
            return;
        }
        conversations.startCreate(player);
    }

    private void handleEdit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Msg.pref(plugin, plugin.getConfig().getString("messages.onlyPlayers")));
            return;
        }
        if (!canManage(player)) {
            sender.sendMessage(Msg.pref(plugin, plugin.getConfig().getString("messages.noPermission")));
            return;
        }
        String key = args.length >= 2 ? args[1] : null;
        conversations.startEdit(player, key);
    }

    private void handleList(CommandSender sender) {
        if (!canManage(sender)) {
            sender.sendMessage(Msg.pref(plugin, plugin.getConfig().getString("messages.noPermission")));
            return;
        }
        List<Team> teams = teamService.listSorted();
        if (teams.isEmpty()) {
            sender.sendMessage(Msg.pref(plugin, "&7Keine Teams vorhanden."));
            return;
        }
        sender.sendMessage(Msg.pref(plugin, "&aTeams (&e" + teams.size() + "&a):"));
        for (Team team : teams) {
            sender.sendMessage(Msg.pref(plugin, "&e" + team.getKey() + " &7| Name: &f" + team.getDisplayName()
                    + " &7| Prefix: &f" + team.getPrefix() + " &7| Weight: &f" + team.getWeight()));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!canManage(sender)) {
            sender.sendMessage(Msg.pref(plugin, plugin.getConfig().getString("messages.noPermission")));
            return;
        }
        teamService.reload();
        sender.sendMessage(Msg.pref(plugin, "&aTeams wurden neu geladen."));
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!canManage(sender)) {
            sender.sendMessage(Msg.pref(plugin, plugin.getConfig().getString("messages.noPermission")));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Msg.pref(plugin, "&cNutzung: /team set <spieler> <teamKey>"));
            return;
        }

        String playerName = args[1];
        String teamKey = args[2];
        if (!teamService.exists(teamKey)) {
            sender.sendMessage(Msg.pref(plugin, "&cDieses Team gibt es nicht."));
            return;
        }

        OfflinePlayer target = plugin.getServer().getOfflinePlayer(playerName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(Msg.pref(plugin, plugin.getConfig().getString("messages.playerNotFound")));
            return;
        }

        teamService.setTeam(target.getUniqueId(), teamKey);
        if (target.isOnline()) {
            teamService.applyTeamDecorations(target.getPlayer());
        }
        sender.sendMessage(Msg.pref(plugin, "&a" + playerName + " ist jetzt im Team &e" + teamKey + "&a."));
    }

    private void handleClear(CommandSender sender, String[] args) {
        if (!canManage(sender)) {
            sender.sendMessage(Msg.pref(plugin, plugin.getConfig().getString("messages.noPermission")));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Msg.pref(plugin, "&cNutzung: /team clear <spieler>"));
            return;
        }

        String playerName = args[1];
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(playerName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(Msg.pref(plugin, plugin.getConfig().getString("messages.playerNotFound")));
            return;
        }

        teamService.clearTeam(target.getUniqueId());
        if (target.isOnline()) {
            teamService.applyTeamDecorations(target.getPlayer());
        }
        sender.sendMessage(Msg.pref(plugin, "&aTeam-Zuordnung für &e" + playerName + " &awurde entfernt."));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if (args.length == 2 && "edit".equalsIgnoreCase(args[0])) {
            return teamService.listSorted().stream().map(Team::getKey)
                    .filter(k -> k.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && "set".equalsIgnoreCase(args[0])) {
            return teamService.listSorted().stream().map(Team::getKey)
                    .filter(k -> k.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && "clear".equalsIgnoreCase(args[0])) {
            List<String> names = new ArrayList<>();
            plugin.getServer().getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return names.stream()
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
