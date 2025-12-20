package com.zbennoz.zbenenchants.command;

import com.zbennoz.zbenenchants.core.ZBenEnchantsPlugin;
import com.zbennoz.zbenenchants.enchant.CustomEnchant;
import com.zbennoz.zbenenchants.util.ItemUtil;
import com.zbennoz.zbenenchants.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * /zbenenchants Kommando.
 */
public class ZBenEnchantsCommand implements CommandExecutor, TabCompleter {

    private final ZBenEnchantsPlugin plugin;

    public ZBenEnchantsCommand(ZBenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            MessageUtil.send(plugin, sender, "usage");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("zbenenchants.reload")) {
                MessageUtil.send(plugin, sender, "no-permission");
                return true;
            }
            plugin.reloadConfig();
            MessageUtil.send(plugin, sender, "reloaded");
            return true;
        }

        if (args[0].equalsIgnoreCase("givebook")) {
            if (!sender.hasPermission("zbenenchants.givebook")) {
                MessageUtil.send(plugin, sender, "no-permission");
                return true;
            }
            if (args.length < 4) {
                MessageUtil.send(plugin, sender, "usage-givebook");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                MessageUtil.send(plugin, sender, "player-not-found");
                return true;
            }
            CustomEnchant enchant = parseEnchant(args[2]);
            if (enchant == null) {
                MessageUtil.send(plugin, sender, "unknown-enchant");
                return true;
            }
            int level;
            try {
                level = Integer.parseInt(args[3]);
            } catch (NumberFormatException ex) {
                MessageUtil.send(plugin, sender, "invalid-number");
                return true;
            }
            level = Math.max(1, Math.min(level, enchant.getMaxLevel()));
            ItemStack book = ItemUtil.createEnchantBook(plugin, enchant, level);
            target.getInventory().addItem(book);
            MessageUtil.send(plugin, sender, "book-given", target.getName());
            MessageUtil.send(plugin, target, "book-received", enchant.getDisplayName() + " " + ItemUtil.roman(level));
            return true;
        }

        MessageUtil.send(plugin, sender, "usage");
        return true;
    }

    private CustomEnchant parseEnchant(String name) {
        return Arrays.stream(CustomEnchant.values())
                .filter(enchant -> enchant.getKey().equalsIgnoreCase(name) || enchant.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("givebook", "reload");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("givebook")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("givebook")) {
            List<String> names = new ArrayList<>();
            for (CustomEnchant enchant : CustomEnchant.values()) {
                names.add(enchant.getKey());
            }
            return names;
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("givebook")) {
            return List.of("1", "2", "3");
        }
        return List.of();
    }
}
