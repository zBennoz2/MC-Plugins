package com.zbennoz.zbencoins.listener;

import com.zbennoz.zbencoins.service.JobService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Liest Eingaben für Job-Erstellung.
 */
public class JobChatListener implements Listener {

    private final JobService jobService;

    public JobChatListener(JobService jobService) {
        this.jobService = jobService;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        boolean handled = jobService.handleChat(player, event.getMessage());
        if (handled) {
            event.setCancelled(true);
        }
    }
}
