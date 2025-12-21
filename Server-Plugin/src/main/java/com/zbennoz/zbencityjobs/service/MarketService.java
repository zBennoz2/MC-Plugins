package com.zbennoz.zbencityjobs.service;

import com.zbennoz.zbencityjobs.ZBenCityJobs;
import com.zbennoz.zbencityjobs.model.Listing;
import com.zbennoz.zbencityjobs.repository.ListingRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MarketService {
    private final ZBenCityJobs plugin;
    private final ListingRepository repository;
    private final EconomyService economyService;
    private final AuditService auditService;
    private final boolean asyncWrites;
    private final Map<Integer, Listing> cache = new ConcurrentHashMap<>();

    public MarketService(ZBenCityJobs plugin, ListingRepository repository, EconomyService economyService, AuditService auditService, boolean asyncWrites) {
        this.plugin = plugin;
        this.repository = repository;
        this.economyService = economyService;
        this.auditService = auditService;
        this.asyncWrites = asyncWrites;
    }

    public void loadCache() {
        try {
            repository.loadAll().forEach(listing -> cache.put(listing.getId(), listing));
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load listings: " + e.getMessage());
        }
    }

    public Collection<Listing> getListings() {
        return cache.values();
    }

    public Optional<Listing> getListing(int id) {
        return Optional.ofNullable(cache.get(id));
    }

    public Optional<Listing> createListing(Player seller, ItemStack itemStack, double price) {
        Listing listing = new Listing(seller.getUniqueId(), price, itemStack);
        try {
            int id = repository.insert(listing);
            listing.setId(id);
            cache.put(id, listing);
            auditService.log(seller.getUniqueId(), "market.create", "listing=" + id);
            return Optional.of(listing);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create listing: " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean purchase(int id, Player buyer) {
        Optional<Listing> optional = getListing(id);
        if (optional.isEmpty()) return false;
        Listing listing = optional.get();
        if (!economyService.withdraw(buyer.getUniqueId(), listing.getPrice())) {
            return false;
        }
        economyService.deposit(listing.getSeller(), listing.getPrice());
        buyer.getInventory().addItem(listing.getItem());
        cache.remove(id);
        remove(id);
        auditService.log(buyer.getUniqueId(), "market.buy", "listing=" + id);
        return true;
    }

    private void remove(int id) {
        Runnable task = () -> {
            try {
                repository.delete(id);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete listing: " + e.getMessage());
            }
        };
        if (asyncWrites) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        } else {
            task.run();
        }
    }
}
