package com.zbennoz.zbenskills.listener;

import com.zbennoz.zbenskills.config.SkillConfig;
import com.zbennoz.zbenskills.storage.PlayerSkillRepository;
import com.zbennoz.zbenskills.service.SkillService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerDataListener implements Listener {
    private final PlayerSkillRepository repository;
    private final SkillService service;
    private final SkillConfig config;

    public PlayerDataListener(PlayerSkillRepository repository, SkillService service, SkillConfig config) {
        this.repository = repository;
        this.service = service;
        this.config = config;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        repository.getProfile(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        repository.saveProfileAsync(repository.getProfile(event.getPlayer().getUniqueId()));
    }
}
