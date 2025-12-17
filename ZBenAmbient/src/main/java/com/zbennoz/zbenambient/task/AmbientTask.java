package com.zbennoz.zbenambient.task;

import com.zbennoz.zbenambient.ZBenAmbientPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Random;

public class AmbientTask implements Runnable {

    private final ZBenAmbientPlugin plugin;
    private final Random random = new Random();

    public AmbientTask(ZBenAmbientPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline()) continue;
            if (random.nextDouble() > 0.3) continue;
            World world = player.getWorld();
            world.spawnParticle(Particle.FALLING_SPORE_BLOSSOM, player.getLocation(), plugin.getConfig().getInt("intensity.leaves", 2));
            if (player.getLocation().getBlock().getBiome().name().toLowerCase().contains("swamp")) {
                world.spawnParticle(Particle.CLOUD, player.getLocation(), plugin.getConfig().getInt("intensity.fog", 1));
            }
        }
    }
}
