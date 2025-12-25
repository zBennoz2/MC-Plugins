package com.zbennoz.zbencoins.gui;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.market.OfferRecord;
import com.zbennoz.zbencoins.service.MarketService;
import com.zbennoz.zbencoins.util.Text;
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
 * Blätterbare Übersicht aller Angebote.
 */
public class MarketBrowseGui implements ManagedGui {

    private final ZBenCoinsPlugin plugin;
    private final MarketService marketService;
    private final Inventory inventory;
    private final Map<Integer, Integer> slotOfferMap = new HashMap<>();
    private final int page;

    public MarketBrowseGui(ZBenCoinsPlugin plugin, MarketService marketService, int page) {
        this.plugin = plugin;
        this.marketService = marketService;
        this.page = page;
        this.inventory = Bukkit.createInventory(this, 54, Text.colorize("&8&lMarktplatz"));
        build();
    }

    private void build() {
        List<OfferRecord> offers = marketService.listActive(page, 45);
        int slot = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(java.time.ZoneId.systemDefault());
        for (OfferRecord offer : offers) {
            if (slot >= 45) {
                break;
            }
            ItemStack display = offer.getItem();
            display.setAmount(Math.min(offer.getAmount(), display.getMaxStackSize()));
            List<String> lore = List.of(
                    "&7Verkäufer: &e" + offer.getSellerName(),
                    "&7Menge: &e" + offer.getAmount(),
                    "&7Preis: &6" + offer.getPrice() + " " + plugin.getConfig().getString("currency-name", "Coins"),
                    "&7Läuft ab: &c" + formatter.format(offer.getExpiresAt())
            );
            display = new GuiItemBuilder(display).lore(lore).build();
            inventory.setItem(slot, display);
            slotOfferMap.put(slot, offer.getId());
            slot++;
        }

        ItemStack back = new GuiItemBuilder(Material.BARRIER).name("&cZurück").build();
        ItemStack next = new GuiItemBuilder(Material.ARROW).name("&aWeiter").build();
        ItemStack previous = new GuiItemBuilder(Material.ARROW).name("&aZurück").build();
        inventory.setItem(45, previous);
        inventory.setItem(49, back);
        inventory.setItem(53, next);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int raw = event.getRawSlot();
        if (slotOfferMap.containsKey(raw)) {
            if (!player.hasPermission("zbencoins.market.buy")) {
                player.sendMessage(plugin.getConfigManager().message("market-no-buy"));
                return;
            }
            int offerId = slotOfferMap.get(raw);
            marketService.getOffer(offerId).ifPresent(offer ->
                    plugin.getGuiManager().openGui(player, new PurchaseConfirmGui(plugin, marketService, offer)));
            return;
        }
        if (raw == 45 && page > 0) {
            plugin.getGuiManager().openGui(player, new MarketBrowseGui(plugin, marketService, page - 1));
        } else if (raw == 53) {
            int max = marketService.countActiveOffers();
            int maxPage = Math.max(0, (int) Math.ceil(max / 45.0) - 1);
            if (page < maxPage) {
                plugin.getGuiManager().openGui(player, new MarketBrowseGui(plugin, marketService, page + 1));
            }
        } else if (raw == 49) {
            plugin.getGuiManager().openGui(player, new MarktMainGui(plugin, plugin.getCoinService(), plugin.getMarketService(), player));
        }
    }
}
