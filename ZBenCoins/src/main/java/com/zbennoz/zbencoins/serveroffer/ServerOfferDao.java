package com.zbennoz.zbencoins.serveroffer;

import com.zbennoz.zbencoins.util.ItemSerializer;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Datenbankzugriff für Server-Angebote.
 */
public class ServerOfferDao {

    private final Connection connection;

    public ServerOfferDao(Connection connection) {
        this.connection = connection;
    }

    public synchronized ServerOffer insert(ServerOfferType type, ItemStack stack, long pricePerItem, boolean enabled,
                                           Integer minAmount, Integer maxAmount, boolean periodLimitEnabled,
                                           long periodTicks, Integer periodMaxAmount, int periodUsedAmount,
                                           Long periodStartMillis, String creator)
            throws SQLException, IOException {
        String serialized = ItemSerializer.serialize(stack);
        long now = Instant.now().getEpochSecond();
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO server_offers(type, item_data, price_per_item, enabled, min_amount, max_amount, period_limit_enabled, period_ticks, period_max_amount, period_used_amount, period_start_millis, created_by, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, type.name());
            stmt.setString(2, serialized);
            stmt.setLong(3, pricePerItem);
            stmt.setInt(4, enabled ? 1 : 0);
            stmt.setObject(5, minAmount);
            stmt.setObject(6, maxAmount);
            stmt.setInt(7, periodLimitEnabled ? 1 : 0);
            stmt.setLong(8, periodTicks);
            stmt.setObject(9, periodMaxAmount);
            stmt.setInt(10, periodUsedAmount);
            stmt.setObject(11, periodStartMillis);
            stmt.setString(12, creator);
            stmt.setLong(13, now);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return new ServerOffer(keys.getInt(1), type, stack, pricePerItem, enabled, minAmount, maxAmount,
                            periodLimitEnabled, periodTicks, periodMaxAmount, periodUsedAmount, periodStartMillis,
                            creator, Instant.ofEpochSecond(now));
                }
            }
        }
        throw new SQLException("Kein Schlüssel beim Speichern des Server-Angebots erhalten");
    }

    public synchronized List<ServerOffer> findAll() throws SQLException, IOException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM server_offers")) {
            try (ResultSet rs = stmt.executeQuery()) {
                List<ServerOffer> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(fromResult(rs));
                }
                return list;
            }
        }
    }

    public synchronized Optional<ServerOffer> findById(int id) throws SQLException, IOException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM server_offers WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(fromResult(rs));
                }
            }
        }
        return Optional.empty();
    }

    public synchronized void delete(int id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM server_offers WHERE id = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public synchronized void update(ServerOffer offer) throws SQLException, IOException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE server_offers SET type = ?, item_data = ?, price_per_item = ?, enabled = ?, min_amount = ?, max_amount = ?, " +
                        "period_limit_enabled = ?, period_ticks = ?, period_max_amount = ?, period_used_amount = ?, period_start_millis = ? " +
                        "WHERE id = ?")) {
            stmt.setString(1, offer.getType().name());
            stmt.setString(2, ItemSerializer.serialize(offer.getItemStack()));
            stmt.setLong(3, offer.getPricePerItem());
            stmt.setInt(4, offer.isEnabled() ? 1 : 0);
            stmt.setObject(5, offer.getMinAmount().orElse(null));
            stmt.setObject(6, offer.getMaxAmount().orElse(null));
            stmt.setInt(7, offer.isPeriodLimitEnabled() ? 1 : 0);
            stmt.setLong(8, offer.getPeriodTicks());
            stmt.setObject(9, offer.getPeriodMaxAmount().orElse(null));
            stmt.setInt(10, offer.getPeriodUsedAmount());
            stmt.setObject(11, offer.getPeriodStartMillis().orElse(null));
            stmt.setInt(12, offer.getId());
            stmt.executeUpdate();
        }
    }

    private ServerOffer fromResult(ResultSet rs) throws SQLException, IOException {
        int id = rs.getInt("id");
        ServerOfferType type = ServerOfferType.valueOf(rs.getString("type"));
        ItemStack stack = ItemSerializer.deserialize(rs.getString("item_data"));
        long price = rs.getLong("price_per_item");
        boolean enabled = rs.getInt("enabled") == 1;
        Integer min = rs.getObject("min_amount") == null ? null : rs.getInt("min_amount");
        Integer max = rs.getObject("max_amount") == null ? null : rs.getInt("max_amount");
        boolean periodLimitEnabled = columnExists(rs, "period_limit_enabled") && rs.getInt("period_limit_enabled") == 1;
        long periodTicks = columnExists(rs, "period_ticks") ? rs.getLong("period_ticks") : 168000L;
        Integer periodMaxAmount = columnExists(rs, "period_max_amount") && rs.getObject("period_max_amount") != null
                ? rs.getInt("period_max_amount") : null;
        int periodUsedAmount = columnExists(rs, "period_used_amount") ? rs.getInt("period_used_amount") : 0;
        Long periodStartMillis = columnExists(rs, "period_start_millis") && rs.getObject("period_start_millis") != null
                ? rs.getLong("period_start_millis") : null;
        String creator = rs.getString("created_by");
        Instant createdAt = rs.getObject("created_at") == null ? null : Instant.ofEpochSecond(rs.getLong("created_at"));
        return new ServerOffer(id, type, stack, price, enabled, min, max, periodLimitEnabled, periodTicks,
                periodMaxAmount, periodUsedAmount, periodStartMillis, creator, createdAt);
    }

    private boolean columnExists(ResultSet rs, String column) {
        try {
            rs.findColumn(column);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
