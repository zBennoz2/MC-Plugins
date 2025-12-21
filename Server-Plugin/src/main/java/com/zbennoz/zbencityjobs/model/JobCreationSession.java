package com.zbennoz.zbencityjobs.model;

import org.bukkit.inventory.ItemStack;

public class JobCreationSession {
    public enum Stage { TYPE, DESCRIPTION, REWARD, DELIVERY_ITEM }

    private Stage stage = Stage.TYPE;
    private JobType type;
    private String description;
    private double reward;
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

    public double getReward() {
        return reward;
    }

    public void setReward(double reward) {
        this.reward = reward;
    }

    public ItemStack getDeliveryItem() {
        return deliveryItem;
    }

    public void setDeliveryItem(ItemStack deliveryItem) {
        this.deliveryItem = deliveryItem;
    }
}
