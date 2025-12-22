package com.zbennoz.zbencityjobs.commands;

import com.zbennoz.zbencityjobs.service.CoinService;
import com.zbennoz.zbencityjobs.util.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CoinsCommand implements CommandExecutor, TabCompleter {
    private static final String USE_PERMISSION = "zbenjobs.coins.use";
    private static final String ADMIN_PERMISSION = "zbenjobs.coins.admin";

    private final CoinService coinService;
    private final MessageService messages;

    public CoinsCommand(CoinService coinService, MessageService messages) {
        this.coinService = coinService;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(USE_PERMISSION)) {
            sender.sendMessage(messages.get("errors.no-permission"));
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player player) {
                sendBalance(sender, player.getUniqueId(), player.getName(), false);
                return true;
            }
            sender.sendMessage("Usage: /coins balance <player>");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "balance" -> handleBalance(sender, args);
            case "pay" -> handlePay(sender, args);
            case "add" -> handleAdminChange(sender, args, Operation.ADD);
            case "remove" -> handleAdminChange(sender, args, Operation.REMOVE);
            case "set" -> handleAdminChange(sender, args, Operation.SET);
            default -> sender.sendMessage("Unknown subcommand.");
        }
        return true;
    }

    private void handleBalance(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (sender instanceof Player player) {
                sendBalance(sender, player.getUniqueId(), player.getName(), false);
            } else {
                sender.sendMessage("Usage: /coins balance <player>");
            }
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            sender.sendMessage(messages.get("errors.player-not-found"));
            return;
        }
        if (!sender.getName().equalsIgnoreCase(target.getName()) && !sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(messages.get("errors.no-permission"));
            return;
        }
        sendBalance(sender, target.getUniqueId(), target.getName(), true);
    }

    private void sendBalance(CommandSender sender, UUID uuid, String name, boolean other) {
        long balance = coinService.getBalance(uuid);
        Map<String, String> placeholders = Map.of(
                "amount", coinService.formatAmount(balance),
                "currency", coinService.getCurrencyName(),
                "player", name
        );
        if (other) {
            sender.sendMessage(messages.get("info.coins-balance-other", placeholders));
        } else {
            sender.sendMessage(messages.get("info.coins-balance-self", placeholders));
        }
    }

    private void handlePay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("Usage: /coins pay <player> <amount>");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            sender.sendMessage(messages.get("errors.player-not-found"));
            return;
        }
        long amount = parseAmount(args[2]);
        if (amount <= 0) {
            sender.sendMessage(messages.get("errors.invalid-amount", Map.of("max", String.valueOf(coinService.getMaxAmount()))));
            return;
        }
        if (!coinService.transfer(player.getUniqueId(), target.getUniqueId(), amount, "pay")) {
            sender.sendMessage(messages.get("errors.not-enough-coins", Map.of(
                    "amount", coinService.formatAmount(coinService.getBalance(player.getUniqueId())),
                    "currency", coinService.getCurrencyName())));
            return;
        }
        sender.sendMessage(messages.get("info.coins-paid", Map.of(
                "amount", coinService.formatAmount(amount),
                "currency", coinService.getCurrencyName(),
                "player", target.getName()
        )));
        if (target.isOnline()) {
            target.getPlayer().sendMessage(messages.get("info.coins-received", Map.of(
                    "amount", coinService.formatAmount(amount),
                    "currency", coinService.getCurrencyName(),
                    "player", sender.getName()
            )));
        }
    }

    private void handleAdminChange(CommandSender sender, String[] args, Operation op) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(messages.get("errors.no-permission"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("Usage: /coins " + op.getLabel() + " <player> <amount> [reason]");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            sender.sendMessage(messages.get("errors.player-not-found"));
            return;
        }
        long amount = parseAmount(args[2]);
        if (amount < 0 || (op != Operation.SET && amount == 0)) {
            sender.sendMessage(messages.get("errors.invalid-amount", Map.of("max", String.valueOf(coinService.getMaxAmount()))));
            return;
        }
        String reason = args.length >= 4 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : op.getLabel();
        boolean success;
        switch (op) {
            case ADD -> success = coinService.add(target.getUniqueId(), amount, reason);
            case REMOVE -> success = coinService.remove(target.getUniqueId(), amount, reason);
            case SET -> {
                coinService.setBalance(target.getUniqueId(), amount, reason);
                success = true;
            }
            default -> success = false;
        }
        if (!success) {
            sender.sendMessage(messages.get("errors.transaction-failed"));
            return;
        }
        Map<String, String> placeholders = Map.of(
                "amount", coinService.formatAmount(amount),
                "currency", coinService.getCurrencyName(),
                "player", target.getName()
        );
        switch (op) {
            case ADD -> sender.sendMessage(messages.get("info.coins-added", placeholders));
            case REMOVE -> sender.sendMessage(messages.get("info.coins-removed", placeholders));
            case SET -> sender.sendMessage(messages.get("info.coins-set", placeholders));
        }
    }

    private long parseAmount(String input) {
        try {
            long value = Long.parseLong(input);
            if (value <= 0) return -1;
            return Math.min(value, coinService.getMaxAmount());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(USE_PERMISSION)) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return filter(Arrays.asList("balance", "pay", "add", "remove", "set"), args[0]);
        }
        if (args.length == 2) {
            return filter(onlineNames(), args[1]);
        }
        if (args.length == 3 && !args[0].equalsIgnoreCase("balance")) {
            return Collections.singletonList("100");
        }
        return Collections.emptyList();
    }

    private List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    private List<String> filter(List<String> values, String prefix) {
        if (prefix == null || prefix.isEmpty()) return values;
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(value);
            }
        }
        return result;
    }

    private enum Operation {
        ADD("add"),
        REMOVE("remove"),
        SET("set");

        private final String label;

        Operation(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
