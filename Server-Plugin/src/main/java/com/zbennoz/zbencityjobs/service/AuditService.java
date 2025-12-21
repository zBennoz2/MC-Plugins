package com.zbennoz.zbencityjobs.service;

import com.zbennoz.zbencityjobs.repository.AuditLogRepository;
import org.bukkit.Bukkit;

import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

public class AuditService {
    private final AuditLogRepository repository;
    private final boolean debug;

    public AuditService(AuditLogRepository repository, boolean debug) {
        this.repository = repository;
        this.debug = debug;
    }

    public void log(UUID actor, String action, String context) {
        try {
            repository.insert(actor != null ? actor.toString() : null, action, context, Instant.now().toEpochMilli());
        } catch (SQLException e) {
            if (debug) {
                Bukkit.getLogger().warning("Audit log failed: " + e.getMessage());
            }
        }
        if (debug) {
            Bukkit.getLogger().info("[Audit] " + action + " - " + context);
        }
    }
}
