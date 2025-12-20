package com.zbennoz.zbencore.commands;

import com.zbennoz.zbencore.ranks.Rank;
import com.zbennoz.zbencore.ranks.RankConversationManager;
import com.zbennoz.zbencore.ranks.RankService;
import com.zbennoz.zbencore.util.Msg;
import com.zbennoz.zbencore.util.Perm;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class RankCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = List.of("create", "edit", "list", "reload");

    private final JavaPlugin plugin;
    private final RankService rankService;
    private final RankConversationManager conversations;

    public RankCommand(JavaPlugin plugin, RankService rankService, RankConversationManager conversations) {
        this.plugin = plugin;
        this.rankService = rankService;
        this.conversations = conversations;
    }

    private boolean canManage(CommandSender sender) {
        return sender.isOp() || Perm.has(sender, "zben.ranks.manage");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Msg.pref(plugin, "&7Nutzung: /rank <create|edit|list|reload>"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> handleCreate(sender);
            case "edit" -> handleEdit(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
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
        List<Rank> ranks = rankService.listSorted();
        if (ranks.isEmpty()) {
            sender.sendMessage(Msg.pref(plugin, "&7Keine Ränge vorhanden."));
            return;
        }
        sender.sendMessage(Msg.pref(plugin, "&aRänge (&e" + ranks.size() + "&a):"));
        for (Rank rank : ranks) {
            sender.sendMessage(Msg.pref(plugin, "&e" + rank.getKey() + " &7| Name: &f" + rank.getDisplayName()
                    + " &7| Prefix: &f" + rank.getPrefix() + " &7| Weight: &f" + rank.getWeight()));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!canManage(sender)) {
            sender.sendMessage(Msg.pref(plugin, plugin.getConfig().getString("messages.noPermission")));
            return;
        }
        rankService.reload();
        sender.sendMessage(Msg.pref(plugin, "&aRanks wurden neu geladen."));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if (args.length == 2 && "edit".equalsIgnoreCase(args[0])) {
            return rankService.listSorted().stream().map(Rank::getKey)
                    .filter(k -> k.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
