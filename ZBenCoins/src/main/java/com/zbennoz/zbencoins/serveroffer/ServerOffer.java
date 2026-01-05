package com.zbennoz.zbencoins.serveroffer;

import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.Optional;

/**
 * Abbild eines Server-Angebots.
 */
public class ServerOffer {

    private final int id;
    private final ServerOfferType type;
    private final ItemStack itemStack;
    private final long pricePerItem;
    private final boolean enabled;
    private final Integer minAmount;
    private final Integer maxAmount;
    private final boolean periodLimitEnabled;
    private final long periodTicks;
    private final Integer periodMaxAmount;
    private final int periodUsedAmount;
    private final Long periodStartMillis;
    private final String createdBy;
    private final Instant createdAt;

    public ServerOffer(int id, ServerOfferType type, ItemStack itemStack, long pricePerItem, boolean enabled,
                       Integer minAmount, Integer maxAmount, boolean periodLimitEnabled, long periodTicks,
                       Integer periodMaxAmount, int periodUsedAmount, Long periodStartMillis,
                       String createdBy, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.itemStack = itemStack;
        this.pricePerItem = pricePerItem;
        this.enabled = enabled;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.periodLimitEnabled = periodLimitEnabled;
        this.periodTicks = periodTicks;
        this.periodMaxAmount = periodMaxAmount;
        this.periodUsedAmount = periodUsedAmount;
        this.periodStartMillis = periodStartMillis;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }

    public ServerOfferType getType() { return type; }

    public ItemStack getItemStack() { return itemStack.clone(); }

    public long getPricePerItem() { return pricePerItem; }

    public boolean isEnabled() { return enabled; }

    public Optional<Integer> getMinAmount() { return Optional.ofNullable(minAmount); }

    public Optional<Integer> getMaxAmount() { return Optional.ofNullable(maxAmount); }

    public boolean isPeriodLimitEnabled() { return periodLimitEnabled; }

    public long getPeriodTicks() { return periodTicks; }

    public Optional<Integer> getPeriodMaxAmount() { return Optional.ofNullable(periodMaxAmount); }

    public int getPeriodUsedAmount() { return periodUsedAmount; }

    public Optional<Long> getPeriodStartMillis() { return Optional.ofNullable(periodStartMillis); }

    public Optional<String> getCreatedBy() { return Optional.ofNullable(createdBy); }

    public Optional<Instant> getCreatedAt() { return Optional.ofNullable(createdAt); }
}
