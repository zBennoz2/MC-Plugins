package com.zbennoz.zbenskills.listener;

import com.zbennoz.zbenskills.ZBenSkillsPlugin;
import com.zbennoz.zbenskills.model.SkillType;
import com.zbennoz.zbenskills.service.SkillBenefitService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class SkillPerkListener implements Listener {
    private final ZBenSkillsPlugin plugin;
    private final SkillBenefitService benefitService;
    private final Map<UUID, Biome> lastBiome = new HashMap<>();

    private static final Set<Material> MINING_BLOCKS = EnumSet.of(Material.STONE, Material.DEEPSLATE,
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE, Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE);
    private static final Set<Material> WOOD_BLOCKS = EnumSet.of(Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG,
            Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG);
    private static final Set<Material> CROP_BLOCKS = EnumSet.of(Material.WHEAT, Material.CARROTS, Material.POTATOES,
            Material.BEETROOTS);

    public SkillPerkListener(ZBenSkillsPlugin plugin, SkillBenefitService benefitService) {
        this.plugin = plugin;
        this.benefitService = benefitService;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material type = event.getBlock().getType();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (MINING_BLOCKS.contains(type)) {
            applyDoubleDrops(player, event.getBlock(), tool, SkillType.MINING, "double-drop-chance");
            applyHaste(player, SkillType.MINING, "haste-seconds");
        }
        if (WOOD_BLOCKS.contains(type)) {
            applyDoubleDrops(player, event.getBlock(), tool, SkillType.WOODCUTTING, "extra-log-chance");
            applyHaste(player, SkillType.WOODCUTTING, "wood-haste-seconds");
        }
        if (CROP_BLOCKS.contains(type)) {
            applyDoubleDrops(player, event.getBlock(), tool, SkillType.FARMING, "extra-crop-chance");
            seedRefund(player, type, SkillType.FARMING, "seed-refund-chance");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Entity caught = event.getCaught();
        if (caught instanceof Item item) {
            double doubleChance = benefitService.value(uuid, SkillType.FISHING, "double-fish-chance");
            if (ThreadLocalRandom.current().nextDouble() < doubleChance) {
                ItemStack copy = item.getItemStack().clone();
                item.getWorld().dropItemNaturally(item.getLocation(), copy);
            }
        }
        double treasureChance = benefitService.value(uuid, SkillType.FISHING, "treasure-chance");
        if (ThreadLocalRandom.current().nextDouble() < treasureChance) {
            ItemStack treasure = randomTreasure();
            player.getWorld().dropItemNaturally(player.getLocation(), treasure);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            double bonus = benefitService.value(player.getUniqueId(), SkillType.COMBAT, "damage-bonus");
            event.setDamage(event.getDamage() * (1 + bonus));
        }
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            double bonus = benefitService.value(player.getUniqueId(), SkillType.ARCHERY, "projectile-damage");
            event.setDamage(event.getDamage() * (1 + bonus));
            double critChance = benefitService.value(player.getUniqueId(), SkillType.ARCHERY, "crit-chance");
            if (ThreadLocalRandom.current().nextDouble() < critChance) {
                event.setDamage(event.getDamage() * 1.25);
            }
        }
    }

    @EventHandler
    public void onBrew(BrewEvent event) {
        if (event.getContents().getViewers().isEmpty() || !(event.getContents().getViewers().get(0) instanceof Player player)) {
            return;
        }
        double chance = benefitService.value(player.getUniqueId(), SkillType.ALCHEMY, "brew-bonus-chance");
        if (chance <= 0) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (int i = 0; i < event.getContents().getContents().length; i++) {
                ItemStack stack = event.getContents().getItem(i);
                if (stack == null || stack.getType() == Material.AIR) {
                    continue;
                }
                if (ThreadLocalRandom.current().nextDouble() < chance) {
                    ItemStack copy = stack.clone();
                    copy.setAmount(1);
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation().add(0.5, 0.8, 0.5), copy);
                }
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }
        double reduction = benefitService.value(player.getUniqueId(), SkillType.ENCHANTING, "anvil-discount");
        if (reduction <= 0) {
            return;
        }
        int cost = event.getInventory().getRepairCost();
        if (cost <= 0) {
            return;
        }
        int newCost = Math.max(1, (int) Math.ceil(cost * (1 - reduction)));
        event.getInventory().setRepairCost(newCost);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        if (!isBuildingBlock(event.getBlockPlaced().getType())) {
            return;
        }
        double chance = benefitService.value(player.getUniqueId(), SkillType.BUILDING, "refund-chance");
        if (ThreadLocalRandom.current().nextDouble() < chance) {
            ItemStack refund = new ItemStack(event.getBlockPlaced().getType());
            Map<Integer, ItemStack> remaining = player.getInventory().addItem(refund);
            remaining.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTrade(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory() instanceof MerchantInventory) || event.getSlot() != 2 || event.getInventory().getType() != InventoryType.MERCHANT) {
            return;
        }
        double chance = benefitService.value(player.getUniqueId(), SkillType.TRADING, "emerald-cashback");
        if (chance <= 0) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (ThreadLocalRandom.current().nextDouble() < chance) {
                player.getInventory().addItem(new ItemStack(Material.EMERALD));
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Biome biome = player.getLocation().getBlock().getBiome();
        Biome last = lastBiome.get(player.getUniqueId());
        if (biome != last) {
            lastBiome.put(player.getUniqueId(), biome);
            double seconds = benefitService.value(player.getUniqueId(), SkillType.EXPLORATION, "speed-seconds");
            if (seconds > 0) {
                int ticks = (int) (seconds * 20);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ticks, 0, true, false, true));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        double chance = benefitService.value(player.getUniqueId(), SkillType.CRAFTING, "extra-output-chance");
        if (ThreadLocalRandom.current().nextDouble() < chance) {
            ItemStack bonus = event.getRecipe().getResult().clone();
            bonus.setAmount(Math.max(1, bonus.getAmount()));
            Map<Integer, ItemStack> remaining = player.getInventory().addItem(bonus);
            remaining.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
    }

    private void applyDoubleDrops(Player player, Block block, ItemStack tool, SkillType skill, String key) {
        if (block == null) {
            return;
        }
        double chance = benefitService.value(player.getUniqueId(), skill, key);
        if (chance <= 0 || block.getType() == Material.AIR) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() < chance) {
            List<ItemStack> drops = block.getDrops(tool, player).stream().map(ItemStack::clone).toList();
            drops.forEach(drop -> block.getWorld().dropItemNaturally(block.getLocation(), drop));
        }
    }

    private void applyHaste(Player player, SkillType skill, String key) {
        double seconds = benefitService.value(player.getUniqueId(), skill, key);
        if (seconds <= 0) {
            return;
        }
        int ticks = (int) (seconds * 20);
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, ticks, 0, true, false, true));
    }

    private void seedRefund(Player player, Material crop, SkillType skill, String key) {
        double chance = benefitService.value(player.getUniqueId(), skill, key);
        if (ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }
        ItemStack seed;
        if (crop == Material.CARROTS || crop == Material.POTATOES || crop == Material.BEETROOTS) {
            seed = new ItemStack(crop);
        } else {
            seed = new ItemStack(Material.WHEAT_SEEDS);
        }
        Map<Integer, ItemStack> remaining = player.getInventory().addItem(seed);
        remaining.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }

    private ItemStack randomTreasure() {
        ItemStack[] pool = new ItemStack[]{
                new ItemStack(Material.NAUTILUS_SHELL),
                new ItemStack(Material.PRISMARINE_CRYSTALS, 2),
                new ItemStack(Material.BONE, 3),
                new ItemStack(Material.SALMON, 2)
        };
        return pool[ThreadLocalRandom.current().nextInt(pool.length)].clone();
    }

    private boolean isBuildingBlock(Material type) {
        String name = type.name();
        return name.contains("BRICK") || name.contains("PLANK") || name.contains("GLASS") || name.contains("STONE") || name.contains("CONCRETE") || name.contains("BRICKS");
    }
}
