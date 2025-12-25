package com.zbennoz.zbencoins.gui;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.market.OfferRecord;
import com.zbennoz.zbencoins.service.MarketService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Bestätigungsdialog für Käufe.
 */
public class PurchaseConfirmGui implements ManagedGui {

    private final ZBenCoinsPlugin plugin;
    private final MarketService marketService;
    private final OfferRecord offerRecord;
    private final Inventory inventory;

    public PurchaseConfirmGui(ZBenCoinsPlugin plugin, MarketService marketService, OfferRecord offerRecord) {
        this.plugin = plugin;
        this.marketService = marketService;
        this.offerRecord = offerRecord;
        this.inventory = Bukkit.createInventory(this, 27, "Bestätigen?");
        build();
    }

    private void build() {
        ItemStack display = new GuiItemBuilder(offerRecord.getItem())
                .lore(List.of(
                        "&7Menge: &e" + offerRecord.getAmount(),
                        "&7Preis: &6" + offerRecord.getPrice() + " " + plugin.getConfig().getString("currency-name", "Coins"),
                        "&7Verkäufer: &e" + offerRecord.getSellerName()
                )).build();
        ItemStack accept = new GuiItemBuilder(Material.LIME_WOOL).name("&aKaufen").build();
        ItemStack deny = new GuiItemBuilder(Material.RED_WOOL).name("&cAbbrechen").build();
        inventory.setItem(11, display);
        inventory.setItem(15, accept);
        inventory.setItem(13, deny);
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
        if (slot == 15) {
            marketService.purchase(player, offerRecord).ifPresentOrElse(
                    player::sendMessage,
                    () -> {
                        player.sendMessage(plugin.getConfigManager().message("buy-success"));
                        player.closeInventory();
                    });
        } else if (slot == 13) {
            com.zbennoz.zbencoins.market.MarketQueryOptions opts = marketService.getBrowseOptions(player.getUniqueId()).copy();
            plugin.getGuiManager().openGui(player, new MarketBrowseGui(plugin, marketService, opts, player));
        }
    }
}
