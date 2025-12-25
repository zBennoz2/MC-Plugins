package com.zbennoz.zbencoins.job;

import org.bukkit.Material;

import java.time.Instant;
import java.util.UUID;

/**
 * Vollständige Darstellung eines Jobs.
 */
public class JobRecord {

    private final int id;
    private final JobType type;
    private final String title;
    private final String description;
    private final long reward;
    private final UUID creatorUuid;
    private final String creatorName;
    private final UUID assigneeUuid;
    private final String assigneeName;
    private final JobStatus status;
    private final Instant expiresAt;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Material itemType;
    private final int itemAmount;
    private final boolean completionRequested;

    public JobRecord(int id, JobType type, String title, String description, long reward, UUID creatorUuid,
                     String creatorName, UUID assigneeUuid, String assigneeName, JobStatus status, Instant expiresAt,
                     Instant createdAt, Instant updatedAt, Material itemType, int itemAmount,
                     boolean completionRequested) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.description = description;
        this.reward = reward;
        this.creatorUuid = creatorUuid;
        this.creatorName = creatorName;
        this.assigneeUuid = assigneeUuid;
        this.assigneeName = assigneeName;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.itemType = itemType;
        this.itemAmount = itemAmount;
        this.completionRequested = completionRequested;
    }

    public int getId() {
        return id;
    }

    public JobType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public long getReward() {
        return reward;
    }

    public UUID getCreatorUuid() {
        return creatorUuid;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public UUID getAssigneeUuid() {
        return assigneeUuid;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public JobStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Material getItemType() {
        return itemType;
    }

    public int getItemAmount() {
        return itemAmount;
    }

    public boolean isCompletionRequested() {
        return completionRequested;
    }
}
