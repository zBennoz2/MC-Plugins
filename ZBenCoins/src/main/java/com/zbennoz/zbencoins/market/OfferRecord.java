package com.zbennoz.zbencoins.market;

import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Datensatz für ein Marktangebot inklusive Escrow-Item.
 */
public class OfferRecord {

    private final int id;
    private final UUID sellerUuid;
    private final String sellerName;
    private final UUID buyerUuid;
    private final ItemStack item;
    private final int amount;
    private final long price;
    private final OfferStatus status;
    private final Instant expiresAt;
    private final Instant createdAt;
    private final boolean delivered;

    public OfferRecord(int id, UUID sellerUuid, String sellerName, UUID buyerUuid, ItemStack item, int amount, long price,
                       OfferStatus status, Instant expiresAt, Instant createdAt, boolean delivered) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.buyerUuid = buyerUuid;
        this.item = item;
        this.amount = amount;
        this.price = price;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.delivered = delivered;
    }

    public int getId() {
        return id;
    }

    public UUID getSellerUuid() {
        return sellerUuid;
    }

    public String getSellerName() {
        return sellerName;
    }

    public Optional<UUID> getBuyerUuid() {
        return Optional.ofNullable(buyerUuid);
    }

    public ItemStack getItem() {
        return item.clone();
    }

    public int getAmount() {
        return amount;
    }

    public long getPrice() {
        return price;
    }

    public OfferStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isDelivered() {
        return delivered;
    }
}
