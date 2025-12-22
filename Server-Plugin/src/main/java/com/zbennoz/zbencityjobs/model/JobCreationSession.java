package com.zbennoz.zbencityjobs.model;

import org.bukkit.inventory.ItemStack;

public class JobCreationSession {
    public enum Stage { TYPE, DESCRIPTION, REWARD, DELIVERY_ITEM }

    private Stage stage = Stage.TYPE;
    private JobType type;
    private String description;
    private long reward;
    private ItemStack deliveryItem;

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public JobType getType() {
        return type;
    }

    public void setType(JobType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getReward() {
        return reward;
    }

    public void setReward(long reward) {
        this.reward = reward;
    }

    public ItemStack getDeliveryItem() {
        return deliveryItem;
    }

    public void setDeliveryItem(ItemStack deliveryItem) {
        this.deliveryItem = deliveryItem;
    }
}
