package com.zbennoz.zbencityjobs.model;

import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.UUID;

public class Job {
    private int id;
    private final JobType type;
    private final UUID requester;
    private UUID worker;
    private final double reward;
    private final boolean escrow;
    private JobStatus status;
    private final String description;
    private ItemStack deliveryItem;
    private final long createdAt;

    public Job(int id, JobType type, UUID requester, UUID worker, double reward, boolean escrow,
               JobStatus status, String description, ItemStack deliveryItem, long createdAt) {
        this.id = id;
        this.type = type;
        this.requester = requester;
        this.worker = worker;
        this.reward = reward;
        this.escrow = escrow;
        this.status = status;
        this.description = description;
        this.deliveryItem = deliveryItem;
        this.createdAt = createdAt;
    }

    public Job(JobType type, UUID requester, double reward, boolean escrow, String description, ItemStack deliveryItem) {
        this(0, type, requester, null, reward, escrow, JobStatus.OPEN, description, deliveryItem, Instant.now().toEpochMilli());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public JobType getType() {
        return type;
    }

    public UUID getRequester() {
        return requester;
    }

    public UUID getWorker() {
        return worker;
    }

    public void setWorker(UUID worker) {
        this.worker = worker;
    }

    public double getReward() {
        return reward;
    }

    public boolean isEscrow() {
        return escrow;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public ItemStack getDeliveryItem() {
        return deliveryItem;
    }

    public void setDeliveryItem(ItemStack deliveryItem) {
        this.deliveryItem = deliveryItem;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
