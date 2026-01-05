package com.zbennoz.zbencoins.gui;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.serveroffer.ServerOfferService;
import com.zbennoz.zbencoins.serveroffer.ServerOfferType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Erstellt ein neues Server-Angebot basierend auf dem Item in der Hand.
 */
public class ServerOfferCreateGui implements ManagedGui {

    private final ZBenCoinsPlugin plugin;
    private final ServerOfferService service;
    private final ServerOfferType type;
    private final Player player;
    private final Inventory inventory;

    public ServerOfferCreateGui(ZBenCoinsPlugin plugin, ServerOfferService service, ServerOfferType type, Player player) {
        this.plugin = plugin;
        this.service = service;
        this.type = type;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 27, "Server-Angebot erstellen");
        service.requestDraft(player, type);
        build();
    }

    private void build() {
        ItemStack placeholder = new GuiItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, placeholder);
        }
        var draftOpt = service.getDraft(player);
        if (draftOpt.isEmpty()) {
            inventory.setItem(13, new GuiItemBuilder(Material.BARRIER).name("&cKein Item in der Hand").build());
            return;
        }
        var draft = draftOpt.get();
        ItemStack info = new GuiItemBuilder(draft.getItemStack())
                .lore(List.of(
                        "&7Typ: &e" + (type == ServerOfferType.SELL_TO_PLAYER ? "Server-Verkauf" : "Server-Ankauf"),
                        "&7Preis: &6" + draft.getPricePerItem(),
                        "&7Min: &e" + draft.getMinAmount().orElse(0),
                        "&7Max: &e" + draft.getMaxAmount().orElse(0),
                        draft.isEnabled() ? "&aAktiv" : "&cDeaktiviert"
                ))
                .build();
        ItemStack setPrice = new GuiItemBuilder(Material.SUNFLOWER).name("&ePreis setzen").build();
        ItemStack setMin = new GuiItemBuilder(Material.IRON_INGOT).name("&eMindestmenge setzen").build();
        ItemStack setMax = new GuiItemBuilder(Material.GOLD_INGOT).name("&eMaximalmenge setzen").build();
        ItemStack toggle = new GuiItemBuilder(draft.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE)
                .name(draft.isEnabled() ? "&aAktiviert" : "&cDeaktiviert")
                .build();
        ItemStack confirm = new GuiItemBuilder(Material.EMERALD_BLOCK).name("&aErstellen").build();
        ItemStack cancel = new GuiItemBuilder(Material.BARRIER).name("&cAbbrechen").build();

        inventory.setItem(10, setPrice);
        inventory.setItem(12, setMin);
        inventory.setItem(14, setMax);
        inventory.setItem(16, toggle);
        inventory.setItem(13, info);
        inventory.setItem(22, cancel);
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
        int raw = event.getRawSlot();
        switch (raw) {
            case 10 -> service.requestPriceInput(clicker, -1);
            case 12 -> service.requestMinInput(clicker, -1);
            case 14 -> service.requestMaxInput(clicker, -1);
            case 16 -> service.getDraft(clicker).ifPresent(draft -> {
                draft.setEnabled(!draft.isEnabled());
                plugin.getGuiManager().openGui(clicker, new ServerOfferCreateGui(plugin, service, type, clicker));
            });
            case 26 -> {
                service.publishDraft(clicker);
                plugin.getGuiManager().openGui(clicker, new ServerOfferListGui(plugin, service, type, clicker));
            }
            case 22 -> {
                service.clearDraft(clicker);
                plugin.getGuiManager().openGui(clicker, new ServerOfferListGui(plugin, service, type, clicker));
            }
            default -> {}
        }
    }
}
