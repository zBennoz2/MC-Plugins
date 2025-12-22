package com.zbennoz.zbencityjobs.gui;

import com.zbennoz.zbencityjobs.model.Listing;
import com.zbennoz.zbencityjobs.service.MarketService;
import com.zbennoz.zbencityjobs.service.CoinService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MarketGUI {
    private final MarketService marketService;
    private final CoinService coinService;
    private final int size;
    private final Map<UUID, Map<Integer, Integer>> openListings = new HashMap<>();

    public MarketGUI(MarketService marketService, CoinService coinService, int size) {
        this.marketService = marketService;
        this.coinService = coinService;
        this.size = size;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(player, size, "Markt");
        Map<Integer, Integer> slots = new HashMap<>();
        int slot = 0;
        for (Listing listing : marketService.getListings()) {
            if (slot >= size) break;
            ItemStack display = listing.getItem().clone();
            ItemMeta meta = display.getItemMeta();
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("§7Preis: §a" + coinService.formatAmount(listing.getPrice()));
            lore.add("§7Anbieter: §f" + listing.getSeller());
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            display.setItemMeta(meta);
            inventory.setItem(slot, display);
            slots.put(slot, listing.getId());
            slot++;
        }
        openListings.put(player.getUniqueId(), slots);
        player.openInventory(inventory);
    }

    public Optional<Integer> resolveListing(Player player, int slot) {
        Map<Integer, Integer> slots = openListings.get(player.getUniqueId());
        if (slots == null) return Optional.empty();
        return Optional.ofNullable(slots.get(slot));
    }
}
