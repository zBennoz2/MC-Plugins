package com.zbennoz.zbencoins.serveroffer;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.database.PlayerDao;
import com.zbennoz.zbencoins.database.TransactionDao;
import com.zbennoz.zbencoins.util.InventoryUtil;
import com.zbennoz.zbencoins.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;

/**
 * Verwaltung der Server-Angebote samt Transaktionen.
 */
public class ServerOfferService {

    private final ZBenCoinsPlugin plugin;
    private final ServerOfferDao dao;
    private final PlayerDao playerDao;
    private final TransactionDao transactionDao;
    private final Connection connection;

    private final Map<UUID, OfferDraft> drafts = new HashMap<>();
    private final Map<UUID, PendingInput> awaitingInput = new HashMap<>();

    public ServerOfferService(ZBenCoinsPlugin plugin, ServerOfferDao dao, PlayerDao playerDao,
                              TransactionDao transactionDao, Connection connection) {
        this.plugin = plugin;
        this.dao = dao;
        this.playerDao = playerDao;
        this.transactionDao = transactionDao;
        this.connection = connection;
    }

    public List<ServerOffer> list(ServerOfferType type, boolean includeDisabled) {
        try {
            return dao.findAll().stream()
                    .filter(offer -> offer.getType() == type)
                    .filter(offer -> includeDisabled || offer.isEnabled())
                    .toList();
        } catch (SQLException | IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Konnte Server-Angebote nicht laden", e);
            return List.of();
        }
    }

    public Optional<ServerOffer> getOffer(int id) {
        try {
            return dao.findById(id);
        } catch (SQLException | IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Konnte Server-Angebot nicht laden", e);
            return Optional.empty();
        }
    }

    public void requestPriceInput(Player player, int offerId) {
        awaitingInput.put(player.getUniqueId(), new PendingInput(InputType.PRICE, offerId));
        player.sendMessage(plugin.getConfigManager().message("enter-server-price"));
        player.closeInventory();
    }

    public void requestMinInput(Player player, int offerId) {
        awaitingInput.put(player.getUniqueId(), new PendingInput(InputType.MIN, offerId));
        player.sendMessage(plugin.getConfigManager().message("enter-min"));
        player.closeInventory();
    }

    public void requestMaxInput(Player player, int offerId) {
        awaitingInput.put(player.getUniqueId(), new PendingInput(InputType.MAX, offerId));
        player.sendMessage(plugin.getConfigManager().message("enter-max"));
        player.closeInventory();
    }

    public boolean handleChat(Player player, String message) {
        PendingInput input = awaitingInput.remove(player.getUniqueId());
        if (input == null) {
            return false;
        }
        try {
            int value = Integer.parseInt(message.trim());
            if (value < 0) {
                player.sendMessage(plugin.getConfigManager().message("server-offer-invalid"));
                return true;
            }
            if (input.offerId() == -1) {
                updateDraftValue(player, value, input.type());
            } else {
                switch (input.type()) {
                    case PRICE -> updateOfferValue(player, input.offerId(), value, InputType.PRICE);
                    case MIN -> updateOfferValue(player, input.offerId(), value == 0 ? null : value, InputType.MIN);
                    case MAX -> updateOfferValue(player, input.offerId(), value == 0 ? null : value, InputType.MAX);
                }
            }
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getConfigManager().message("server-offer-invalid"));
        }
        return true;
    }

    private void updateOfferValue(Player player, int offerId, Integer value, InputType type) {
        getOffer(offerId).ifPresent(offer -> {
            long price = offer.getPricePerItem();
            Integer min = offer.getMinAmount().orElse(null);
            Integer max = offer.getMaxAmount().orElse(null);
            boolean enabled = offer.isEnabled();

            switch (type) {
                case PRICE -> price = value == null ? 0 : value;
                case MIN -> min = value;
                case MAX -> max = value;
            }
            saveOffer(new ServerOffer(offer.getId(), offer.getType(), offer.getItemStack(), price, enabled, min, max,
                    offer.getCreatedBy().orElse(null), offer.getCreatedAt().orElse(null)));
            player.sendMessage(plugin.getConfigManager().message("server-offer-updated"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openGui(player,
                    new com.zbennoz.zbencoins.gui.ServerOfferAdminGui(plugin, this, offer.getId(), player)));
        });
    }

    private void updateDraftValue(Player player, Integer value, InputType type) {
        getDraft(player).ifPresent(draft -> {
            switch (type) {
                case PRICE -> draft.setPricePerItem(value);
                case MIN -> draft.setMinAmount(value == 0 ? null : value);
                case MAX -> draft.setMaxAmount(value == 0 ? null : value);
            }
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openGui(player,
                    new com.zbennoz.zbencoins.gui.ServerOfferCreateGui(plugin, this, draft.getType(), player)));
        });
    }

    public void saveOffer(ServerOffer offer) {
        try {
            dao.update(offer);
        } catch (SQLException | IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Konnte Server-Angebot nicht speichern", e);
        }
    }

    public void deleteOffer(int id) {
        try {
            dao.delete(id);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Konnte Server-Angebot nicht löschen", e);
        }
    }

    public void toggleOffer(int id) {
        getOffer(id).ifPresent(offer -> saveOffer(new ServerOffer(offer.getId(), offer.getType(), offer.getItemStack(),
                offer.getPricePerItem(), !offer.isEnabled(), offer.getMinAmount().orElse(null),
                offer.getMaxAmount().orElse(null), offer.getCreatedBy().orElse(null), offer.getCreatedAt().orElse(null))));
    }

    public void requestDraft(Player player, ServerOfferType type) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            player.sendMessage(plugin.getConfigManager().message("server-offer-select-item"));
            return;
        }
        drafts.put(player.getUniqueId(), new OfferDraft(type, hand.clone(), 0, true, null, null));
    }

    public Optional<OfferDraft> getDraft(Player player) {
        return Optional.ofNullable(drafts.get(player.getUniqueId()));
    }

    public void clearDraft(Player player) {
        drafts.remove(player.getUniqueId());
    }

    public void setDraftPrice(Player player, long price) {
        getDraft(player).ifPresent(draft -> draft.setPricePerItem(price));
    }

    public void setDraftMin(Player player, Integer min) {
        getDraft(player).ifPresent(draft -> draft.setMinAmount(min));
    }

    public void setDraftMax(Player player, Integer max) {
        getDraft(player).ifPresent(draft -> draft.setMaxAmount(max));
    }

    public Optional<ServerOffer> publishDraft(Player player) {
        Optional<OfferDraft> optional = getDraft(player);
        if (optional.isEmpty()) return Optional.empty();
        OfferDraft draft = optional.get();
        if (draft.getPricePerItem() <= 0 || draft.getItemStack().getType() == Material.AIR) {
            player.sendMessage(plugin.getConfigManager().message("server-offer-invalid"));
            return Optional.empty();
        }

        try {
            ServerOffer offer = dao.insert(draft.getType(), draft.getItemStack(), draft.getPricePerItem(), draft.isEnabled(),
                    draft.getMinAmount().orElse(null), draft.getMaxAmount().orElse(null), player.getName());
            player.sendMessage(plugin.getConfigManager().message("server-offer-created"));
            clearDraft(player);
            return Optional.of(offer);
        } catch (SQLException | IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Konnte Server-Angebot nicht erstellen", e);
            return Optional.empty();
        }
    }

    public Optional<String> handleBuy(Player player, ServerOffer offer, int amount) {
        if (!offer.isEnabled()) {
            return Optional.of(plugin.getConfigManager().message("server-offer-disabled"));
        }
        if (offer.getType() != ServerOfferType.SELL_TO_PLAYER) {
            return Optional.of(plugin.getConfigManager().message("server-offer-invalid"));
        }
        return processLimits(offer, amount).or(() -> executePurchase(player, offer, amount));
    }

    public Optional<String> handleSell(Player player, ServerOffer offer, int amount) {
        if (!offer.isEnabled()) {
            return Optional.of(plugin.getConfigManager().message("server-offer-disabled"));
        }
        if (offer.getType() != ServerOfferType.BUY_FROM_PLAYER) {
            return Optional.of(plugin.getConfigManager().message("server-offer-invalid"));
        }
        return processLimits(offer, amount).or(() -> executeSell(player, offer, amount));
    }

    private Optional<String> processLimits(ServerOffer offer, int amount) {
        if (offer.getMinAmount().isPresent() && amount < offer.getMinAmount().get()) {
            return Optional.of(plugin.getConfigManager().message("server-offer-limits", Map.of(
                    "min", String.valueOf(offer.getMinAmount().get()),
                    "max", String.valueOf(offer.getMaxAmount().orElse(amount))
            )));
        }
        if (offer.getMaxAmount().isPresent() && amount > offer.getMaxAmount().get()) {
            return Optional.of(plugin.getConfigManager().message("server-offer-limits", Map.of(
                    "min", String.valueOf(offer.getMinAmount().orElse(1)),
                    "max", String.valueOf(offer.getMaxAmount().get())
            )));
        }
        return Optional.empty();
    }

    private Optional<String> executePurchase(Player player, ServerOffer offer, int amount) {
        long total = offer.getPricePerItem() * amount;
        long balance = plugin.getCoinService().getBalance(player.getUniqueId());
        if (balance < total) {
            return Optional.of(plugin.getConfigManager().message("server-missing-coins", Map.of("coins", String.valueOf(total))));
        }

        ItemStack toGive = offer.getItemStack().clone();
        toGive.setAmount(amount);
        boolean dropOnFull = plugin.getConfig().getBoolean("serverOffers.dropItemIfInventoryFull", true);
        if (!dropOnFull && !InventoryUtil.canFit(player, toGive)) {
            return Optional.of(plugin.getConfigManager().message("not-enough-space"));
        }

        try {
            connection.setAutoCommit(false);
            playerDao.addCoins(player.getUniqueId(), -total);
            transactionDao.insert(player.getUniqueId(), "SERVER_SHOP_BUY", -total, "Kauf Server-Angebot #" + offer.getId());
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Rollback fehlgeschlagen", ex);
            }
            plugin.getLogger().log(Level.SEVERE, "Konnte Kauf nicht durchführen", e);
            return Optional.of(plugin.getConfigManager().message("error"));
        }

        InventoryUtil.giveItem(player, toGive);
        player.sendMessage(plugin.getConfigManager().message("server-bought", Map.of(
                "amount", String.valueOf(amount),
                "item", Text.strip(toGive.getItemMeta() != null && toGive.getItemMeta().hasDisplayName()
                        ? toGive.getItemMeta().getDisplayName() : toGive.getType().name()),
                "coins", String.valueOf(total)
        )));
        return Optional.empty();
    }

    private Optional<String> executeSell(Player player, ServerOffer offer, int amount) {
        ItemStack template = offer.getItemStack().clone();
        template.setAmount(1);
        if (!InventoryUtil.hasEnough(player, template, amount)) {
            String itemName = template.getType().name();
            if (template.getItemMeta() != null && template.getItemMeta().hasDisplayName()) {
                itemName = Text.strip(template.getItemMeta().getDisplayName());
            }
            return Optional.of(plugin.getConfigManager().message("server-missing-item", Map.of("item", itemName)));
        }

        long total = offer.getPricePerItem() * amount;
        if (!InventoryUtil.remove(player, template, amount)) {
            return Optional.of(plugin.getConfigManager().message("server-missing-item", Map.of("item", template.getType().name())));
        }

        try {
            connection.setAutoCommit(false);
            playerDao.addCoins(player.getUniqueId(), total);
            transactionDao.insert(player.getUniqueId(), "SERVER_SHOP_SELL", total, "Verkauf an Server-Angebot #" + offer.getId());
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Rollback fehlgeschlagen", ex);
            }
            plugin.getLogger().log(Level.SEVERE, "Konnte Verkauf nicht durchführen", e);
            return Optional.of(plugin.getConfigManager().message("error"));
        }

        player.sendMessage(plugin.getConfigManager().message("server-sold", Map.of(
                "amount", String.valueOf(amount),
                "item", template.getType().name(),
                "coins", String.valueOf(total)
        )));
        return Optional.empty();
    }

    private record PendingInput(InputType type, int offerId) {}

    private enum InputType { PRICE, MIN, MAX }

    public static class OfferDraft {
        private final ServerOfferType type;
        private final ItemStack itemStack;
        private long pricePerItem;
        private boolean enabled;
        private Integer minAmount;
        private Integer maxAmount;

        public OfferDraft(ServerOfferType type, ItemStack itemStack, long pricePerItem, boolean enabled,
                          Integer minAmount, Integer maxAmount) {
            this.type = type;
            this.itemStack = itemStack;
            this.pricePerItem = pricePerItem;
            this.enabled = enabled;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
        }

        public ServerOfferType getType() { return type; }

        public ItemStack getItemStack() { return itemStack; }

        public long getPricePerItem() { return pricePerItem; }

        public boolean isEnabled() { return enabled; }

        public Optional<Integer> getMinAmount() { return Optional.ofNullable(minAmount); }

        public Optional<Integer> getMaxAmount() { return Optional.ofNullable(maxAmount); }

        public void setPricePerItem(long pricePerItem) { this.pricePerItem = pricePerItem; }

        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public void setMinAmount(Integer minAmount) { this.minAmount = minAmount; }

        public void setMaxAmount(Integer maxAmount) { this.maxAmount = maxAmount; }
    }
}
