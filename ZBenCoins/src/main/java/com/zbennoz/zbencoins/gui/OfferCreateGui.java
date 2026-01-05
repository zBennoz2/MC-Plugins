package com.zbennoz.zbencoins.gui;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.service.MarketService;
import com.zbennoz.zbencoins.service.MarketService.OfferDraft;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Erstellung eines neuen Angebotes.
 */
public class OfferCreateGui implements ManagedGui {

    private final ZBenCoinsPlugin plugin;
    private final MarketService marketService;
    private final Player player;
    private final Inventory inventory;

    public OfferCreateGui(ZBenCoinsPlugin plugin, MarketService marketService, Player player) {
        this.plugin = plugin;
        this.marketService = marketService;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 27, "Angebot erstellen");
        initDraft();
        build();
    }

    private void initDraft() {
        if (marketService.getDraft(player).isPresent()) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            player.sendMessage(plugin.getConfigManager().message("no-item-in-hand"));
            return;
        }
        marketService.createDraft(player, hand);
    }

    private void build() {
        Optional<OfferDraft> optionalDraft = marketService.getDraft(player);
        if (optionalDraft.isEmpty()) {
            inventory.setItem(13, new GuiItemBuilder(Material.BARRIER).name("&cKein Item").build());
            return;
        }
        OfferDraft draft = optionalDraft.get();
        ItemStack itemDisplay = new GuiItemBuilder(draft.getItem())
                .lore(List.of(
                        "&7Menge: &e" + draft.getAmount(),
                        "&7Preis: &6" + draft.getPrice(),
                        "&7Läuft ab in Stunden: &e" + plugin.getConfig().getInt("market.default-duration-hours", 24)
                ))
                .build();
        ItemStack minus = new GuiItemBuilder(Material.RED_WOOL).name("&c-1").build();
        ItemStack plus = new GuiItemBuilder(Material.LIME_WOOL).name("&a+1").build();
        ItemStack price = new GuiItemBuilder(Material.SUNFLOWER).name("&ePreis setzen").build();
        ItemStack confirm = new GuiItemBuilder(Material.EMERALD_BLOCK).name("&aAngebot erstellen").build();
        ItemStack cancel = new GuiItemBuilder(Material.BARRIER).name("&cAbbrechen").build();

        inventory.setItem(11, minus);
        inventory.setItem(13, itemDisplay);
        inventory.setItem(15, plus);
        inventory.setItem(22, cancel);
        inventory.setItem(16, price);
        inventory.setItem(26, confirm);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player clicker)) {
            return;
        }
        Optional<OfferDraft> optionalDraft = marketService.getDraft(clicker);
        if (optionalDraft.isEmpty()) {
            clicker.closeInventory();
            return;
        }
        OfferDraft draft = optionalDraft.get();
        int raw = event.getRawSlot();
        if (raw == 11) {
            int newAmount = Math.max(1, draft.getAmount() - 1);
            draft.setAmount(newAmount);
            plugin.getGuiManager().openGui(clicker, new OfferCreateGui(plugin, marketService, clicker));
        } else if (raw == 15) {
            int newAmount = Math.min(draft.getItem().getAmount(), draft.getAmount() + 1);
            draft.setAmount(newAmount);
            plugin.getGuiManager().openGui(clicker, new OfferCreateGui(plugin, marketService, clicker));
        } else if (raw == 16) {
            marketService.requestPriceInput(clicker);
        } else if (raw == 26) {
            int limit = marketService.resolveMaxOffers(clicker);
            if (!clicker.hasPermission("zbencoins.market.create")) {
                clicker.sendMessage(plugin.getConfigManager().message("market-no-sell"));
                return;
            }
            if (marketService.countActive(clicker.getUniqueId()) >= limit) {
                clicker.sendMessage(plugin.getConfigManager().message("market-limit"));
                return;
            }
            marketService.publishOffer(clicker);
            plugin.getGuiManager().openGui(clicker, new MarktMainGui(plugin, plugin.getCoinService(), plugin.getMarketService(), clicker));
        } else if (raw == 22) {
            marketService.clearDraft(clicker);
            plugin.getGuiManager().openGui(clicker, new MarktMainGui(plugin, plugin.getCoinService(), plugin.getMarketService(), clicker));
        }
    }
}
