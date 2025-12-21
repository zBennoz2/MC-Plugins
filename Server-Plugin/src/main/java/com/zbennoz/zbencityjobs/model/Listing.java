package com.zbennoz.zbencityjobs.model;

import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.UUID;

public class Listing {
    private int id;
    private final UUID seller;
    private final double price;
    private final ItemStack item;
    private final long createdAt;

    public Listing(int id, UUID seller, double price, ItemStack item, long createdAt) {
        this.id = id;
        this.seller = seller;
        this.price = price;
        this.item = item;
        this.createdAt = createdAt;
    }

    public Listing(UUID seller, double price, ItemStack item) {
        this(0, seller, price, item, Instant.now().toEpochMilli());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UUID getSeller() {
        return seller;
    }

    public double getPrice() {
        return price;
    }

    public ItemStack getItem() {
        return item;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
