package com.zbennoz.zbenskills.service;

import com.zbennoz.zbenskills.config.BenefitValue;
import com.zbennoz.zbenskills.config.SkillConfig;
import com.zbennoz.zbenskills.data.PlayerProfile;
import com.zbennoz.zbenskills.model.SkillType;
import com.zbennoz.zbenskills.storage.PlayerSkillRepository;
import org.bukkit.ChatColor;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class SkillBenefitService {
    private static final DecimalFormat PERCENT = new DecimalFormat("0.##");
    private static final DecimalFormat SECONDS = new DecimalFormat("0.0");
    private final SkillConfig config;
    private final PlayerSkillRepository repository;

    public SkillBenefitService(SkillConfig config, PlayerSkillRepository repository) {
        this.config = config;
        this.repository = repository;
    }

    public double value(UUID uuid, SkillType skill, String key) {
        PlayerProfile profile = repository.getProfile(uuid);
        int level = profile.getLevels().getOrDefault(skill, 1);
        int prestige = profile.getPrestige().getOrDefault(skill, 0);
        return config.getBenefitValue(skill, key, level, prestige);
    }

    public List<String> describe(UUID uuid, SkillType skill) {
        PlayerProfile profile = repository.getProfile(uuid);
        int level = profile.getLevels().getOrDefault(skill, 1);
        int prestige = profile.getPrestige().getOrDefault(skill, 0);
        Map<String, BenefitValue> values = config.getBenefitConfig(skill);
        List<String> lines = new ArrayList<>();
        for (var entry : values.entrySet()) {
            double current = entry.getValue().calculate(level, prestige, config.getPrestigeBenefitMultiplier());
            lines.add(ChatColor.GRAY + "- " + displayName(skill, entry.getKey()) + ChatColor.YELLOW + " " + formatValue(entry.getKey(), current));
        }
        if (lines.isEmpty()) {
            lines.add(ChatColor.GRAY + "Keine aktiven Vorteile definiert.");
        }
        return lines;
    }

    private String displayName(SkillType skill, String key) {
        return switch ((skill.name() + ":" + key).toLowerCase(Locale.ROOT)) {
            case "mining:double-drop-chance" -> "Doppelter Erz-Drop";
            case "mining:haste-seconds" -> "Haste-Dauer";
            case "mining:xp-bonus" -> "Bonus-XP";
            case "woodcutting:extra-log-chance" -> "Extra-Stämme";
            case "woodcutting:wood-haste-seconds" -> "Schnellerer Holzabbau";
            case "farming:extra-crop-chance" -> "Zusätzliche Ernte";
            case "farming:seed-refund-chance" -> "Samen-Erstattung";
            case "fishing:treasure-chance" -> "Schatzchance";
            case "fishing:double-fish-chance" -> "Doppelter Fang";
            case "combat:damage-bonus" -> "Nahkampf-Schaden";
            case "archery:crit-chance" -> "Kritische Treffer";
            case "archery:projectile-damage" -> "Fernkampf-Schaden";
            case "alchemy:brew-bonus-chance" -> "Zusätzliche Tränke";
            case "enchanting:anvil-discount" -> "Schmiedekosten";
            case "building:refund-chance" -> "Baumaterial-Erstattung";
            case "trading:emerald-cashback" -> "Smaragd-Rabatt";
            case "exploration:speed-seconds" -> "Reise-Tempo";
            case "crafting:extra-output-chance" -> "Zusätzliches Crafting";
            default -> key;
        };
    }

    private String formatValue(String key, double value) {
        if (key.endsWith("seconds")) {
            return SECONDS.format(value) + "s";
        }
        if (key.contains("damage") || key.contains("bonus") || key.contains("chance") || key.contains("refund") || key.contains("cashback") || key.contains("crit")) {
            return PERCENT.format(value * 100) + "%";
        }
        return String.valueOf(value);
    }
}
