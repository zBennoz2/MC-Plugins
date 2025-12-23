package com.zbennoz.zbentimber.command;

import com.zbennoz.zbentimber.PlayerSettingsStorage;
import com.zbennoz.zbentimber.ZBenTimberPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TimberCommand implements CommandExecutor, TabCompleter {
    private final ZBenTimberPlugin plugin;
    private final PlayerSettingsStorage storage;

    public TimberCommand(ZBenTimberPlugin plugin, PlayerSettingsStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            plugin.getMessages().send(sender, "invalid-argument", Map.of());
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "leaves" -> handleLeaves(sender, args);
            case "reload" -> handleReload(sender);
            default -> plugin.getMessages().send(sender, "invalid-argument", Map.of());
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("zbentimber.admin")) {
            plugin.getMessages().send(sender, "no-permission", Map.of());
            return;
        }
        plugin.reloadConfiguration();
        plugin.getMessages().send(sender, "reload", Map.of());
    }

    private void handleLeaves(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessages().send(sender, "not-player", Map.of());
            return;
        }
        boolean current = storage.isLeavesEnabled(player.getUniqueId(), plugin.getLeavesDefault());
        boolean target = current;
        if (args.length >= 2) {
            if (args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("an")) {
                target = true;
            } else if (args[1].equalsIgnoreCase("off") || args[1].equalsIgnoreCase("aus")) {
                target = false;
            } else {
                plugin.getMessages().send(player, "invalid-argument", Map.of());
                return;
            }
        } else {
            target = !current;
        }
        storage.setLeaves(player.getUniqueId(), target);
        plugin.getMessages().send(player, target ? "toggle-on" : "toggle-off", Map.of());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> base = new ArrayList<>();
            base.add("leaves");
            if (sender.hasPermission("zbentimber.admin")) {
                base.add("reload");
            }
            return base;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("leaves")) {
            return List.of("on", "off", "an", "aus");
        }
        return List.of();
    }
}
