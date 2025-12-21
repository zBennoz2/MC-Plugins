package com.zbennoz.zbenskills.listener;

import com.zbennoz.zbenskills.config.SkillConfig;
import com.zbennoz.zbenskills.model.SkillType;
import com.zbennoz.zbenskills.service.SkillService;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class SkillXpListener implements Listener {
    private final SkillService skillService;
    private final SkillConfig config;

    public SkillXpListener(SkillService skillService, SkillConfig config) {
        this.skillService = skillService;
        this.config = config;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Material type = event.getBlock().getType();
        if (type.toString().contains("LOG")) {
            skillService.addXp(event.getPlayer(), SkillType.WOODCUTTING, 4.0, "wood:" + type);
        } else if (type == Material.STONE || type == Material.DEEPSLATE) {
            skillService.addXp(event.getPlayer(), SkillType.MINING, 2.0, "mine:stone");
        } else if (type == Material.DIAMOND_ORE || type == Material.DEEPSLATE_DIAMOND_ORE) {
            skillService.addXp(event.getPlayer(), SkillType.MINING, 20.0, "mine:diamond");
        } else if (type == Material.WHEAT || type == Material.CARROTS || type == Material.POTATOES) {
            skillService.addXp(event.getPlayer(), SkillType.FARMING, 3.0, "farming:" + type);
        } else if (type == Material.OAK_PLANKS || type == Material.BRICKS) {
            skillService.addXp(event.getPlayer(), SkillType.BUILDING, 2.0, "build:" + type);
        }
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            skillService.addXp(event.getPlayer(), SkillType.FISHING, 6.0, "fish");
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;
        if (config.isDisableSpawnerMobs() && event.getEntity().getEntitySpawnReason() != null && event.getEntity().getEntitySpawnReason().name().equalsIgnoreCase("SPAWNER")) {
            return;
        }
        EntityType type = event.getEntityType();
        if (type == EntityType.SKELETON || type == EntityType.ZOMBIE) {
            skillService.addXp(event.getEntity().getKiller(), SkillType.COMBAT, 5.0, "combat:" + type);
        } else if (type == EntityType.CREEPER || type == EntityType.BLAZE) {
            skillService.addXp(event.getEntity().getKiller(), SkillType.ARCHERY, 8.0, "archery:" + type);
        }
    }

    @EventHandler
    public void onBrew(BrewEvent event) {
        if (event.getContents() != null && event.getContents().getViewers().stream().findFirst().isPresent()) {
            skillService.addXp((org.bukkit.entity.Player) event.getContents().getViewers().get(0), SkillType.ALCHEMY, 5.0, "brew");
        }
    }

    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        if (event.getView().getPlayer() != null) {
            skillService.addXp((org.bukkit.entity.Player) event.getView().getPlayer(), SkillType.ENCHANTING, 4.0, "enchant");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null && event.getClickedBlock().getBiome() != null) {
            Biome biome = event.getClickedBlock().getBiome();
            skillService.addXp(event.getPlayer(), SkillType.EXPLORATION, 1.0, "biome:" + biome.name());
        }
    }
}
