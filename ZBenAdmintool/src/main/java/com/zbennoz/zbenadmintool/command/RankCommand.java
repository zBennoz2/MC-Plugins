package com.zbennoz.zbenadmintool.command;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import com.zbennoz.zbenadmintool.rank.Rank;
import com.zbennoz.zbenadmintool.rank.RankPermission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class RankCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("help", "list", "info", "create", "delete", "set", "remove", "perm", "backpack");
    private final ZBenAdmintool plugin;

    public RankCommand(ZBenAdmintool plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasManagePermission(sender)) {
            sender.sendMessage(plugin.getMessages().raw("no_permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> sendHelp(sender);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "set" -> handleSet(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "perm" -> handlePerm(sender, args);
            case "backpack" -> handleBackpack(sender, args);
            default -> sender.sendMessage(plugin.getMessages().raw("rank.unknown_command"));
        }
        return true;
    }

    private boolean hasManagePermission(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        return plugin.getPermissionResolver().has(player, RankPermission.RANK_MANAGE);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getMessages().raw("rank.help"));
    }

    private void handleList(CommandSender sender) {
        String ranks = plugin.getRankManager().getRanks().stream()
                .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority()))
                .map(Rank::getName)
                .collect(Collectors.joining(", "));
        sender.sendMessage(plugin.getMessages().raw("rank.list").replace("%ranks%", ranks));
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessages().raw("rank.info_usage"));
            return;
        }
        Rank rank = plugin.getRankManager().getRank(args[1]);
        if (rank == null) {
            sender.sendMessage(plugin.getMessages().raw("rank.not_found"));
            return;
        }
        sender.sendMessage(plugin.getMessages().raw("rank.info_line")
                .replace("%name%", rank.getName())
                .replace("%color%", rank.getColorText())
                .replace("%priority%", String.valueOf(rank.getPriority())));
        sender.sendMessage(plugin.getMessages().raw("rank.info_backpack")
                .replace("%slots%", String.valueOf(rank.getBackpackSlots())));
        Set<String> permissions = collectPermissions(rank);
        if (permissions.isEmpty()) {
            sender.sendMessage(plugin.getMessages().raw("rank.permissions_empty"));
        } else {
            sender.sendMessage(plugin.getMessages().raw("rank.perm_list_header").replace("%rank%", rank.getName()));
            permissions.stream().sorted().forEach(perm ->
                    sender.sendMessage(plugin.getMessages().raw("rank.perm_list_entry").replace("%perm%", perm)));
        }
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(plugin.getMessages().raw("rank.create_usage"));
            return;
        }
        String name = args[1];
        if (!plugin.getRankManager().isValidRankName(name)) {
            sender.sendMessage(plugin.getMessages().raw("rank.invalid_name"));
            return;
        }
        if (plugin.getRankManager().getRank(name) != null) {
            sender.sendMessage(plugin.getMessages().raw("rank.exists"));
            return;
        }
        String colorInput = args[2];
        if (!plugin.getRankManager().isValidColor(colorInput)) {
            sender.sendMessage(plugin.getMessages().raw("rank.invalid_color"));
            return;
        }
        int priority;
        try {
            priority = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessages().raw("rank.invalid_priority"));
            return;
        }
        int backpackSlots = plugin.getRankManager().defaultBackpackSlotsFor(name);
        if (args.length >= 5) {
            try {
                backpackSlots = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getMessages().raw("rank.invalid_backpack"));
                return;
            }
            if (!plugin.getRankManager().isValidBackpackSize(backpackSlots)) {
                sender.sendMessage(plugin.getMessages().raw("rank.invalid_backpack"));
                return;
            }
        }
        String legacyColor = plugin.getRankManager().parseLegacyColor(colorInput);
        boolean success = plugin.getRankManager().createRank(name, colorInput, legacyColor, priority, "", "", backpackSlots);
        sender.sendMessage(plugin.getMessages().raw(success ? "rank.created" : "rank.create_failed").replace("%name%", name));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessages().raw("rank.delete_usage"));
            return;
        }
        String name = args[1];
        Rank rank = plugin.getRankManager().getRank(name);
        if (rank == null) {
            sender.sendMessage(plugin.getMessages().raw("rank.not_found"));
            return;
        }
        boolean success = plugin.getRankManager().deleteRank(name);
        sender.sendMessage(plugin.getMessages().raw(success ? "rank.deleted" : "rank.delete_failed").replace("%name%", name));
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getMessages().raw("rank.set_usage"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        Rank rank = plugin.getRankManager().getRank(args[2]);
        if (rank == null) {
            sender.sendMessage(plugin.getMessages().raw("rank.not_found"));
            return;
        }
        plugin.getRankManager().setPlayerRank(target.getUniqueId(), rank.getName());
        sender.sendMessage(plugin.getMessages().raw("rank.assigned")
                .replace("%player%", target.getName() == null ? args[1] : target.getName())
                .replace("%rank%", rank.getName()));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessages().raw("rank.remove_usage"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        plugin.getRankManager().removePlayerRank(target.getUniqueId());
        sender.sendMessage(plugin.getMessages().raw("rank.removed").replace("%player%", target.getName() == null ? args[1] : target.getName()));
    }

    private void handlePerm(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getMessages().raw("rank.perm_usage"));
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        String rankName = args[2];
        Rank rank = plugin.getRankManager().getRank(rankName);
        if (rank == null) {
            sender.sendMessage(plugin.getMessages().raw("rank.not_found"));
            return;
        }
        switch (action) {
            case "add" -> {
                if (args.length < 4) {
                    sender.sendMessage(plugin.getMessages().raw("rank.perm_add_usage"));
                    return;
                }
                String perm = args[3];
                boolean success = plugin.getRankManager().addPermission(rankName, perm);
                sender.sendMessage(plugin.getMessages().raw(success ? "rank.permission_added" : "rank.permission_failed")
                        .replace("%perm%", perm)
                        .replace("%rank%", rankName));
            }
            case "remove" -> {
                if (args.length < 4) {
                    sender.sendMessage(plugin.getMessages().raw("rank.perm_remove_usage"));
                    return;
                }
                String perm = args[3];
                boolean success = plugin.getRankManager().removePermission(rankName, perm);
                sender.sendMessage(plugin.getMessages().raw(success ? "rank.permission_removed" : "rank.permission_failed")
                        .replace("%perm%", perm)
                        .replace("%rank%", rankName));
            }
            case "list" -> {
                Set<String> permissions = collectPermissions(rank);
                if (permissions.isEmpty()) {
                    sender.sendMessage(plugin.getMessages().raw("rank.permissions_empty"));
                } else {
                    sender.sendMessage(plugin.getMessages().raw("rank.perm_list_header").replace("%rank%", rank.getName()));
                    permissions.stream().sorted().forEach(perm ->
                            sender.sendMessage(plugin.getMessages().raw("rank.perm_list_entry").replace("%perm%", perm)));
                }
            }
            default -> sender.sendMessage(plugin.getMessages().raw("rank.unknown_command"));
        }
    }

    private void handleBackpack(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getMessages().raw("rank.backpack_usage"));
            return;
        }
        String rankName = args[1];
        int slots;
        try {
            slots = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessages().raw("rank.invalid_backpack"));
            return;
        }
        if (!plugin.getRankManager().isValidBackpackSize(slots)) {
            sender.sendMessage(plugin.getMessages().raw("rank.invalid_backpack"));
            return;
        }
        boolean success = plugin.getRankManager().updateBackpackSlots(rankName, slots);
        sender.sendMessage(plugin.getMessages().raw(success ? "rank.backpack_updated" : "rank.backpack_failed")
                .replace("%rank%", rankName)
                .replace("%slots%", String.valueOf(slots)));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(sub -> sub.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "set" -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                case "delete", "info", "backpack" -> plugin.getRankManager().getRankNames();
                case "perm" -> List.of("add", "remove", "list");
                case "create" -> Collections.emptyList();
                case "remove" -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                default -> Collections.emptyList();
            };
        }
        if (args.length == 3) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "set" -> plugin.getRankManager().getRankNames();
                case "perm" -> plugin.getRankManager().getRankNames();
                case "create" -> colorSuggestions(args[2]);
                case "backpack" -> List.of("9", "18", "27", "36", "45", "54");
                default -> Collections.emptyList();
            };
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("perm")) {
            return Collections.singletonList("example.permission");
        }
        return Collections.emptyList();
    }

    private List<String> colorSuggestions(String current) {
        List<String> suggestions = new ArrayList<>(Arrays.asList("red", "green", "blue", "yellow", "gold", "white", "#ff0000", "#00ff00", "#0000ff"));
        if (current == null || current.isEmpty()) {
            return suggestions;
        }
        return suggestions.stream().filter(c -> c.toLowerCase(Locale.ROOT).startsWith(current.toLowerCase(Locale.ROOT))).toList();
    }

    private Set<String> collectPermissions(Rank rank) {
        Set<String> permissions = new HashSet<>(rank.getBukkitPermissions());
        rank.getRolePermissions().forEach(perm -> permissions.add(perm.name()));
        return permissions;
    }
}
