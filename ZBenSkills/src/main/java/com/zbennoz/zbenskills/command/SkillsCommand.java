package com.zbennoz.zbenskills.command;

import com.zbennoz.zbenskills.config.SkillConfig;
import com.zbennoz.zbenskills.gui.InventoryController;
import com.zbennoz.zbenskills.model.SkillType;
import com.zbennoz.zbenskills.service.MessageService;
import com.zbennoz.zbenskills.service.SkillBenefitService;
import com.zbennoz.zbenskills.service.SkillService;
import com.zbennoz.zbenskills.storage.PlayerSkillRepository;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SkillsCommand implements CommandExecutor, TabCompleter {
    private final InventoryController controller;
    private final SkillService skillService;
    private final SkillConfig config;
    private final PlayerSkillRepository repository;
    private final MessageService messages;
    private final SkillBenefitService benefitService;

    public SkillsCommand(InventoryController controller, SkillService skillService, SkillConfig config, PlayerSkillRepository repository, MessageService messages, SkillBenefitService benefitService) {
        this.controller = controller;
        this.skillService = skillService;
        this.config = config;
        this.repository = repository;
        this.messages = messages;
        this.benefitService = benefitService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "command-no-player", Map.of());
            return true;
        }
        if (!player.hasPermission("zbenskills.use")) {
            messages.send(player, "no-permission", Map.of());
            return true;
        }
        if (args.length == 0) {
            controller.openMain(player);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "challenges" -> controller.openChallenges(player);
            case "achievements" -> controller.openAchievements(player);
            case "info" -> {
                if (args.length < 2) {
                    messages.send(player, "usage-info", Map.of());
                    return true;
                }
                showInfo(player, args[1]);
            }
            case "prestige" -> {
                if (args.length < 2) {
                    messages.send(player, "usage-prestige", Map.of());
                    return true;
                }
                try {
                    SkillType type = SkillType.valueOf(args[1].toUpperCase(Locale.ROOT));
                    skillService.prestige(player, type);
                } catch (IllegalArgumentException e) {
                    messages.send(player, "invalid-skill", Map.of("skill", args[1]));
                }
            }
            case "reload" -> {
                if (!player.hasPermission("zbenskills.admin")) {
                    messages.send(player, "no-permission", Map.of());
                    return true;
                }
                player.getServer().getPluginManager().disablePlugin(player.getServer().getPluginManager().getPlugin("ZBenSkills"));
                player.getServer().getPluginManager().enablePlugin(player.getServer().getPluginManager().getPlugin("ZBenSkills"));
                messages.send(player, "reload", Map.of());
            }
            default -> controller.openMain(player);
        }
        return true;
    }

    private void showInfo(Player player, String skillName) {
        try {
            SkillType skill = SkillType.valueOf(skillName.toUpperCase(Locale.ROOT));
            var profile = skillService.getProfile(player.getUniqueId());
            int level = profile.getLevels().getOrDefault(skill, 1);
            int prestige = profile.getPrestige().getOrDefault(skill, 0);
            double xp = profile.getXp().getOrDefault(skill, 0.0);
            double needed = config.xpRequiredForLevel(level);
            double remaining = Math.max(0, needed - xp);
            messages.send(player, "info-header", Map.of(
                    "skill", skill.getDisplayName(),
                    "level", String.valueOf(level),
                    "max", String.valueOf(config.getMaxLevel()),
                    "prestige", String.valueOf(prestige)
            ));
            messages.send(player, "info-xp", Map.of(
                    "xp", String.format(Locale.GERMAN, "%.1f", xp),
                    "needed", String.format(Locale.GERMAN, "%.1f", needed)
            ));
            messages.send(player, "info-next-level", Map.of(
                    "remaining", String.format(Locale.GERMAN, "%.1f", remaining)
            ));
            messages.send(player, "info-bonus", Map.of(
                    "bonus", String.format(Locale.GERMAN, "%.1f", config.getPrestigeBenefitMultiplier() * 100)
            ));
            messages.send(player, "info-benefits", Map.of());
            benefitService.describe(player.getUniqueId(), skill).forEach(player::sendMessage);
        } catch (IllegalArgumentException ex) {
            messages.send(player, "invalid-skill", Map.of("skill", skillName));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("challenges", "achievements", "prestige", "info", "reload");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("prestige")) {
            List<String> result = new ArrayList<>();
            for (SkillType type : SkillType.values()) {
                result.add(type.name().toLowerCase());
            }
            return result;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            List<String> result = new ArrayList<>();
            for (SkillType type : SkillType.values()) {
                result.add(type.name().toLowerCase());
            }
            return result;
        }
        return List.of();
    }
}
