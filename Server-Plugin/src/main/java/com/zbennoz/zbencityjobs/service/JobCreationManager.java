package com.zbennoz.zbencityjobs.service;

import com.zbennoz.zbencityjobs.model.JobCreationSession;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JobCreationManager {
    private final Map<UUID, JobCreationSession> sessions = new ConcurrentHashMap<>();

    public JobCreationSession start(Player player) {
        JobCreationSession session = new JobCreationSession();
        sessions.put(player.getUniqueId(), session);
        return session;
    }

    public Optional<JobCreationSession> get(Player player) {
        return Optional.ofNullable(sessions.get(player.getUniqueId()));
    }

    public void clear(Player player) {
        sessions.remove(player.getUniqueId());
    }
}
