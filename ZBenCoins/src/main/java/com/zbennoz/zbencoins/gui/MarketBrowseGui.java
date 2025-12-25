package com.zbennoz.zbencoins.gui;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.market.MarketQueryOptions;
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
    private final MarketQueryOptions options;
    private final Player viewer;
    private int maxPage = 0;

    public MarketBrowseGui(ZBenCoinsPlugin plugin, MarketService marketService, MarketQueryOptions options, Player viewer) {
        this.plugin = plugin;
        this.marketService = marketService;
        this.options = options;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, 54, Text.colorize("&8&lMarktplatz"));
        build();
    }

    private void build() {
        List<OfferRecord> offers = marketService.listFiltered(options);
        int pageSize = 45;
        maxPage = Math.max(0, (int) Math.ceil(offers.size() / (double) pageSize) - 1);
        if (options.getPage() > maxPage) {
            options.setPage(maxPage);
        }
        int from = options.getPage() * pageSize;
        int to = Math.min(from + pageSize, offers.size());
        List<OfferRecord> pageOffers = offers.subList(Math.min(from, offers.size()), to);
        int slot = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(java.time.ZoneId.systemDefault());
        for (OfferRecord offer : pageOffers) {
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

        ItemStack coins = new GuiItemBuilder(Material.GOLD_NUGGET)
                .name("&eDeine Coins")
                .lore(List.of(
                        "&7Kontostand: &e" + plugin.getCoinService().getBalance(viewer.getUniqueId()) + " "
                                + plugin.getConfig().getString("currency-name", "Coins"),
                        "&7Nutze &e/pay &7für Transfers"
                ))
                .build();
        inventory.setItem(8, coins);

        ItemStack back = new GuiItemBuilder(Material.BARRIER).name("&cZurück").build();
        ItemStack next = new GuiItemBuilder(Material.ARROW).name("&aWeiter").lore(List.of("&7Seite " + (options.getPage() + 1) + "/" + (maxPage + 1))).build();
        ItemStack previous = new GuiItemBuilder(Material.ARROW).name("&aZurück").build();
        ItemStack search = new GuiItemBuilder(options.getSearchTerm().isBlank() ? Material.OAK_SIGN : Material.GLOW_INK_SAC)
                .name("&bSuche")
                .lore(List.of(
                        options.getSearchTerm().isBlank() ? "&7Klicke für Suche" : "&7Aktiv: &f" + options.getSearchTerm(),
                        "&7Unterstützt Item- & Verkäufernamen"
                ))
                .build();
        ItemStack reset = new GuiItemBuilder(Material.SUNFLOWER)
                .name("&eFilter zurücksetzen")
                .build();
        ItemStack sort = new GuiItemBuilder(Material.HOPPER)
                .name("&bSortierung")
                .lore(List.of("&7Aktuell: &f" + options.getSortOption().name().replace('_', ' ')))
                .glow(true)
                .build();
        ItemStack category = new GuiItemBuilder(Material.CHEST)
                .name("&aKategorie")
                .lore(List.of("&7Aktuell: &f" + options.getCategory().name()))
                .build();
        ItemStack online = new GuiItemBuilder(options.isOnlineOnly() ? Material.LIME_DYE : Material.GRAY_DYE)
                .name("&aNur Online")
                .lore(List.of(options.isOnlineOnly() ? "&7Aktiv" : "&7Deaktiviert"))
                .build();
        ItemStack pageInfo = new GuiItemBuilder(Material.PAPER)
                .name("&7Seite " + (options.getPage() + 1) + " / " + (maxPage + 1))
                .build();
        inventory.setItem(45, previous);
        inventory.setItem(46, search);
        inventory.setItem(47, reset);
        inventory.setItem(48, sort);
        inventory.setItem(49, back);
        inventory.setItem(50, category);
        inventory.setItem(51, online);
        inventory.setItem(52, pageInfo);
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
        if (raw == 45 && options.getPage() > 0) {
            MarketQueryOptions newOptions = options.copy();
            newOptions.setPage(options.getPage() - 1);
            reopen(player, newOptions);
        } else if (raw == 53 && options.getPage() < maxPage) {
            MarketQueryOptions newOptions = options.copy();
            newOptions.setPage(options.getPage() + 1);
            reopen(player, newOptions);
        } else if (raw == 46) {
            if (player.hasPermission("zbencoins.market.search")) {
                marketService.requestSearch(player);
            } else {
                player.sendMessage(plugin.getConfigManager().message("no-permission"));
            }
        } else if (raw == 47) {
            if (!player.hasPermission("zbencoins.market.filter")) {
                player.sendMessage(plugin.getConfigManager().message("no-permission"));
                return;
            }
            MarketQueryOptions newOptions = options.copy();
            newOptions.reset();
            reopen(player, newOptions);
        } else if (raw == 48) {
            if (!player.hasPermission("zbencoins.market.filter")) {
                player.sendMessage(plugin.getConfigManager().message("no-permission"));
                return;
            }
            MarketQueryOptions newOptions = options.copy();
            newOptions.setSortOption(nextSort(options.getSortOption()));
            newOptions.setPage(0);
            reopen(player, newOptions);
        } else if (raw == 50) {
            if (!player.hasPermission("zbencoins.market.filter")) {
                player.sendMessage(plugin.getConfigManager().message("no-permission"));
                return;
            }
            MarketQueryOptions newOptions = options.copy();
            newOptions.setCategory(nextCategory(options.getCategory()));
            newOptions.setPage(0);
            reopen(player, newOptions);
        } else if (raw == 51) {
            if (!player.hasPermission("zbencoins.market.filter")) {
                player.sendMessage(plugin.getConfigManager().message("no-permission"));
                return;
            }
            MarketQueryOptions newOptions = options.copy();
            newOptions.setOnlineOnly(!options.isOnlineOnly());
            newOptions.setPage(0);
            reopen(player, newOptions);
        } else if (raw == 49) {
            plugin.getGuiManager().openGui(player, new MarktMainGui(plugin, plugin.getCoinService(), plugin.getMarketService(),
                    player));
        }
    }

    private MarketQueryOptions.SortOption nextSort(MarketQueryOptions.SortOption current) {
        return switch (current) {
            case NEUESTE -> MarketQueryOptions.SortOption.PREIS_AUFSTEIGEND;
            case PREIS_AUFSTEIGEND -> MarketQueryOptions.SortOption.PREIS_ABSTEIGEND;
            case PREIS_ABSTEIGEND -> MarketQueryOptions.SortOption.ABLAUFEND;
            case ABLAUFEND -> MarketQueryOptions.SortOption.NEUESTE;
        };
    }

    private MarketQueryOptions.Category nextCategory(MarketQueryOptions.Category current) {
        MarketQueryOptions.Category[] values = MarketQueryOptions.Category.values();
        int idx = (current.ordinal() + 1) % values.length;
        return values[idx];
    }

    private void reopen(Player player, MarketQueryOptions newOptions) {
        MarketQueryOptions stored = marketService.getBrowseOptions(player.getUniqueId());
        stored.setSearchTerm(newOptions.getSearchTerm());
        stored.setCategory(newOptions.getCategory());
        stored.setSortOption(newOptions.getSortOption());
        stored.setOnlineOnly(newOptions.isOnlineOnly());
        stored.setPage(newOptions.getPage());
        plugin.getGuiManager().openGui(player, new MarketBrowseGui(plugin, marketService, stored.copy(), player));
    }
}
