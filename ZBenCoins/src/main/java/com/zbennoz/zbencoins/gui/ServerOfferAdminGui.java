package com.zbennoz.zbencoins.gui;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.serveroffer.ServerOffer;
import com.zbennoz.zbencoins.serveroffer.ServerOfferService;
import com.zbennoz.zbencoins.serveroffer.ServerOfferType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Verwaltungs-GUI für ein Server-Angebot.
 */
public class ServerOfferAdminGui implements ManagedGui {

    private final ZBenCoinsPlugin plugin;
    private final ServerOfferService service;
    private final int offerId;
    private final Inventory inventory;
    private final Player admin;

    public ServerOfferAdminGui(ZBenCoinsPlugin plugin, ServerOfferService service, int offerId, Player admin) {
        this.plugin = plugin;
        this.service = service;
        this.offerId = offerId;
        this.admin = admin;
        this.inventory = Bukkit.createInventory(this, 27, "Server-Angebot verwalten");
        build();
    }

    private void build() {
        Optional<ServerOffer> opt = service.getOffer(offerId);
        if (opt.isEmpty()) {
            return;
        }
        ServerOffer offer = opt.get();
        ItemStack info = new GuiItemBuilder(offer.getItemStack())
                .lore(List.of(
                        "&7Typ: &e" + (offer.getType() == ServerOfferType.SELL_TO_PLAYER ? "Server-Verkauf" : "Server-Ankauf"),
                        "&7Preis/Stück: &6" + offer.getPricePerItem(),
                        "&7Min: &e" + offer.getMinAmount().orElse(0),
                        "&7Max: &e" + offer.getMaxAmount().orElse(0),
                        offer.isEnabled() ? "&aAktiv" : "&cDeaktiviert"
                ))
                .build();
        ItemStack price = new GuiItemBuilder(Material.SUNFLOWER).name("&ePreis anpassen").build();
        ItemStack min = new GuiItemBuilder(Material.IRON_INGOT).name("&eMindestmenge anpassen").build();
        ItemStack max = new GuiItemBuilder(Material.GOLD_INGOT).name("&eMaximalmenge anpassen").build();
        ItemStack toggle = new GuiItemBuilder(offer.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE)
                .name(offer.isEnabled() ? "&cDeaktivieren" : "&aAktivieren")
                .build();
        ItemStack delete = new GuiItemBuilder(Material.REDSTONE_BLOCK).name("&cLöschen").build();
        ItemStack back = new GuiItemBuilder(Material.BARRIER).name("&cZurück").build();

        inventory.setItem(10, price);
        inventory.setItem(11, min);
        inventory.setItem(12, max);
        inventory.setItem(13, info);
        inventory.setItem(14, toggle);
        inventory.setItem(15, delete);
        inventory.setItem(22, back);
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
        switch (raw) {
            case 10 -> service.requestPriceInput(player, offerId);
            case 11 -> service.requestMinInput(player, offerId);
            case 12 -> service.requestMaxInput(player, offerId);
            case 14 -> {
                service.toggleOffer(offerId);
                reopen(player);
            }
            case 15 -> {
                ServerOfferType type = service.getOffer(offerId).map(ServerOffer::getType).orElse(ServerOfferType.SELL_TO_PLAYER);
                service.deleteOffer(offerId);
                player.sendMessage(plugin.getConfigManager().message("server-offer-deleted"));
                plugin.getGuiManager().openGui(player, new ServerOfferListGui(plugin, service, type, player));
            }
            case 22 -> plugin.getGuiManager().openGui(player, new ServerOfferListGui(plugin, service,
                    service.getOffer(offerId).map(ServerOffer::getType).orElse(ServerOfferType.SELL_TO_PLAYER), player));
            default -> {
            }
        }
    }

    private void reopen(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openGui(player,
                new ServerOfferAdminGui(plugin, service, offerId, admin)));
    }
}
