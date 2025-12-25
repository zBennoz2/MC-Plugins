package com.zbennoz.zbencoins.service;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.database.PlayerDao;
import com.zbennoz.zbencoins.database.TransactionDao;
import com.zbennoz.zbencoins.market.MarketLogDao;
import com.zbennoz.zbencoins.market.MarketQueryOptions;
import com.zbennoz.zbencoins.market.OfferDao;
import com.zbennoz.zbencoins.market.OfferRecord;
import com.zbennoz.zbencoins.market.OfferStatus;
import com.zbennoz.zbencoins.util.InventoryUtil;
import com.zbennoz.zbencoins.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Kernlogik für den Marktplatz inklusive Escrow.
 */
public class MarketService {

    private final ZBenCoinsPlugin plugin;
    private final OfferDao offerDao;
    private final MarketLogDao logDao;
    private final PlayerDao playerDao;
    private final TransactionDao transactionDao;
    private final Connection connection;

    private final Map<UUID, OfferDraft> drafts = new ConcurrentHashMap<>();
    private final Map<UUID, MarketInput> awaitingInput = new ConcurrentHashMap<>();
    private final Map<UUID, MarketQueryOptions> browseOptions = new ConcurrentHashMap<>();

    private enum MarketInput {
        PRICE,
        SEARCH
    }

    public MarketService(ZBenCoinsPlugin plugin,
                         OfferDao offerDao,
                         MarketLogDao logDao,
                         PlayerDao playerDao,
                         TransactionDao transactionDao,
                         Connection connection) {
        this.plugin = plugin;
        this.offerDao = offerDao;
        this.logDao = logDao;
        this.playerDao = playerDao;
        this.transactionDao = transactionDao;
        this.connection = connection;
        startExpiryTask();
    }

    public OfferDraft createDraft(Player player, ItemStack baseItem) {
        OfferDraft draft = new OfferDraft(
                baseItem.clone(),
                baseItem.getAmount(),
                0L,
                Instant.now().plus(plugin.getConfig().getInt("market.default-duration-hours", 24), ChronoUnit.HOURS)
        );
        drafts.put(player.getUniqueId(), draft);
        return draft;
    }

    public Optional<OfferDraft> getDraft(Player player) {
        return Optional.ofNullable(drafts.get(player.getUniqueId()));
    }

    public Optional<OfferRecord> getOffer(int id) {
        try {
            return offerDao.findById(id);
        } catch (SQLException | IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Konnte Angebot nicht laden", e);
            return Optional.empty();
        }
    }

    public void clearDraft(Player player) {
        drafts.remove(player.getUniqueId());
        awaitingInput.remove(player.getUniqueId());
    }

    public MarketQueryOptions getBrowseOptions(UUID playerId) {
        return browseOptions.computeIfAbsent(playerId, id -> new MarketQueryOptions());
    }

    public List<OfferRecord> listFiltered(MarketQueryOptions options) {
        try {
            List<OfferRecord> offers = offerDao.findAllActive();
            String term = options.getSearchTerm().toLowerCase(Locale.ROOT);

            return offers.stream()
                    .filter(offer -> term.isBlank() || matchesSearch(offer, term))
                    .filter(offer -> !options.isOnlineOnly() || Bukkit.getPlayer(offer.getSellerUuid()) != null)
                    .filter(offer -> options.getCategory().matches(offer.getItem()))
                    .sorted(resolveComparator(options))
                    .toList();

        } catch (SQLException | IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Konnte Angebote nicht filtern", e);
            return List.of();
        }
    }

    private Comparator<OfferRecord> resolveComparator(MarketQueryOptions options) {
        return switch (options.getSortOption()) {
            case PREIS_AUFSTEIGEND -> Comparator.comparingLong(OfferRecord::getPrice);
            case PREIS_ABSTEIGEND -> Comparator.comparingLong(OfferRecord::getPrice).reversed();
            case ABLAUFEND -> Comparator.comparing(OfferRecord::getExpiresAt);
            case NEUESTE -> Comparator.comparing(OfferRecord::getCreatedAt).reversed();
        };
    }

    private boolean matchesSearch(OfferRecord offer, String term) {
        String itemName = offer.getItem().getType().name().toLowerCase(Locale.ROOT);
        String display = offer.getItem().getItemMeta() != null && offer.getItem().getItemMeta().hasDisplayName()
                ? Text.strip(offer.getItem().getItemMeta().getDisplayName()).toLowerCase(Locale.ROOT)
                : "";
        String seller = offer.getSellerName().toLowerCase(Locale.ROOT);
        return itemName.contains(term) || display.contains(term) || seller.contains(term);
    }

    public void setAmount(Player player, int amount) {
        getDraft(player).ifPresent(d -> d.setAmount(amount));
    }

    public void requestPriceInput(Player player) {
        awaitingInput.put(player.getUniqueId(), MarketInput.PRICE);
        player.sendMessage(plugin.getConfigManager().message("enter-price"));
        player.closeInventory();
    }

    public boolean handleChat(Player player, String message) {
        MarketInput input = awaitingInput.remove(player.getUniqueId());
        if (input == null) return false;

        if (input == MarketInput.PRICE) {
            try {
                long price = Long.parseLong(message.trim());
                if (price <= 0) {
                    player.sendMessage(plugin.getConfigManager().message("invalid-amount"));
                    return true;
                }
                getDraft(player).ifPresent(draft -> draft.setPrice(price));
                Bukkit.getScheduler().runTask(plugin, () ->
                        plugin.getGuiManager().openGui(player,
                                new com.zbennoz.zbencoins.gui.OfferCreateGui(plugin, this, player)));
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getConfigManager().message("invalid-amount"));
            }
            return true;
        }

        MarketQueryOptions options = getBrowseOptions(player.getUniqueId());
        options.setSearchTerm(message);
        options.setPage(0);

        player.sendMessage(Text.colorize("&aSuche aktualisiert."));
        Bukkit.getScheduler().runTask(plugin, () ->
                plugin.getGuiManager().openGui(player,
                        new com.zbennoz.zbencoins.gui.MarketBrowseGui(plugin, this, options.copy(), player)));
        return true;
    }

    public void requestSearch(Player player) {
        awaitingInput.put(player.getUniqueId(), MarketInput.SEARCH);
        player.sendMessage(Text.colorize("&eGib den Suchbegriff im Chat ein (Item oder Verkäufer)."));
        player.closeInventory();
    }

    public Optional<OfferRecord> publishOffer(Player player) {
        Optional<OfferDraft> optionalDraft = getDraft(player);
        if (optionalDraft.isEmpty()) return Optional.empty();

        OfferDraft draft = optionalDraft.get();
        if (draft.getPrice() <= 0 || draft.getAmount() <= 0) {
            player.sendMessage(plugin.getConfigManager().message("offer-missing-data"));
            return Optional.empty();
        }

        ItemStack template = draft.getItem().clone();
        template.setAmount(1);

        if (!InventoryUtil.hasEnough(player, template, draft.getAmount())) {
            player.sendMessage(plugin.getConfigManager().message("not-enough-items"));
            return Optional.empty();
        }

        if (!InventoryUtil.remove(player, template, draft.getAmount())) {
            player.sendMessage(plugin.getConfigManager().message("not-enough-items"));
            return Optional.empty();
        }

        ItemStack escrowItem = draft.getItem().clone();
        escrowItem.setAmount(draft.getAmount());

        try {
            OfferRecord record = offerDao.insert(
                    player.getUniqueId(),
                    player.getName(),
                    escrowItem,
                    draft.getAmount(),
                    draft.getPrice(),
                    draft.getExpiresAt()
            );
            logDao.log(record.getId(), "CREATE", player.getUniqueId(), player.getName(), "Angebot erstellt");
            player.sendMessage(plugin.getConfigManager().message("offer-created"));
            clearDraft(player);
            return Optional.of(record);

        } catch (SQLException | IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Konnte Angebot nicht speichern", e);
            player.sendMessage(plugin.getConfigManager().message("error"));
            return Optional.empty();
        }
    }

    public Optional<String> purchase(Player buyer, OfferRecord record) {
        if (record.getSellerUuid().equals(buyer.getUniqueId())) {
            return Optional.of(plugin.getConfigManager().message("buy-own"));
        }

        long balance = plugin.getCoinService().getBalance(buyer.getUniqueId());
        if (balance < record.getPrice()) {
            return Optional.of(plugin.getConfigManager().message("not-enough-coins"));
        }

        try {
            connection.setAutoCommit(false);

            if (!offerDao.reserveForPurchase(record.getId(), buyer.getUniqueId())) {
                connection.rollback();
                connection.setAutoCommit(true);
                return Optional.of(plugin.getConfigManager().message("offer-not-available"));
            }

            playerDao.addCoins(buyer.getUniqueId(), -record.getPrice());
            playerDao.addCoins(record.getSellerUuid(), record.getPrice());

            transactionDao.insert(buyer.getUniqueId(), "MARKET_BUY", -record.getPrice(), "Kauf Angebot #" + record.getId());
            transactionDao.insert(record.getSellerUuid(), "MARKET_SELL", record.getPrice(), "Verkauf an " + buyer.getName());

            connection.commit();
            connection.setAutoCommit(true);

            InventoryUtil.giveItem(buyer, record.getItem());
            logDao.log(record.getId(), "BUY", buyer.getUniqueId(), buyer.getName(), "gekauft");
            notifySeller(record);
            return Optional.empty();

        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Rollback fehlgeschlagen", ex);
            }
            plugin.getLogger().log(Level.SEVERE, "Kauf fehlgeschlagen", e);
            return Optional.of(plugin.getConfigManager().message("error"));
        }
    }

    private void notifySeller(OfferRecord record) {
        Player onlineSeller = Bukkit.getPlayer(record.getSellerUuid());
        if (onlineSeller != null) {
            onlineSeller.sendMessage(plugin.getConfigManager().message("item-sold"));
        }
    }

    public boolean cancel(Player seller, OfferRecord record) {
        if (!record.getSellerUuid().equals(seller.getUniqueId())) return false;
        if (record.getStatus() != OfferStatus.ACTIVE) return false;

        try {
            offerDao.markStatus(record.getId(), OfferStatus.CANCELLED, null, false);
            InventoryUtil.giveItem(seller, record.getItem());
            offerDao.markDelivered(record.getId());
            logDao.log(record.getId(), "CANCEL", seller.getUniqueId(), seller.getName(), "manuell beendet");
            return true;

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Konnte Angebot nicht abbrechen", e);
            return false;
        }
    }

    public void deliverPending(Player player) {
        try {
            for (OfferRecord record : offerDao.findExpiredUndelivered()) {
                if (!record.getSellerUuid().equals(player.getUniqueId())) continue;

                InventoryUtil.giveItem(player, record.getItem());
                offerDao.markDelivered(record.getId());
                player.sendMessage(plugin.getConfigManager().message("offer-returned"));
            }
        } catch (SQLException | IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Konnte Escrow nicht ausliefern", e);
        }
    }

    private void startExpiryTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (OfferRecord record : offerDao.findExpiredActive()) {
                        offerDao.markStatus(record.getId(), OfferStatus.EXPIRED, null, false);
                        logDao.log(record.getId(), "EXPIRE", null, null, "Automatisch abgelaufen");

                        Player seller = Bukkit.getPlayer(record.getSellerUuid());
                        if (seller != null) {
                            InventoryUtil.giveItem(seller, record.getItem());
                            offerDao.markDelivered(record.getId());
                            seller.sendMessage(plugin.getConfigManager().message("offer-expired"));
                        }
                    }
                } catch (SQLException | IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Fehler beim Ablauf-Check", e);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L * 60L);
    }

    public static class OfferDraft {
        private final ItemStack item;
        private final Instant expiresAt;
        private int amount;
        private long price;

        public OfferDraft(ItemStack item, int amount, long price, Instant expiresAt) {
            this.item = item;
            this.amount = amount;
            this.price = price;
            this.expiresAt = expiresAt;
        }

        public ItemStack getItem() { return item; }
        public int getAmount() { return amount; }
        public long getPrice() { return price; }
        public Instant getExpiresAt() { return expiresAt; }
        public void setAmount(int amount) { this.amount = amount; }
        public void setPrice(long price) { this.price = price; }
    }

    public int countActive(UUID uuid) {
        try {
            return (int) offerDao.findForSeller(uuid, OfferStatus.ACTIVE).size();
        } catch (SQLException | IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Konnte aktive Angebote nicht zählen", e);
            return 0;
        }
    }

    public int resolveMaxOffers(Player player) {
        if (player.hasPermission("zbencoins.market.limit.bypass")) return Integer.MAX_VALUE;

        int max = plugin.getConfig().getInt("market.default-limit", 3);
        for (int i = 1; i <= 64; i++) {
            if (player.hasPermission("zbencoins.market.limit." + i)) {
                max = Math.max(max, i);
            }
        }
        return max;
    }
}
