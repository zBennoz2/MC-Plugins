package com.zbennoz.zbenskills.command;

import com.zbennoz.zbenskills.config.SkillConfig;
import com.zbennoz.zbenskills.gui.InventoryController;
import com.zbennoz.zbenskills.model.SkillType;
import com.zbennoz.zbenskills.service.SkillService;
import com.zbennoz.zbenskills.storage.PlayerSkillRepository;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SkillsCommand implements CommandExecutor, TabCompleter {
    private final InventoryController controller;
    private final SkillService skillService;
    private final SkillConfig config;
    private final PlayerSkillRepository repository;

    public SkillsCommand(InventoryController controller, SkillService skillService, SkillConfig config, PlayerSkillRepository repository) {
        this.controller = controller;
        this.skillService = skillService;
        this.config = config;
        this.repository = repository;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur Spieler.");
            return true;
        }
        if (args.length == 0) {
            controller.openMain(player);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "challenges" -> controller.openChallenges(player);
            case "achievements" -> controller.openAchievements(player);
            case "prestige" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Nutze /skills prestige <skill>");
                    return true;
                }
                try {
                    SkillType type = SkillType.valueOf(args[1].toUpperCase(Locale.ROOT));
                    skillService.prestige(player, type);
                } catch (IllegalArgumentException e) {
                    player.sendMessage(ChatColor.RED + "Skill nicht gefunden.");
                }
            }
            case "reload" -> {
                player.getServer().getPluginManager().disablePlugin(player.getServer().getPluginManager().getPlugin("ZBenSkills"));
                player.getServer().getPluginManager().enablePlugin(player.getServer().getPluginManager().getPlugin("ZBenSkills"));
                player.sendMessage(ChatColor.GREEN + "ZBenSkills neu geladen.");
            }
            default -> controller.openMain(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("challenges", "achievements", "prestige", "reload");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("prestige")) {
            List<String> result = new ArrayList<>();
            for (SkillType type : SkillType.values()) {
                result.add(type.name().toLowerCase());
            }
            return result;
        }
        return List.of();
    }
}
