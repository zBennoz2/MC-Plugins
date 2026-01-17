package com.zben.essentials.model;

import java.time.Instant;
import java.util.UUID;

public class TpaRequest {
    private final UUID senderId;
    private final String senderName;
    private final UUID targetId;
    private final String targetName;
    private final boolean teleportHere;
    private final Instant createdAt;

    public TpaRequest(UUID senderId,
                      String senderName,
                      UUID targetId,
                      String targetName,
                      boolean teleportHere,
                      Instant createdAt) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.targetId = targetId;
        this.targetName = targetName;
        this.teleportHere = teleportHere;
        this.createdAt = createdAt;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public boolean isTeleportHere() {
        return teleportHere;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
