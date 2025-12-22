package com.zbennoz.zbenadmintool.command;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import com.zbennoz.zbenadmintool.rank.Rank;
import com.zbennoz.zbenadmintool.rank.RankManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class RankCommand implements CommandExecutor, TabCompleter {

    private final ZBenAdmintool plugin;

    public RankCommand(ZBenAdmintool plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessages().raw("rank.player_only"));
            return true;
        }
        if (!plugin.getPermissionResolver().has(player, "zbenadmintool.rank")) {
            player.sendMessage(plugin.getMessages().raw("no_permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> sendHelp(player);
            case "list" -> handleList(player);
            case "info" -> handleInfo(player, args);
            case "create" -> handleCreate(player, args);
            case "delete" -> handleDelete(player, args);
            case "set" -> handleSet(player, args);
            case "remove" -> handleRemove(player, args);
            case "perm" -> handlePerm(player, args);
            default -> player.sendMessage(plugin.getMessages().raw("rank.unknown_command"));
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(plugin.getMessages().raw("rank.help"));
    }

    private void handleList(Player player) {
        String ranks = plugin.getRankManager().getRanks().stream()
                .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority()))
                .map(Rank::getName)
                .collect(Collectors.joining(", "));
        player.sendMessage(plugin.getMessages().raw("rank.list").replace("%ranks%", ranks));
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getMessages().raw("rank.info_usage"));
            return;
        }
        Rank rank = plugin.getRankManager().getRank(args[1]);
        if (rank == null) {
            player.sendMessage(plugin.getMessages().raw("rank.not_found"));
            return;
        }
        player.sendMessage(plugin.getMessages().raw("rank.info_line")
                .replace("%name%", rank.getName())
                .replace("%color%", rank.getColorText())
                .replace("%priority%", String.valueOf(rank.getPriority())));
        if (rank.getPermissions().isEmpty()) {
            player.sendMessage(plugin.getMessages().raw("rank.permissions_empty"));
        } else {
            player.sendMessage(plugin.getMessages().raw("rank.perm_list_header").replace("%rank%", rank.getName()));
            rank.getPermissions().stream().sorted().forEach(perm ->
                    player.sendMessage(plugin.getMessages().raw("rank.perm_list_entry").replace("%perm%", perm)));
        }
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(plugin.getMessages().raw("rank.create_usage"));
            return;
        }
        String name = args[1];
        String colorInput = args[2];
        if (!plugin.getRankManager().isValidColor(colorInput)) {
            player.sendMessage(plugin.getMessages().raw("rank.invalid_color"));
            return;
        }
        int priority;
        try {
            priority = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessages().raw("rank.invalid_priority"));
            return;
        }
        String legacyColor = plugin.getRankManager().parseLegacyColor(colorInput);
        String prefix = legacyColor + "[" + name + "] ";
        boolean success = plugin.getRankManager().createRank(name, colorInput, legacyColor, priority, prefix, "");
        player.sendMessage(plugin.getMessages().raw(success ? "rank.created" : "rank.create_failed").replace("%name%", name));
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getMessages().raw("rank.delete_usage"));
            return;
        }
        String name = args[1];
        Rank rank = plugin.getRankManager().getRank(name);
        if (rank == null) {
            player.sendMessage(plugin.getMessages().raw("rank.not_found"));
            return;
        }
        boolean success = plugin.getRankManager().deleteRank(name);
        player.sendMessage(plugin.getMessages().raw(success ? "rank.deleted" : "rank.delete_failed").replace("%name%", name));
    }

    private void handleSet(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getMessages().raw("rank.set_usage"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        Rank rank = plugin.getRankManager().getRank(args[2]);
        if (rank == null) {
            player.sendMessage(plugin.getMessages().raw("rank.not_found"));
            return;
        }
        plugin.getRankManager().setPlayerRank(target.getUniqueId(), rank.getName());
        player.sendMessage(plugin.getMessages().raw("rank.assigned")
                .replace("%player%", target.getName() == null ? args[1] : target.getName())
                .replace("%rank%", rank.getName()));
    }

    private void handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getMessages().raw("rank.remove_usage"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        plugin.getRankManager().removePlayerRank(target.getUniqueId());
        player.sendMessage(plugin.getMessages().raw("rank.removed").replace("%player%", target.getName() == null ? args[1] : target.getName()));
    }

    private void handlePerm(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getMessages().raw("rank.perm_usage"));
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        String rankName = args[2];
        Rank rank = plugin.getRankManager().getRank(rankName);
        if (rank == null) {
            player.sendMessage(plugin.getMessages().raw("rank.not_found"));
            return;
        }
        switch (action) {
            case "add" -> {
                if (args.length < 4) {
                    player.sendMessage(plugin.getMessages().raw("rank.perm_add_usage"));
                    return;
                }
                String perm = args[3];
                boolean success = plugin.getRankManager().addPermission(rankName, perm);
                player.sendMessage(plugin.getMessages().raw(success ? "rank.permission_added" : "rank.permission_failed")
                        .replace("%perm%", perm)
                        .replace("%rank%", rankName));
            }
            case "remove" -> {
                if (args.length < 4) {
                    player.sendMessage(plugin.getMessages().raw("rank.perm_remove_usage"));
                    return;
                }
                String perm = args[3];
                boolean success = plugin.getRankManager().removePermission(rankName, perm);
                player.sendMessage(plugin.getMessages().raw(success ? "rank.permission_removed" : "rank.permission_failed")
                        .replace("%perm%", perm)
                        .replace("%rank%", rankName));
            }
            case "list" -> {
                if (rank.getPermissions().isEmpty()) {
                    player.sendMessage(plugin.getMessages().raw("rank.permissions_empty"));
                } else {
                    player.sendMessage(plugin.getMessages().raw("rank.perm_list_header").replace("%rank%", rank.getName()));
                    rank.getPermissions().stream().sorted().forEach(perm ->
                            player.sendMessage(plugin.getMessages().raw("rank.perm_list_entry").replace("%perm%", perm)));
                }
            }
            default -> player.sendMessage(plugin.getMessages().raw("rank.unknown_command"));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("help", "list", "info", "create", "delete", "set", "remove", "perm");
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "set" -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                case "delete", "info" -> rankNames();
                case "perm" -> List.of("add", "remove", "list");
                default -> Collections.emptyList();
            };
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return rankNames();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("perm")) {
            return rankNames();
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("perm")) {
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    private List<String> rankNames() {
        return plugin.getRankManager().getRanks().stream().map(Rank::getName).toList();
    }
}
