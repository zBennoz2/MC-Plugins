package com.zbennoz.zbencoins.gui;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.service.CoinService;
import com.zbennoz.zbencoins.service.MarketService;
import com.zbennoz.zbencoins.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Hauptmenü des Marktes.
 */
public class MarktMainGui implements ManagedGui {

    private final Inventory inventory;
    private final CoinService coinService;
    private final ZBenCoinsPlugin plugin;
    private final MarketService marketService;

    public MarktMainGui(ZBenCoinsPlugin plugin, CoinService coinService, MarketService marketService, Player player) {
        this.plugin = plugin;
        this.coinService = coinService;
        this.marketService = marketService;
        FileConfiguration config = plugin.getConfigManager().getConfig();
        String title = Text.colorize(config.getString("gui.title", "ZBenMarkt"));
        this.inventory = Bukkit.createInventory(this, 54, title);
        build(player);
    }

    private void build(Player player) {
        long balance = coinService.getBalance(player.getUniqueId());
        String currency = plugin.getConfig().getString("currency-name", "Coins");

        ItemStack coinsInfo = new GuiItemBuilder(Material.GOLD_INGOT)
                .name("&eDeine Coins")
                .lore(List.of("&7Kontostand:", "&e" + balance + " " + currency))
                .build();

        ItemStack markt = new GuiItemBuilder(Material.CHEST)
                .name("&6Spieler-Markt")
                .lore(List.of("&7Durchstöbere Spielerangebote"))
                .build();

        ItemStack serverShop = new GuiItemBuilder(Material.DIAMOND)
                .name("&bServer-Shop")
                .lore(List.of("&7Kaufe Items vom Server"))
                .build();

        ItemStack serverBuy = new GuiItemBuilder(Material.OAK_LOG)
                .name("&6Server-Ankauf")
                .lore(List.of("&7Verkaufe Items an den Server"))
                .build();

        ItemStack myOffers = new GuiItemBuilder(Material.WRITABLE_BOOK)
                .name("&eMeine Angebote")
                .lore(List.of("&7Verwalte deine Einträge"))
                .build();

        ItemStack create = new GuiItemBuilder(Material.ANVIL)
                .name("&aAngebot einstellen")
                .lore(List.of("&7Stelle dein Item zum Verkauf ein"))
                .build();

        ItemStack jobs = new GuiItemBuilder(Material.EMERALD)
                .name("&aJobs")
                .lore(List.of("&7Verdiene Coins", "&eNimm Aufträge an"))
                .build();

        ItemStack info = new GuiItemBuilder(Material.BOOK)
                .name("&eInfo")
                .lore(List.of("&7Statistiken & Hinweise"))
                .build();

        ItemStack close = new GuiItemBuilder(Material.BARRIER)
                .name("&cSchließen")
                .build();

        inventory.setItem(10, coinsInfo);
        inventory.setItem(20, markt);
        inventory.setItem(22, serverShop);
        inventory.setItem(24, serverBuy);
        inventory.setItem(29, create);
        inventory.setItem(30, myOffers);
        inventory.setItem(32, jobs);
        inventory.setItem(40, info);
        inventory.setItem(49, close);
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
        int slot = event.getRawSlot();
        switch (slot) {
            case 20 -> {
                var opts = marketService.getBrowseOptions(player.getUniqueId()).copy();
                opts.setPage(0);
                plugin.getGuiManager().openGui(player, new MarketBrowseGui(plugin, marketService, opts, player));
            }
            case 24 -> plugin.getGuiManager().openGui(player, new ServerOfferListGui(plugin, plugin.getServerOfferService(), com.zbennoz.zbencoins.serveroffer.ServerOfferType.BUY_FROM_PLAYER, player));
            case 22 -> plugin.getGuiManager().openGui(player, new ServerOfferListGui(plugin, plugin.getServerOfferService(), com.zbennoz.zbencoins.serveroffer.ServerOfferType.SELL_TO_PLAYER, player));
            case 29 -> plugin.getGuiManager().openGui(player, new OfferCreateGui(plugin, marketService, player));
            case 30 -> plugin.getGuiManager().openGui(player, new MyOffersGui(plugin, marketService, player));
            case 40 -> plugin.getGuiManager().openGui(player, new MarktInfoGui(plugin, coinService, player));
            case 32 -> plugin.getGuiManager().openGui(player, new JobsMainGui(plugin, plugin.getJobService()));
            case 49 -> player.closeInventory();
            default -> {
            }
        }
    }
}
