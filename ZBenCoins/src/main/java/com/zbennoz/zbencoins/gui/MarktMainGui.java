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
        this.inventory = Bukkit.createInventory(this, 27, title);
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
                .name("&6Marktplatz")
                .lore(List.of("&7Durchstöbere Angebote"))
                .build();

        ItemStack jobs = new GuiItemBuilder(Material.EMERALD)
                .name("&aJobs")
                .lore(List.of("&7Verdiene Coins", "&eNimm Aufträge an"))
                .build();

        ItemStack anbieten = new GuiItemBuilder(Material.ANVIL)
                .name("&bAnbieten")
                .lore(List.of("&7Stelle eigene Angebote ein"))
                .build();

        ItemStack info = new GuiItemBuilder(Material.BOOK)
                .name("&eInfo")
                .lore(List.of("&7Statistiken & Hinweise"))
                .build();

        ItemStack close = new GuiItemBuilder(Material.BARRIER)
                .name("&cSchließen")
                .build();

        inventory.setItem(10, coinsInfo);
        inventory.setItem(11, markt);
        inventory.setItem(12, jobs);
        inventory.setItem(14, anbieten);
        inventory.setItem(15, info);
        inventory.setItem(22, close);
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
            case 11 -> plugin.getGuiManager().openGui(player, new MarketBrowseGui(plugin, marketService, 0));
            case 14 -> plugin.getGuiManager().openGui(player, new OfferCreateGui(plugin, marketService, player));
            case 15 -> plugin.getGuiManager().openGui(player, new MarktInfoGui(plugin, coinService, player));
            case 12 -> plugin.getGuiManager().openGui(player, new JobsMainGui(plugin, plugin.getJobService()));
            case 22 -> player.closeInventory();
            default -> {
            }
        }
    }
}
