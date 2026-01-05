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

    private static final int MIN_AMOUNT_LIMIT = 1;
    private static final int MAX_AMOUNT_LIMIT = 2304;

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

    public void requestPeriodMaxInput(Player player, int offerId) {
        awaitingInput.put(player.getUniqueId(), new PendingInput(InputType.PERIOD_MAX, offerId));
        player.sendMessage(plugin.getConfigManager().message("enter-period-max"));
        player.closeInventory();
    }

    public boolean handleChat(Player player, String message) {
        PendingInput input = awaitingInput.remove(player.getUniqueId());
        if (input == null) {
            return false;
        }
        String normalized = message.trim().replace(" ", "").replace(",", ".");
        debug("Eingabe erhalten", input.type().name(), message, null);

        try {
            switch (input.type()) {
                case PRICE -> {
                    OptionalLong price = parsePrice(normalized);
                    if (price.isEmpty()) {
                        debug("Ungültiger Preis", input.type().name(), message, "parsePrice leer");
                        player.sendMessage(plugin.getConfigManager().message("server-offer-invalid-price"));
                        return true;
                    }
                    long parsed = price.getAsLong();
                    debug("Preis geparst", input.type().name(), message, String.valueOf(parsed));
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (input.offerId() == -1) {
                            updateDraftValue(player, parsed, InputType.PRICE);
                        } else {
                            updateOfferValue(player, input.offerId(), parsed, InputType.PRICE);
                        }
                    });
                }
                case MIN -> {
                    OptionalInt value = parseInteger(normalized, false);
                    if (value.isEmpty()) {
                        debug("Ungültige Mindestmenge", input.type().name(), message, "parseInteger leer");
                        player.sendMessage(plugin.getConfigManager().message("server-offer-invalid-int"));
                        return true;
                    }
                    int parsed = value.getAsInt();
                    debug("Mindestmenge geparst", input.type().name(), message, String.valueOf(parsed));
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (input.offerId() == -1) {
                            updateDraftValue(player, parsed, InputType.MIN);
                        } else {
                            updateOfferValue(player, input.offerId(), parsed, InputType.MIN);
                        }
                    });
                }
                case MAX -> {
                    OptionalInt value = parseInteger(normalized, true);
                    if (value.isEmpty()) {
                        debug("Ungültige Maximalmenge", input.type().name(), message, "parseInteger leer");
                        player.sendMessage(plugin.getConfigManager().message("server-offer-invalid-int"));
                        return true;
                    }
                    int parsed = value.getAsInt();
                    debug("Maximalmenge geparst", input.type().name(), message, String.valueOf(parsed));
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (input.offerId() == -1) {
                            updateDraftValue(player, parsed, InputType.MAX);
                        } else {
                            updateOfferValue(player, input.offerId(), parsed, InputType.MAX);
                        }
                    });
                }
                case PERIOD_MAX -> {
                    OptionalInt value = parseInteger(normalized, true);
                    if (value.isEmpty()) {
                        debug("Ungültiges Wochenlimit", input.type().name(), message, "parseInteger leer");
                        player.sendMessage(plugin.getConfigManager().message("server-offer-invalid-int"));
                        return true;
                    }
                    int parsed = value.getAsInt();
                    debug("Wochenlimit geparst", input.type().name(), message, String.valueOf(parsed));
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (input.offerId() == -1) {
                            updateDraftValue(player, parsed, InputType.PERIOD_MAX);
                        } else {
                            updateOfferValue(player, input.offerId(), parsed, InputType.PERIOD_MAX);
                        }
                    });
                }
            }
        } catch (Exception e) {
            debug("Fehler bei Eingabe", input.type().name(), message, e.getMessage());
            player.sendMessage(plugin.getConfigManager().message("server-offer-invalid"));
        }
        return true;
    }

    private OptionalInt parseInteger(String raw, boolean allowUnlimited) {
        if (raw.isEmpty()) return OptionalInt.empty();
        try {
            double parsed = Double.parseDouble(raw);
            long rounded = Math.round(parsed);
            if (Math.abs(parsed - rounded) > 0.0001) return OptionalInt.empty();
            int value = (int) rounded;
            if (value < 0 && !(allowUnlimited && value == -1)) return OptionalInt.empty();
            if (!allowUnlimited && value == 0) return OptionalInt.of(0);
            if (allowUnlimited && value == 0) return OptionalInt.of(-1);
            if (value != -1) {
                if (value < MIN_AMOUNT_LIMIT || value > MAX_AMOUNT_LIMIT) return OptionalInt.empty();
            }
            return OptionalInt.of(value);
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    private OptionalLong parsePrice(String raw) {
        if (raw.isEmpty()) return OptionalLong.empty();
        try {
            double parsed = Double.parseDouble(raw);
            long value = Math.round(parsed);
            if (value <= 0) return OptionalLong.empty();
            if (value > 10_000_000L) return OptionalLong.empty();
            return OptionalLong.of(value);
        } catch (NumberFormatException e) {
            return OptionalLong.empty();
        }
    }

    private void debug(String step, String type, String raw, String parsed) {
        if (!plugin.getConfigManager().isDebug()) return;
        plugin.getLogger().info("[ServerOfferInput] Schritt=" + type + " | Eingabe='" + raw + "' | Wert=" + parsed);
    }

    private void updateOfferValue(Player player, int offerId, Number value, InputType type) {
        getOffer(offerId).ifPresent(offer -> {
            long price = offer.getPricePerItem();
            Integer min = offer.getMinAmount().orElse(null);
            Integer max = offer.getMaxAmount().orElse(null);
            boolean enabled = offer.isEnabled();
            boolean periodEnabled = offer.isPeriodLimitEnabled();
            Integer periodMax = offer.getPeriodMaxAmount().orElse(null);
            int periodUsed = offer.getPeriodUsedAmount();
            Long periodStart = offer.getPeriodStartMillis().orElse(null);

            switch (type) {
                case PRICE -> price = value == null ? 0 : value.longValue();
                case MIN -> min = value == null ? null : value.intValue();
                case MAX -> max = value == null ? null : value.intValue();
                case PERIOD_MAX -> {
                    periodMax = value == null ? null : value.intValue();
                    if (periodMax != null && periodMax <= 0) {
                        periodMax = null;
                    }
                    periodEnabled = periodMax != null;
                    periodUsed = 0;
                    periodStart = System.currentTimeMillis();
                }
            }
            if (min != null && max != null && max != -1 && min > max) {
                player.sendMessage(plugin.getConfigManager().message("server-offer-invalid"));
                return;
            }
            saveOffer(new ServerOffer(offer.getId(), offer.getType(), offer.getItemStack(), price, enabled, min, max,
                    periodEnabled, offer.getPeriodTicks(), periodMax,
                    periodUsed, periodStart,
                    offer.getCreatedBy().orElse(null), offer.getCreatedAt().orElse(null)));
            player.sendMessage(plugin.getConfigManager().message("server-offer-updated"));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openGui(player,
                    new com.zbennoz.zbencoins.gui.ServerOfferAdminGui(plugin, this, offer.getId(), player)));
        });
    }

    private void updateDraftValue(Player player, Number value, InputType type) {
        getDraft(player).ifPresent(draft -> {
            switch (type) {
                case PRICE -> draft.setPricePerItem(value.longValue());
                case MIN -> draft.setMinAmount(value.intValue() == 0 ? null : value.intValue());
                case MAX -> draft.setMaxAmount(value.intValue() == 0 ? null : value.intValue());
                case PERIOD_MAX -> {
                    draft.setPeriodMaxAmount(value.intValue() == 0 ? null : value.intValue());
                    draft.setPeriodLimitEnabled(draft.getPeriodMaxAmount().isPresent());
                    if (draft.isPeriodLimitEnabled()) {
                        draft.setPeriodStartMillis(System.currentTimeMillis());
                        draft.setPeriodUsedAmount(0);
                    }
                }
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
                offer.getMaxAmount().orElse(null), offer.isPeriodLimitEnabled(), offer.getPeriodTicks(),
                offer.getPeriodMaxAmount().orElse(null), offer.getPeriodUsedAmount(),
                offer.getPeriodStartMillis().orElse(null), offer.getCreatedBy().orElse(null),
                offer.getCreatedAt().orElse(null))));
    }

    public void requestDraft(Player player, ServerOfferType type) {
        if (drafts.containsKey(player.getUniqueId())) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            player.sendMessage(plugin.getConfigManager().message("server-offer-select-item"));
            return;
        }
        int defaultMax = plugin.getConfig().getInt("serverOffers.defaultMaxAmount", 64);
        boolean periodDefault = plugin.getConfig().getBoolean("serverOffers.periodLimit.enabledByDefault", false);
        int defaultPeriodMax = plugin.getConfig().getInt("serverOffers.periodLimit.defaultMaxAmountPerPeriod", 0);
        long defaultPeriodTicks = plugin.getConfig().getLong("serverOffers.periodLimit.periodTicks", 168000L);
        drafts.put(player.getUniqueId(), new OfferDraft(type, hand.clone(), 0, true, MIN_AMOUNT_LIMIT, defaultMax,
                periodDefault, defaultPeriodTicks, defaultPeriodMax, 0, System.currentTimeMillis()));
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

    public void setDraftPeriodMax(Player player, Integer value) {
        getDraft(player).ifPresent(draft -> draft.setPeriodMaxAmount(value));
    }

    public void toggleDraftPeriodLimit(Player player) {
        getDraft(player).ifPresent(draft -> {
            draft.setPeriodLimitEnabled(!draft.isPeriodLimitEnabled());
            if (draft.isPeriodLimitEnabled()) {
                draft.setPeriodStartMillis(System.currentTimeMillis());
                if (draft.getPeriodMaxAmount().isEmpty()) {
                    int defaultMax = plugin.getConfig().getInt("serverOffers.periodLimit.defaultMaxAmountPerPeriod", 0);
                    draft.setPeriodMaxAmount(defaultMax <= 0 ? null : defaultMax);
                }
            } else {
                draft.setPeriodMaxAmount(null);
                draft.setPeriodUsedAmount(0);
            }
        });
    }

    public void toggleOfferPeriodLimit(int offerId, Player player) {
        getOffer(offerId).ifPresent(offer -> {
            boolean newState = !offer.isPeriodLimitEnabled();
            Integer periodMax = offer.getPeriodMaxAmount().orElse(null);
            if (newState && periodMax == null) {
                int defaultMax = plugin.getConfig().getInt("serverOffers.periodLimit.defaultMaxAmountPerPeriod", 0);
                periodMax = defaultMax <= 0 ? null : defaultMax;
            }
            long start = offer.getPeriodStartMillis().orElse(System.currentTimeMillis());
            saveOffer(new ServerOffer(offer.getId(), offer.getType(), offer.getItemStack(), offer.getPricePerItem(),
                    offer.isEnabled(), offer.getMinAmount().orElse(null), offer.getMaxAmount().orElse(null), newState,
                    offer.getPeriodTicks(), periodMax, newState ? 0 : offer.getPeriodUsedAmount(), newState ? start : null,
                    offer.getCreatedBy().orElse(null), offer.getCreatedAt().orElse(null)));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openGui(player,
                    new com.zbennoz.zbencoins.gui.ServerOfferAdminGui(plugin, this, offerId, player)));
        });
    }

    public Optional<ServerOffer> publishDraft(Player player) {
        Optional<OfferDraft> optional = getDraft(player);
        if (optional.isEmpty()) return Optional.empty();
        OfferDraft draft = optional.get();
        if (draft.getPricePerItem() <= 0 || draft.getItemStack().getType() == Material.AIR) {
            player.sendMessage(plugin.getConfigManager().message("server-offer-invalid"));
            return Optional.empty();
        }

        if (draft.getMinAmount().isPresent() && draft.getMaxAmount().isPresent()) {
            int min = draft.getMinAmount().get();
            int max = draft.getMaxAmount().get();
            if (max != -1 && min > max) {
                player.sendMessage(plugin.getConfigManager().message("server-offer-invalid"));
                return Optional.empty();
            }
        }

        try {
            ServerOffer offer = dao.insert(draft.getType(), draft.getItemStack(), draft.getPricePerItem(), draft.isEnabled(),
                    draft.getMinAmount().orElse(null), draft.getMaxAmount().orElse(null), draft.isPeriodLimitEnabled(),
                    draft.getPeriodTicks(), draft.getPeriodMaxAmount().orElse(null), draft.getPeriodUsedAmount(),
                    draft.isPeriodLimitEnabled() ? draft.getPeriodStartMillis() : null, player.getName());
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
        ServerOffer current = refreshPeriod(offer);
        Optional<String> limit = processLimits(current, amount);
        if (limit.isPresent()) return limit;
        Optional<String> result = executePurchase(player, current, amount);
        result.ifPresentOrElse($ -> {}, () -> incrementPeriodUsage(current, amount));
        return result;
    }

    public Optional<String> handleSell(Player player, ServerOffer offer, int amount) {
        if (!offer.isEnabled()) {
            return Optional.of(plugin.getConfigManager().message("server-offer-disabled"));
        }
        if (offer.getType() != ServerOfferType.BUY_FROM_PLAYER) {
            return Optional.of(plugin.getConfigManager().message("server-offer-invalid"));
        }
        ServerOffer current = refreshPeriod(offer);
        Optional<String> limit = processLimits(current, amount);
        if (limit.isPresent()) return limit;
        Optional<String> result = executeSell(player, current, amount);
        result.ifPresentOrElse($ -> {}, () -> incrementPeriodUsage(current, amount));
        return result;
    }

    private Optional<String> processLimits(ServerOffer offer, int amount) {
        if (offer.getMinAmount().isPresent() && offer.getMinAmount().get() > 0 && amount < offer.getMinAmount().get()) {
            return Optional.of(plugin.getConfigManager().message("server-offer-limits", Map.of(
                    "min", String.valueOf(offer.getMinAmount().get()),
                    "max", String.valueOf(offer.getMaxAmount().orElse(amount))
            )));
        }
        if (offer.getMaxAmount().isPresent() && offer.getMaxAmount().get() != -1 && amount > offer.getMaxAmount().get()) {
            return Optional.of(plugin.getConfigManager().message("server-offer-limits", Map.of(
                    "min", String.valueOf(offer.getMinAmount().orElse(1)),
                    "max", String.valueOf(offer.getMaxAmount().get())
            )));
        }
        if (offer.isPeriodLimitEnabled() && offer.getPeriodMaxAmount().isPresent() && offer.getPeriodMaxAmount().get() != -1) {
            int remaining = offer.getPeriodMaxAmount().get() - offer.getPeriodUsedAmount();
            if (remaining < amount) {
                long millis = remainingMillis(offer);
                return Optional.of(plugin.getConfigManager().message("server-offer-weekly-limit", Map.of(
                        "remaining", String.valueOf(Math.max(0, remaining)),
                        "limit", String.valueOf(offer.getPeriodMaxAmount().get()),
                        "reset", formatDuration(millis)
                )));
            }
        }
        return Optional.empty();
    }

    private ServerOffer refreshPeriod(ServerOffer offer) {
        if (!offer.isPeriodLimitEnabled()) {
            return offer;
        }
        long now = System.currentTimeMillis();
        long start = offer.getPeriodStartMillis().orElse(now);
        long duration = resolvePeriodMillis(offer.getPeriodTicks());
        if (now - start >= duration) {
            ServerOffer refreshed = new ServerOffer(offer.getId(), offer.getType(), offer.getItemStack(),
                    offer.getPricePerItem(), offer.isEnabled(), offer.getMinAmount().orElse(null),
                    offer.getMaxAmount().orElse(null), offer.isPeriodLimitEnabled(), offer.getPeriodTicks(),
                    offer.getPeriodMaxAmount().orElse(null), 0, now, offer.getCreatedBy().orElse(null),
                    offer.getCreatedAt().orElse(null));
            saveOffer(refreshed);
            return refreshed;
        }
        return offer;
    }

    private long resolvePeriodMillis(long periodTicks) {
        return periodTicks * 50L;
    }

    private long remainingMillis(ServerOffer offer) {
        long now = System.currentTimeMillis();
        long start = offer.getPeriodStartMillis().orElse(now);
        long duration = resolvePeriodMillis(offer.getPeriodTicks());
        return Math.max(0, duration - (now - start));
    }

    private String formatDuration(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long hours = minutes / 60;
        minutes = minutes % 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    private void incrementPeriodUsage(ServerOffer offer, int amount) {
        if (!offer.isPeriodLimitEnabled() || offer.getPeriodMaxAmount().isEmpty()) return;
        Integer max = offer.getPeriodMaxAmount().get();
        if (max == -1) return;
        int newUsed = Math.min(max, offer.getPeriodUsedAmount() + amount);
        saveOffer(new ServerOffer(offer.getId(), offer.getType(), offer.getItemStack(), offer.getPricePerItem(),
                offer.isEnabled(), offer.getMinAmount().orElse(null), offer.getMaxAmount().orElse(null),
                offer.isPeriodLimitEnabled(), offer.getPeriodTicks(), offer.getPeriodMaxAmount().orElse(null), newUsed,
                offer.getPeriodStartMillis().orElse(System.currentTimeMillis()), offer.getCreatedBy().orElse(null),
                offer.getCreatedAt().orElse(null)));
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

    private enum InputType { PRICE, MIN, MAX, PERIOD_MAX }

    public static class OfferDraft {
        private final ServerOfferType type;
        private final ItemStack itemStack;
        private long pricePerItem;
        private boolean enabled;
        private Integer minAmount;
        private Integer maxAmount;
        private boolean periodLimitEnabled;
        private long periodTicks;
        private Integer periodMaxAmount;
        private int periodUsedAmount;
        private long periodStartMillis;

        public OfferDraft(ServerOfferType type, ItemStack itemStack, long pricePerItem, boolean enabled,
                          Integer minAmount, Integer maxAmount, boolean periodLimitEnabled, long periodTicks,
                          Integer periodMaxAmount, int periodUsedAmount, long periodStartMillis) {
            this.type = type;
            this.itemStack = itemStack;
            this.pricePerItem = pricePerItem;
            this.enabled = enabled;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.periodLimitEnabled = periodLimitEnabled;
            this.periodTicks = periodTicks;
            this.periodMaxAmount = periodMaxAmount;
            this.periodUsedAmount = periodUsedAmount;
            this.periodStartMillis = periodStartMillis;
        }

        public ServerOfferType getType() { return type; }

        public ItemStack getItemStack() { return itemStack; }

        public long getPricePerItem() { return pricePerItem; }

        public boolean isEnabled() { return enabled; }

        public Optional<Integer> getMinAmount() { return Optional.ofNullable(minAmount); }

        public Optional<Integer> getMaxAmount() { return Optional.ofNullable(maxAmount); }

        public boolean isPeriodLimitEnabled() { return periodLimitEnabled; }

        public long getPeriodTicks() { return periodTicks; }

        public Optional<Integer> getPeriodMaxAmount() { return Optional.ofNullable(periodMaxAmount); }

        public int getPeriodUsedAmount() { return periodUsedAmount; }

        public long getPeriodStartMillis() { return periodStartMillis; }

        public void setPricePerItem(long pricePerItem) { this.pricePerItem = pricePerItem; }

        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public void setMinAmount(Integer minAmount) { this.minAmount = minAmount; }

        public void setMaxAmount(Integer maxAmount) { this.maxAmount = maxAmount; }

        public void setPeriodLimitEnabled(boolean periodLimitEnabled) { this.periodLimitEnabled = periodLimitEnabled; }

        public void setPeriodTicks(long periodTicks) { this.periodTicks = periodTicks; }

        public void setPeriodMaxAmount(Integer periodMaxAmount) { this.periodMaxAmount = periodMaxAmount; }

        public void setPeriodUsedAmount(int periodUsedAmount) { this.periodUsedAmount = periodUsedAmount; }

        public void setPeriodStartMillis(long periodStartMillis) { this.periodStartMillis = periodStartMillis; }
    }
}
