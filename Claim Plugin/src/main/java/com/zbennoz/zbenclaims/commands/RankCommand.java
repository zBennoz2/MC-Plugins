package com.zbennoz.zbenclaims.commands;

import com.zbennoz.zbenclaims.ZBenClaimsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class RankCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    public RankCommand(ZBenClaimsPlugin plugin) { super(plugin); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!ensurePerm(sender, "zbenclaims.admin.rank")) return true;
        if (args.length < 1) return false;

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("list")) {
            var sec = plugin.getConfig().getConfigurationSection("ranks.list");
            sender.sendMessage(plugin.getMessages().comp("&7Ranks: &f" + String.join("&7, &f", sec.getKeys(false))));
            return true;
        }

        if (args.length < 2) return false;

        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[1]);
        if (target == null || target.getUniqueId() == null) {
            plugin.getMessages().send(sender, "unknownPlayer");
            return true;
        }

        if (sub.equals("get")) {
            String r = plugin.getDatabase().getPlayerRank(target.getUniqueId());
            if (r == null) r = "(auto)";
            plugin.getMessages().send(sender, "rankGet", Map.of("player", target.getName() != null ? target.getName() : args[1], "rank", r));
            return true;
        }

        if (sub.equals("set")) {
            if (args.length < 3) return false;
            String rank = args[2];

            if (rank.equalsIgnoreCase("none") || rank.equalsIgnoreCase("clear")) {
                plugin.getDatabase().setPlayerRank(target.getUniqueId(), null);
                plugin.getMessages().send(sender, "rankCleared", Map.of("player", target.getName() != null ? target.getName() : args[1]));
                if (target.isOnline() && target.getPlayer() != null) plugin.getRankManager().applyVisuals(target.getPlayer());
                return true;
            }

            var sec = plugin.getConfig().getConfigurationSection("ranks.list");
            if (sec == null || !sec.getKeys(false).contains(rank)) {
                sender.sendMessage(plugin.getMessages().comp("&cUnbekannter Rank. Nutze: /rank list"));
                return true;
            }

            plugin.getDatabase().setPlayerRank(target.getUniqueId(), rank);
            plugin.getMessages().send(sender, "rankSet", Map.of("player", target.getName() != null ? target.getName() : args[1], "rank", rank));
            if (target.isOnline() && target.getPlayer() != null) plugin.getRankManager().applyVisuals(target.getPlayer());
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("set", "get", "list").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(prefix)).sorted().collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            String prefix = args[2].toLowerCase();
            List<String> ranks = new ArrayList<>(plugin.getConfig().getConfigurationSection("ranks.list").getKeys(false));
            ranks.add("none");
            return ranks.stream().filter(r -> r.toLowerCase().startsWith(prefix)).sorted().collect(Collectors.toList());
        }
        return List.of();
    }
}
