package com.zbennoz.zbencoins.gui;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.serveroffer.ServerOffer;
import com.zbennoz.zbencoins.serveroffer.ServerOfferService;
import com.zbennoz.zbencoins.serveroffer.ServerOfferType;
import com.zbennoz.zbencoins.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Liste der Server-Angebote für Spieler und Admins.
 */
public class ServerOfferListGui implements ManagedGui {

    private final ZBenCoinsPlugin plugin;
    private final ServerOfferService service;
    private final ServerOfferType type;
    private final Inventory inventory;
    private final Map<Integer, Integer> slotMap = new HashMap<>();
    private final Player viewer;

    public ServerOfferListGui(ZBenCoinsPlugin plugin, ServerOfferService service, ServerOfferType type, Player viewer) {
        this.plugin = plugin;
        this.service = service;
        this.type = type;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, 54, Text.colorize(plugin.getConfigManager().message("server-offers-title")));
        build();
    }

    private void build() {
        if (!plugin.getConfig().getBoolean("serverOffers.enabled", true)) {
            viewer.sendMessage(plugin.getConfigManager().message("server-offer-disabled-global"));
            plugin.getGuiManager().openGui(viewer, new MarktMainGui(plugin, plugin.getCoinService(), plugin.getMarketService(), viewer));
            return;
        }
        slotMap.clear();
        boolean admin = viewer.hasPermission("zbencoins.admin.serveroffers");
        List<ServerOffer> offers = service.list(type, admin);
        int slot = 0;
        for (ServerOffer offer : offers) {
            ItemStack display = offer.getItemStack();
            display.setAmount(Math.min(display.getMaxStackSize(), offer.getMaxAmount().orElse(display.getAmount())));
            List<String> lore = new ArrayList<>();
            lore.add("&7Typ: &e" + (offer.getType() == ServerOfferType.SELL_TO_PLAYER ? "Server-Verkauf" : "Server-Ankauf"));
            lore.add("&7Preis: &6" + offer.getPricePerItem() + " " + plugin.getConfig().getString("currency-name", "Coins") + " pro Stück");
            offer.getMinAmount().ifPresent(min -> lore.add("&7Mindestmenge: &e" + min));
            offer.getMaxAmount().ifPresent(max -> lore.add("&7Maximal: &e" + max));
            lore.add("&7Links: 1 | Shift-Links: 16 | Rechts: 64");
            if (admin) {
                lore.add(offer.isEnabled() ? "&aAktiv" : "&cDeaktiviert");
                lore.add("&eRechtsklick zum Verwalten");
            }
            display = new GuiItemBuilder(display).lore(lore).glow(!offer.isEnabled()).build();
            inventory.setItem(slot, display);
            slotMap.put(slot, offer.getId());
            slot++;
            if (slot >= 45) break;
        }

        ItemStack back = new GuiItemBuilder(Material.BARRIER).name("&cZurück").build();
        ItemStack create = new GuiItemBuilder(Material.ANVIL).name("&aServer-Angebot erstellen").build();
        inventory.setItem(49, back);
        if (admin) {
            inventory.setItem(53, create);
        }
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
        if (slotMap.containsKey(raw)) {
            if (!player.hasPermission("zbencoins.serveroffers.use")) {
                player.sendMessage(plugin.getConfigManager().message("server-offer-no-permission"));
                return;
            }
            int id = slotMap.get(raw);
            Optional<ServerOffer> opt = service.getOffer(id);
            if (opt.isEmpty()) {
                player.sendMessage(plugin.getConfigManager().message("server-offer-invalid"));
                return;
            }
            ServerOffer offer = opt.get();
            if (player.hasPermission("zbencoins.admin.serveroffers")) {
                if (event.getClick() == ClickType.SHIFT_RIGHT) {
                    service.deleteOffer(id);
                    player.sendMessage(plugin.getConfigManager().message("server-offer-deleted"));
                    Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openGui(player,
                            new ServerOfferListGui(plugin, service, type, player)));
                    return;
                }
                if (event.getClick() == ClickType.RIGHT) {
                    plugin.getGuiManager().openGui(player, new ServerOfferAdminGui(plugin, service, id, player));
                    return;
                }
            }
            int amount = resolveAmount(event.getClick(), offer);
            if (type == ServerOfferType.SELL_TO_PLAYER) {
                service.handleBuy(player, offer, amount).ifPresent(player::sendMessage);
            } else {
                service.handleSell(player, offer, amount).ifPresent(player::sendMessage);
            }
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openGui(player,
                    new ServerOfferListGui(plugin, service, type, player)));
            return;
        }

        if (raw == 49) {
            plugin.getGuiManager().openGui(player, new MarktMainGui(plugin, plugin.getCoinService(), plugin.getMarketService(), player));
            return;
        }
        if (raw == 53 && player.hasPermission("zbencoins.admin.serveroffers")) {
            service.requestDraft(player, type);
            plugin.getGuiManager().openGui(player, new ServerOfferCreateGui(plugin, service, type, player));
        }
    }

    private int resolveAmount(ClickType click, ServerOffer offer) {
        List<Integer> defaults = type == ServerOfferType.SELL_TO_PLAYER
                ? plugin.getConfig().getIntegerList("serverOffers.defaultBuyAmounts")
                : plugin.getConfig().getIntegerList("serverOffers.defaultSellAmounts");
        int first = defaults.isEmpty() ? 1 : defaults.get(0);
        int shift = defaults.size() > 1 ? defaults.get(1) : first;
        int max = defaults.size() > 2 ? defaults.get(2) : shift;
        int amount = switch (click) {
            case LEFT -> first;
            case SHIFT_LEFT -> shift;
            case RIGHT, SHIFT_RIGHT -> max;
            default -> first;
        };
        if (offer.getMaxAmount().isPresent() && !viewer.hasPermission("zbencoins.serveroffers.bypasslimits")) {
            amount = Math.min(amount, offer.getMaxAmount().get());
        }
        return Math.max(1, amount);
    }
}
