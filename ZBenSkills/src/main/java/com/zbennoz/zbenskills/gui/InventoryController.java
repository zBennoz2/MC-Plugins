package com.zbennoz.zbenskills.gui;

import com.zbennoz.zbenskills.ZBenSkillsPlugin;
import com.zbennoz.zbenskills.config.SkillConfig;
import com.zbennoz.zbenskills.model.ChallengeDefinition;
import com.zbennoz.zbenskills.model.SkillNode;
import com.zbennoz.zbenskills.model.SkillType;
import com.zbennoz.zbenskills.service.MessageService;
import com.zbennoz.zbenskills.service.ChallengeService;
import com.zbennoz.zbenskills.service.SkillBenefitService;
import com.zbennoz.zbenskills.service.SkillService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InventoryController implements Listener {
    private final ZBenSkillsPlugin plugin;
    private final SkillService skillService;
    private final SkillConfig config;
    private final ChallengeService challengeService;
    private final MessageService messages;
    private final SkillBenefitService benefitService;

    public InventoryController(ZBenSkillsPlugin plugin, SkillService skillService, SkillConfig config, ChallengeService challengeService, MessageService messages, SkillBenefitService benefitService) {
        this.plugin = plugin;
        this.skillService = skillService;
        this.config = config;
        this.challengeService = challengeService;
        this.messages = messages;
        this.benefitService = benefitService;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.MAIN, null), 27, ChatColor.BLUE + "Skills");
        int slot = 0;
        for (SkillType type : SkillType.values()) {
            ItemStack stack = new ItemStack(Material.NETHER_STAR);
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + type.getDisplayName());
            List<String> lore = new ArrayList<>();
            int level = skillService.getLevel(player.getUniqueId(), type);
            int prestige = skillService.getPrestige(player.getUniqueId(), type);
            lore.add(ChatColor.GRAY + "Level: " + ChatColor.GREEN + level + ChatColor.GRAY + " / " + config.getMaxLevel());
            lore.add(ChatColor.GOLD + "Prestige: " + prestige + ChatColor.GRAY + " (" + ChatColor.YELLOW + String.format(java.util.Locale.GERMAN, "%.1f%%", config.getPrestigeBenefitMultiplier() * 100) + ChatColor.GRAY + " Bonus/Stufe)");
            lore.add(ChatColor.YELLOW + "Vorteile:");
            List<String> perks = benefitService.describe(player.getUniqueId(), type);
            for (int i = 0; i < Math.min(3, perks.size()); i++) {
                lore.add(perks.get(i));
            }
            meta.setLore(lore);
            stack.setItemMeta(meta);
            inv.setItem(slot++, stack);
        }
        player.openInventory(inv);
    }

    public void openTree(Player player, SkillType skill) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.TREE, skill), 54, ChatColor.DARK_AQUA + skill.getDisplayName() + "-Baum");
        List<SkillNode> nodes = config.getSkillNodes().get(skill);
        int slot = 0;
        for (SkillNode node : nodes) {
            ItemStack stack = new ItemStack(Material.BOOK);
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + node.getName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + node.getDescription());
            lore.add(ChatColor.GRAY + "Kosten: " + node.getCost() + " Skillpunkte");
            lore.add(ChatColor.GRAY + "Benötigtes Level: " + node.getRequiredLevel());
            lore.add(ChatColor.GRAY + "Prestige: " + (node.isPrestigeLocked() ? "Ja" : "Nein"));
            meta.setLore(lore);
            stack.setItemMeta(meta);
            inv.setItem(slot++, stack);
        }
        player.openInventory(inv);
    }

    public void openChallenges(Player player) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.CHALLENGES, null), 27, ChatColor.GOLD + "Aufgaben");
        int i = 0;
        for (ChallengeDefinition def : challengeService.getDailyChallenges()) {
            inv.setItem(i++, challengeItem(def, ChatColor.GREEN + "Daily"));
        }
        for (ChallengeDefinition def : challengeService.getWeeklyChallenges()) {
            inv.setItem(i++, challengeItem(def, ChatColor.YELLOW + "Weekly"));
        }
        player.openInventory(inv);
    }

    private ItemStack challengeItem(ChallengeDefinition def, String header) {
        ItemStack stack = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(header + ChatColor.GRAY + " " + def.getId());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.AQUA + def.getSkill().getDisplayName());
        lore.add(ChatColor.GRAY + "Ziel: " + def.getGoal());
        lore.add(ChatColor.GOLD + "Belohnung: " + def.getRewardPoints() + " Skillpunkte");
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    public void openAchievements(Player player) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.ACHIEVEMENTS, null), 54, ChatColor.DARK_GREEN + "Erfolge");
        int slot = 0;
        for (var def : config.getAchievements()) {
            ItemStack stack = new ItemStack(Material.EMERALD);
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + def.getName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + def.getDescription());
            lore.add(ChatColor.AQUA + def.getSkill().getDisplayName());
            lore.add(ChatColor.GRAY + "Ziel: Level " + def.getGoal());
            lore.add(ChatColor.GOLD + "+" + def.getSkillPoints() + " Skillpunkte");
            meta.setLore(lore);
            stack.setItemMeta(meta);
            inv.setItem(slot++, stack);
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        switch (holder.type()) {
            case MAIN -> {
                int slot = event.getRawSlot();
                SkillType[] values = SkillType.values();
                if (slot >= 0 && slot < values.length) {
                    openTree(player, values[slot]);
                }
            }
            case TREE -> {
                int slot = event.getRawSlot();
                List<SkillNode> nodes = config.getSkillNodes().get(holder.skill());
                if (slot >= 0 && slot < nodes.size()) {
                    SkillNode node = nodes.get(slot);
                    if (skillService.unlockNode(player, node)) {
                        Bukkit.getPluginManager().callEvent(new com.zbennoz.zbenskills.api.NodeUnlockEvent(player, node));
                        player.closeInventory();
                    }
                }
            }
            case ACHIEVEMENTS -> messages.send(player, "gui-achievements", Map.of());
            case CHALLENGES -> messages.send(player, "gui-challenges", Map.of());
        }
    }

    private record MenuHolder(MenuType type, SkillType skill) implements org.bukkit.inventory.InventoryHolder {
        @Override
        public Inventory getInventory() {
            return Bukkit.createInventory(this, 9);
        }
    }

    private enum MenuType { MAIN, TREE, CHALLENGES, ACHIEVEMENTS }
}
