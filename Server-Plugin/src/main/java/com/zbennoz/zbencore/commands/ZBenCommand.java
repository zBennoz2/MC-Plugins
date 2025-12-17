package com.zbennoz.zbencore.commands;

import com.zbennoz.zbencore.util.Msg;
import com.zbennoz.zbencore.util.Perm;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.*;
import java.util.stream.Collectors;

public final class ZBenCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;

    private static final List<String> SUBS = List.of("kick", "gamerule", "time", "weather", "whitelist");
    private static final List<String> WEATHER = List.of("sun", "rain", "thunder");
    private static final List<String> TIME = List.of("day", "night", "set");
    private static final List<String> WHITELIST = List.of("on", "off", "add", "remove", "list");

    public ZBenCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage(Msg.pref(plugin, plugin.getConfig().getString("messages.invalidUsage")
                    .replace("{usage}", "/zben <kick|gamerule|time|weather|whitelist>")));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "kick" -> handleKick(sender, args);
            case "gamerule" -> handleGamerule(sender, args);
            case "time" -> handleTime(sender, args);
            case "weather" -> handleWeather(sender, args);
            case "whitelist" -> handleWhitelist(sender, args);
            default -> sender.sendMessage(Msg.pref(plugin, plugin.getConfig().getString("messages.invalidUsage")
                    .replace("{usage}", "/zben <kick|gamerule|time|weather|whitelist>")));
        }

        return true;
    }

    private World resolveWorld(CommandSender sender) {
        if (sender instanceof Player p) return p.getWorld();
        var worlds = Bukkit.getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }

    private void noPerm(CommandSender sender) {
        sender.sendMessage(Msg.pref(plugin, plugin.getConfig().getString("messages.noPermission")));
    }

    private void usage(CommandSender sender, String pathInConfig) {
        String u = plugin.getConfig().getString("messages." + pathInConfig + ".usage");
        sender.sendMessage(Msg.pref(plugin, plugin.getConfig().getString("messages.invalidUsage")
                .replace("{usage}", u == null ? "" : u)));
    }

    private void handleKick(CommandSender sender, String[] args) {
        if (!Perm.has(sender, "zben.manage.kick")) { noPerm(sender); return; }
        if (args.length < 3) { usage(sender, "kick"); return; }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Msg.pref(plugin, plugin.getConfig().getString("messages.playerNotFound")));
            return;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        target.kick(LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&cDu wurdest gekickt: &7" + reason));

        sender.sendMessage(Msg.pref(plugin,
                plugin.getConfig().getString("messages.kick.done")
                        .replace("{player}", target.getName())
                        .replace("{reason}", reason)));
    }

    private void handleGamerule(CommandSender sender, String[] args) {
        if (!Perm.has(sender, "zben.manage.gamerule")) { noPerm(sender); return; }
        if (args.length < 3) { usage(sender, "gamerule"); return; }

        World world = resolveWorld(sender);
        if (world == null) {
            sender.sendMessage(Msg.pref(plugin, "&cKeine Welt gefunden."));
            return;
        }

        String ruleName = args[1];
        GameRule<?> rule = GameRule.getByName(ruleName);

        if (rule == null) {
            sender.sendMessage(Msg.pref(plugin,
                    plugin.getConfig().getString("messages.gamerule.invalidRule").replace("{rule}", ruleName)));
            return;
        }

        String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim();

        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            @SuppressWarnings("unchecked")
            GameRule<Boolean> br = (GameRule<Boolean>) rule;
            try {
                world.setGameRule(br, Boolean.parseBoolean(value));
            } catch (ClassCastException ex) {
                world.setGameRuleValue(rule.getName(), value);
            }
        } else {
            world.setGameRuleValue(rule.getName(), value);
        }

        sender.sendMessage(Msg.pref(plugin,
                plugin.getConfig().getString("messages.gamerule.done")
                        .replace("{rule}", rule.getName())
                        .replace("{value}", value)
                        .replace("{world}", world.getName())));
    }

    private void handleTime(CommandSender sender, String[] args) {
        if (!Perm.has(sender, "zben.manage.time")) { noPerm(sender); return; }
        if (args.length < 2) { usage(sender, "time"); return; }

        World world = resolveWorld(sender);
        if (world == null) {
            sender.sendMessage(Msg.pref(plugin, "&cKeine Welt gefunden."));
            return;
        }

        String mode = args[1].toLowerCase(Locale.ROOT);
        long t;

        if ("day".equals(mode)) t = 1000L;
        else if ("night".equals(mode)) t = 13000L;
        else if ("set".equals(mode)) {
            if (args.length < 3) { usage(sender, "time"); return; }
            try {
                t = Long.parseLong(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Msg.pref(plugin, "&cUngültiger Wert. Nutze eine Zahl."));
                return;
            }
        } else {
            usage(sender, "time"); return;
        }

        world.setTime(t);
        sender.sendMessage(Msg.pref(plugin,
                plugin.getConfig().getString("messages.time.done")
                        .replace("{world}", world.getName())
                        .replace("{value}", String.valueOf(t))));
    }

    private void handleWeather(CommandSender sender, String[] args) {
        if (!Perm.has(sender, "zben.manage.weather")) { noPerm(sender); return; }
        if (args.length < 2) { usage(sender, "weather"); return; }

        World world = resolveWorld(sender);
        if (world == null) {
            sender.sendMessage(Msg.pref(plugin, "&cKeine Welt gefunden."));
            return;
        }

        String mode = args[1].toLowerCase(Locale.ROOT);
        switch (mode) {
            case "sun" -> { world.setStorm(false); world.setThundering(false); }
            case "rain" -> { world.setStorm(true); world.setThundering(false); }
            case "thunder" -> { world.setStorm(true); world.setThundering(true); }
            default -> { usage(sender, "weather"); return; }
        }

        sender.sendMessage(Msg.pref(plugin,
                plugin.getConfig().getString("messages.weather.done")
                        .replace("{world}", world.getName())
                        .replace("{value}", mode)));
    }

    private void handleWhitelist(CommandSender sender, String[] args) {
        if (!Perm.has(sender, "zben.manage.whitelist")) { noPerm(sender); return; }
        if (args.length < 2) { usage(sender, "whitelist"); return; }

        String mode = args[1].toLowerCase(Locale.ROOT);

        switch (mode) {
            case "on" -> {
                Bukkit.setWhitelist(true);
                sender.sendMessage(Msg.pref(plugin, plugin.getConfig().getString("messages.whitelist.on")));
            }
            case "off" -> {
                Bukkit.setWhitelist(false);
                sender.sendMessage(Msg.pref(plugin, plugin.getConfig().getString("messages.whitelist.off")));
            }
            case "add" -> {
                if (args.length < 3) { usage(sender, "whitelist"); return; }
                OfflinePlayer p = Bukkit.getOfflinePlayer(args[2]);
                p.setWhitelisted(true);
                sender.sendMessage(Msg.pref(plugin, plugin.getConfig().getString("messages.whitelist.add")
                        .replace("{player}", p.getName() == null ? args[2] : p.getName())));
            }
            case "remove" -> {
                if (args.length < 3) { usage(sender, "whitelist"); return; }
                OfflinePlayer p = Bukkit.getOfflinePlayer(args[2]);
                p.setWhitelisted(false);
                sender.sendMessage(Msg.pref(plugin, plugin.getConfig().getString("messages.whitelist.remove")
                        .replace("{player}", p.getName() == null ? args[2] : p.getName())));
            }
            case "list" -> {
                var set = Bukkit.getWhitelistedPlayers();
                String names = set.stream()
                        .map(op -> op.getName() == null ? "unknown" : op.getName())
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .collect(Collectors.joining(", "));
                sender.sendMessage(Msg.pref(plugin, plugin.getConfig().getString("messages.whitelist.listHeader")
                        .replace("{count}", String.valueOf(set.size()))
                        .replace("{players}", names.isEmpty() ? "-" : names)));
            }
            default -> usage(sender, "whitelist");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return filterPrefix(SUBS, args[0]);
        }

        if (args.length >= 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);

            return switch (sub) {
                case "kick" -> (args.length == 2)
                        ? filterPrefix(onlineNames(), args[1])
                        : Collections.emptyList();

                case "gamerule" -> {
                    if (args.length == 2) yield filterPrefix(gameRuleNames(), args[1]);
                    if (args.length == 3) yield filterPrefix(List.of("true", "false", "0", "1", "10", "100"), args[2]);
                    yield Collections.emptyList();
                }

                case "time" -> {
                    if (args.length == 2) yield filterPrefix(TIME, args[1]);
                    if (args.length == 3 && "set".equalsIgnoreCase(args[1]))
                        yield filterPrefix(List.of("0", "1000", "6000", "12000", "13000", "18000"), args[2]);
                    yield Collections.emptyList();
                }

                case "weather" -> (args.length == 2)
                        ? filterPrefix(WEATHER, args[1])
                        : Collections.emptyList();

                case "whitelist" -> {
                    if (args.length == 2) yield filterPrefix(WHITELIST, args[1]);
                    if (args.length == 3 && ("add".equalsIgnoreCase(args[1]) || "remove".equalsIgnoreCase(args[1])))
                        yield filterPrefix(knownPlayerNames(), args[2]);
                    yield Collections.emptyList();
                }

                default -> Collections.emptyList();
            };
        }

        return Collections.emptyList();
    }

    private List<String> filterPrefix(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(p))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private List<String> onlineNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }

    private List<String> knownPlayerNames() {
        Set<String> names = new HashSet<>(onlineNames());
        Bukkit.getWhitelistedPlayers().forEach(p -> { if (p.getName() != null) names.add(p.getName()); });
        var list = new ArrayList<>(names);
        list.sort(String.CASE_INSENSITIVE_ORDER);
        return list;
    }

    private List<String> gameRuleNames() {
        return Arrays.stream(GameRule.values()).map(GameRule::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }
}
