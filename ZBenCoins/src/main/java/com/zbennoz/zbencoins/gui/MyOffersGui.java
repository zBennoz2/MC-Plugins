package com.zbennoz.zbencoins.gui;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.market.OfferRecord;
import com.zbennoz.zbencoins.market.OfferStatus;
import com.zbennoz.zbencoins.service.MarketService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Übersicht der eigenen Angebote.
 */
public class MyOffersGui implements ManagedGui {

    private final ZBenCoinsPlugin plugin;
    private final MarketService marketService;
    private final Inventory inventory;
    private final Map<Integer, Integer> slotOfferMap = new HashMap<>();
    private final Player owner;

    public MyOffersGui(ZBenCoinsPlugin plugin, MarketService marketService, Player owner) {
        this.plugin = plugin;
        this.marketService = marketService;
        this.owner = owner;
        this.inventory = Bukkit.createInventory(this, 54, "Meine Angebote");
        build();
    }

    private void build() {
        List<OfferRecord> offers = marketService.listFiltered(marketService.getBrowseOptions(owner.getUniqueId()));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(java.time.ZoneId.systemDefault());
        int slot = 0;
        for (OfferRecord offer : offers) {
            if (!offer.getSellerUuid().equals(owner.getUniqueId())) continue;
            ItemStack display = offer.getItem();
            display.setAmount(Math.min(display.getMaxStackSize(), offer.getAmount()));
            List<String> lore = List.of(
                    "&7Status: &e" + offer.getStatus().name(),
                    "&7Preis: &6" + offer.getPrice(),
                    "&7Menge: &e" + offer.getAmount(),
                    "&7Ablauf: &c" + formatter.format(offer.getExpiresAt()),
                    offer.getStatus() == OfferStatus.ACTIVE ? "&cRechtsklick zum Abbrechen" : ""
            );
            display = new GuiItemBuilder(display).lore(lore).build();
            inventory.setItem(slot, display);
            slotOfferMap.put(slot, offer.getId());
            slot++;
            if (slot >= 45) break;
        }
        ItemStack back = new GuiItemBuilder(Material.BARRIER).name("&cZurück").build();
        inventory.setItem(49, back);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int raw = event.getRawSlot();
        if (raw == 49) {
            plugin.getGuiManager().openGui(player, new MarktMainGui(plugin, plugin.getCoinService(), plugin.getMarketService(), player));
            return;
        }
        if (slotOfferMap.containsKey(raw)) {
            int id = slotOfferMap.get(raw);
            marketService.getOffer(id).ifPresent(offer -> {
                if (offer.getStatus() == OfferStatus.ACTIVE) {
                    if (marketService.cancel(player, offer)) {
                        player.sendMessage(plugin.getConfigManager().message("offer-returned"));
                    }
                }
            });
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openGui(player, new MyOffersGui(plugin, marketService, player)));
        }
    }
}
