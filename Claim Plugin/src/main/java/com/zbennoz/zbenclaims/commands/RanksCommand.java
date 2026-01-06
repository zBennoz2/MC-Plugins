package com.zbennoz.zbenclaims.commands;

import com.zbennoz.zbenclaims.ZBenClaimsPlugin;
import com.zbennoz.zbenclaims.ranks.Rank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;
import java.util.stream.Collectors;

public class RanksCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    public RanksCommand(ZBenClaimsPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (plugin.getRankManager() == null) {
            plugin.getMessages().sendRaw(sender, "&cRank-System ist deaktiviert.");
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> handleList(sender);
            case "info" -> {
                if (args.length < 2) return false;
                handleInfo(sender, args[1]);
            }
            case "create" -> {
                if (args.length < 2) return false;
                if (!ensureAdmin(sender)) return true;
                handleCreate(sender, args[1]);
            }
            case "delete" -> {
                if (args.length < 2) return false;
                if (!ensureAdmin(sender)) return true;
                String confirm = args.length >= 3 ? args[2] : "";
                handleDelete(sender, args[1], confirm.equalsIgnoreCase("confirm"));
            }
            case "rename" -> {
                if (args.length < 3) return false;
                if (!ensureAdmin(sender)) return true;
                handleRename(sender, args[1], args[2]);
            }
            case "set" -> {
                if (args.length < 4) return false;
                if (!ensureAdmin(sender)) return true;
                handleSet(sender, args[1], args[2], args[3]);
            }
            case "addflag" -> {
                if (args.length < 3) return false;
                if (!ensureAdmin(sender)) return true;
                handleFlag(sender, args[1], args[2], true);
            }
            case "removeflag" -> {
                if (args.length < 3) return false;
                if (!ensureAdmin(sender)) return true;
                handleFlag(sender, args[1], args[2], false);
            }
            case "reload" -> {
                if (!ensureAdmin(sender)) return true;
                plugin.reloadAll();
                plugin.getMessages().send(sender, "reload");
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleList(CommandSender sender) {
        List<String> names = plugin.getRankManager().getRanks().stream()
                .map(Rank::name)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        plugin.getMessages().send(sender, "ranks.list", Map.of("ranks", String.join("&7, &f", names)));
    }

    private void handleInfo(CommandSender sender, String name) {
        Optional<Rank> rankOpt = plugin.getRankManager().getRank(name);
        if (rankOpt.isEmpty()) {
            plugin.getMessages().send(sender, "ranks.notFound", Map.of("rank", name));
            return;
        }
        Rank rank = rankOpt.get();
        String flags = rank.flags().isEmpty() ? "-" : String.join(", ", new TreeSet<>(rank.flags()));
        String perms = rank.permissions().isEmpty() ? "-" : String.join(", ", new TreeSet<>(rank.permissions()));

        plugin.getMessages().sendRaw(sender, "&7Rank &f" + rank.name() + "&7:" +
                "\n &8• &7Priorität: &f" + rank.priority() +
                "\n &8• &7Claim-Limit: &f" + rank.limit() +
                "\n &8• &7Kosten: &f" + rank.cost() +
                "\n &8• &7Tab-Prefix: &f" + rank.tabPrefix() +
                "\n &8• &7Chat-Prefix: &f" + rank.chatPrefix() +
                "\n &8• &7Nametag-Prefix: &f" + rank.nametagPrefix() +
                "\n &8• &7Flags: &f" + flags +
                "\n &8• &7Permissions: &f" + perms);
    }

    private void handleCreate(CommandSender sender, String name) {
        if (plugin.getRankManager().getRank(name).isPresent()) {
            plugin.getMessages().send(sender, "ranks.exists", Map.of("rank", name));
            return;
        }

        String path = "ranks.list." + name + ".";
        plugin.getConfig().set(path + "priority", 0);
        plugin.getConfig().set(path + "limit", 0);
        plugin.getConfig().set(path + "tabPrefix", "");
        plugin.getConfig().set(path + "chatPrefix", "");
        plugin.getConfig().set(path + "nametagPrefix", "");
        plugin.getConfig().set(path + "cost", 0.0D);
        plugin.saveConfig();
        plugin.getRankManager().reload();
        plugin.getMessages().send(sender, "ranks.created", Map.of("rank", name));
    }

    private void handleDelete(CommandSender sender, String name, boolean confirmed) {
        if (plugin.getRankManager().getRank(name).isEmpty()) {
            plugin.getMessages().send(sender, "ranks.notFound", Map.of("rank", name));
            return;
        }

        String path = "ranks.list." + name;
        if (!confirmed) {
            plugin.getMessages().send(sender, "ranks.deleteConfirm", Map.of("rank", name));
            return;
        }
        plugin.getConfig().set(path, null);
        plugin.saveConfig();
        plugin.getRankManager().reload();
        plugin.getMessages().send(sender, "ranks.deleted", Map.of("rank", name));
    }

    private void handleRename(CommandSender sender, String oldName, String newName) {
        if (plugin.getRankManager().getRank(oldName).isEmpty()) {
            plugin.getMessages().send(sender, "ranks.notFound", Map.of("rank", oldName));
            return;
        }
        if (plugin.getRankManager().getRank(newName).isPresent()) {
            plugin.getMessages().send(sender, "ranks.exists", Map.of("rank", newName));
            return;
        }

        String oldPath = "ranks.list." + oldName;
        String newPath = "ranks.list." + newName;
        var section = plugin.getConfig().getConfigurationSection(oldPath);
        if (section != null) {
            plugin.getConfig().set(newPath, section.getValues(true));
        }
        plugin.getConfig().set(oldPath, null);
        plugin.saveConfig();
        plugin.getRankManager().reload();
        plugin.getMessages().send(sender, "ranks.renamed", Map.of("old", oldName, "new", newName));
    }

    private void handleSet(CommandSender sender, String rankName, String property, String value) {
        Optional<Rank> rankOpt = plugin.getRankManager().getRank(rankName);
        if (rankOpt.isEmpty()) {
            plugin.getMessages().send(sender, "ranks.notFound", Map.of("rank", rankName));
            return;
        }

        String base = "ranks.list." + rankOpt.get().name() + ".";
        switch (property.toLowerCase(Locale.ROOT)) {
            case "priority" -> plugin.getConfig().set(base + "priority", parseInt(value));
            case "limit", "claimlimit" -> plugin.getConfig().set(base + "limit", parseInt(value));
            case "cost" -> plugin.getConfig().set(base + "cost", parseDouble(value));
            case "tabprefix" -> plugin.getConfig().set(base + "tabPrefix", value);
            case "chatprefix" -> plugin.getConfig().set(base + "chatPrefix", value);
            case "nametagprefix" -> plugin.getConfig().set(base + "nametagPrefix", value);
            default -> {
                if (property.toLowerCase(Locale.ROOT).startsWith("flags.")) {
                    String flag = property.substring("flags.".length());
                    plugin.getConfig().set(base + "flags." + flag, parseBoolean(value));
                } else if (property.toLowerCase(Locale.ROOT).startsWith("permissions.")) {
                    String perm = property.substring("permissions.".length());
                    plugin.getConfig().set(base + "permissions." + perm, parseBoolean(value));
                } else {
                    plugin.getMessages().send(sender, "ranks.invalidProperty", Map.of("property", property));
                    return;
                }
            }
        }
        plugin.saveConfig();
        plugin.getRankManager().reload();
        plugin.getMessages().send(sender, "ranks.updated", Map.of("rank", rankOpt.get().name(), "property", property));
    }

    private void handleFlag(CommandSender sender, String rankName, String flag, boolean add) {
        Optional<Rank> rankOpt = plugin.getRankManager().getRank(rankName);
        if (rankOpt.isEmpty()) {
            plugin.getMessages().send(sender, "ranks.notFound", Map.of("rank", rankName));
            return;
        }
        String path = "ranks.list." + rankOpt.get().name() + ".flags." + flag;
        if (add) {
            plugin.getConfig().set(path, true);
            plugin.getMessages().send(sender, "ranks.flagAdded", Map.of("rank", rankOpt.get().name(), "flag", flag));
        } else {
            plugin.getConfig().set(path, null);
            plugin.getMessages().send(sender, "ranks.flagRemoved", Map.of("rank", rankOpt.get().name(), "flag", flag));
        }
        plugin.saveConfig();
        plugin.getRankManager().reload();
    }

    private int parseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private double parseDouble(String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            return 0.0D;
        }
    }

    private boolean parseBoolean(String raw) {
        return raw.equalsIgnoreCase("true") || raw.equalsIgnoreCase("yes") || raw.equalsIgnoreCase("on") || raw.equalsIgnoreCase("1");
    }

    private void sendHelp(CommandSender sender) {
        boolean admin = sender.isOp() || sender.hasPermission("claim.admin") || sender.hasPermission("zbenclaims.admin") || sender.hasPermission("zbenclaims.admin.rank");
        sender.sendMessage(plugin.getMessages().comp("&7/ranks list &8- &7Alle Ränge anzeigen"));
        sender.sendMessage(plugin.getMessages().comp("&7/ranks info <rang> &8- &7Details zu einem Rang"));
        if (admin) {
            sender.sendMessage(plugin.getMessages().comp("&7/ranks create <rang> &8- &7Neuen Rang anlegen"));
            sender.sendMessage(plugin.getMessages().comp("&7/ranks delete <rang> [confirm] &8- &7Rang löschen"));
            sender.sendMessage(plugin.getMessages().comp("&7/ranks rename <alt> <neu> &8- &7Rang umbenennen"));
            sender.sendMessage(plugin.getMessages().comp("&7/ranks set <rang> <eigenschaft> <wert> &8- &7Eigenschaften setzen"));
            sender.sendMessage(plugin.getMessages().comp("&7/ranks addflag <rang> <flag> &8- &7Flag hinzufügen"));
            sender.sendMessage(plugin.getMessages().comp("&7/ranks removeflag <rang> <flag> &8- &7Flag entfernen"));
            sender.sendMessage(plugin.getMessages().comp("&7/ranks reload &8- &7Config neu laden"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean admin = sender.isOp() || sender.hasPermission("claim.admin") || sender.hasPermission("zbenclaims.admin") || sender.hasPermission("zbenclaims.admin.rank");
        if (args.length == 1) {
            List<String> base = new ArrayList<>(List.of("list", "info"));
            if (admin) base.addAll(List.of("create", "delete", "rename", "set", "addflag", "removeflag", "reload"));
            return base.stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).sorted().toList();
        }
        if (args.length == 2) {
            List<String> names = plugin.getRankManager().getRanks().stream().map(Rank::name).toList();
            if (args[0].equalsIgnoreCase("delete")) {
                names = new ArrayList<>(names);
            }
            return names.stream().filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).sorted().collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("rename")) {
            return List.of();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            List<String> props = List.of("priority", "limit", "claimlimit", "cost", "tabPrefix", "chatPrefix", "nametagPrefix", "flags.", "permissions.");
            return props.stream().filter(p -> p.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("addflag") || args[0].equalsIgnoreCase("removeflag"))) {
            return List.of();
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("set")) {
            return List.of();
        }
        return List.of();
    }
}
