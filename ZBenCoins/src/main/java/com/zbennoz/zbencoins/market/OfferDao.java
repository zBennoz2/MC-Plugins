package com.zbennoz.zbencoins.market;

import com.zbennoz.zbencoins.util.ItemSerializer;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Datenzugriffsschicht für Marktangebote.
 */
public class OfferDao {

    private final Connection connection;

    public OfferDao(Connection connection) {
        this.connection = connection;
    }

    public synchronized OfferRecord insert(UUID seller, String sellerName, ItemStack itemStack, int amount, long price,
                                           Instant expiresAt) throws SQLException, IOException {
        String serialized = ItemSerializer.serialize(itemStack);
        long now = Instant.now().getEpochSecond();
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO market_offers(seller_uuid, seller_name, item_data, amount, price, status, expires_at, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, seller.toString());
            stmt.setString(2, sellerName);
            stmt.setString(3, serialized);
            stmt.setInt(4, amount);
            stmt.setLong(5, price);
            stmt.setString(6, OfferStatus.ACTIVE.name());
            stmt.setLong(7, expiresAt.getEpochSecond());
            stmt.setLong(8, now);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return new OfferRecord(keys.getInt(1), seller, sellerName, null, itemStack, amount, price,
                            OfferStatus.ACTIVE, expiresAt, Instant.ofEpochSecond(now), false);
                }
            }
        }
        throw new SQLException("Kein Schlüssel beim Speichern eines Angebots erhalten");
    }

    public synchronized List<OfferRecord> findActive(int limit, int offset) throws SQLException, IOException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM market_offers WHERE status = ? AND expires_at > ? ORDER BY created_at DESC LIMIT ? OFFSET ?")) {
            stmt.setString(1, OfferStatus.ACTIVE.name());
            stmt.setLong(2, Instant.now().getEpochSecond());
            stmt.setInt(3, limit);
            stmt.setInt(4, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                List<OfferRecord> offers = new ArrayList<>();
                while (rs.next()) {
                    offers.add(fromResult(rs));
                }
                return offers;
            }
        }
    }

    public synchronized int countActive() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) as cnt FROM market_offers WHERE status = ? AND expires_at > ?")) {
            stmt.setString(1, OfferStatus.ACTIVE.name());
            stmt.setLong(2, Instant.now().getEpochSecond());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt");
                }
            }
        }
        return 0;
    }

    public synchronized List<OfferRecord> findForSeller(UUID seller, OfferStatus... statuses) throws SQLException, IOException {
        StringBuilder query = new StringBuilder("SELECT * FROM market_offers WHERE seller_uuid = ?");
        if (statuses.length > 0) {
            query.append(" AND status IN (");
            query.append("?,".repeat(statuses.length));
            query.setLength(query.length() - 1);
            query.append(")");
        }
        query.append(" ORDER BY created_at DESC");
        try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
            stmt.setString(1, seller.toString());
            int index = 2;
            for (OfferStatus status : statuses) {
                stmt.setString(index++, status.name());
            }
            try (ResultSet rs = stmt.executeQuery()) {
                List<OfferRecord> offers = new ArrayList<>();
                while (rs.next()) {
                    offers.add(fromResult(rs));
                }
                return offers;
            }
        }
    }

    public synchronized List<OfferRecord> findBought(UUID buyer, int limit) throws SQLException, IOException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM market_offers WHERE buyer_uuid = ? AND status = ? ORDER BY created_at DESC LIMIT ?")) {
            stmt.setString(1, buyer.toString());
            stmt.setString(2, OfferStatus.SOLD.name());
            stmt.setInt(3, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                List<OfferRecord> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(fromResult(rs));
                }
                return list;
            }
        }
    }

    public synchronized List<OfferRecord> findSoldBySeller(UUID seller, int limit) throws SQLException, IOException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM market_offers WHERE seller_uuid = ? AND status = ? ORDER BY created_at DESC LIMIT ?")) {
            stmt.setString(1, seller.toString());
            stmt.setString(2, OfferStatus.SOLD.name());
            stmt.setInt(3, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                List<OfferRecord> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(fromResult(rs));
                }
                return list;
            }
        }
    }

    public synchronized Optional<OfferRecord> findById(int id) throws SQLException, IOException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM market_offers WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(fromResult(rs));
                }
            }
        }
        return Optional.empty();
    }

    public synchronized void markStatus(int id, OfferStatus status, UUID buyer, boolean delivered) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE market_offers SET status = ?, buyer_uuid = ?, delivered = ? WHERE id = ?")) {
            stmt.setString(1, status.name());
            stmt.setString(2, buyer == null ? null : buyer.toString());
            stmt.setInt(3, delivered ? 1 : 0);
            stmt.setInt(4, id);
            stmt.executeUpdate();
        }
    }

    public synchronized boolean reserveForPurchase(int id, UUID buyer) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE market_offers SET status = ?, buyer_uuid = ? WHERE id = ? AND status = ? AND expires_at > ?")) {
            stmt.setString(1, OfferStatus.SOLD.name());
            stmt.setString(2, buyer.toString());
            stmt.setInt(3, id);
            stmt.setString(4, OfferStatus.ACTIVE.name());
            stmt.setLong(5, Instant.now().getEpochSecond());
            return stmt.executeUpdate() == 1;
        }
    }

    public synchronized void markDelivered(int id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("UPDATE market_offers SET delivered = 1 WHERE id = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public synchronized List<OfferRecord> findExpiredUndelivered() throws SQLException, IOException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM market_offers WHERE status IN (?, ?) AND delivered = 0")) {
            stmt.setString(1, OfferStatus.EXPIRED.name());
            stmt.setString(2, OfferStatus.CANCELLED.name());
            try (ResultSet rs = stmt.executeQuery()) {
                List<OfferRecord> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(fromResult(rs));
                }
                return list;
            }
        }
    }

    public synchronized List<OfferRecord> findExpiredActive() throws SQLException, IOException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM market_offers WHERE status = ? AND expires_at <= ?")) {
            stmt.setString(1, OfferStatus.ACTIVE.name());
            stmt.setLong(2, Instant.now().getEpochSecond());
            try (ResultSet rs = stmt.executeQuery()) {
                List<OfferRecord> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(fromResult(rs));
                }
                return list;
            }
        }
    }

    private OfferRecord fromResult(ResultSet rs) throws SQLException, IOException {
        int id = rs.getInt("id");
        UUID seller = UUID.fromString(rs.getString("seller_uuid"));
        String sellerName = rs.getString("seller_name");
        String buyer = rs.getString("buyer_uuid");
        ItemStack item = ItemSerializer.deserialize(rs.getString("item_data"));
        int amount = rs.getInt("amount");
        long price = rs.getLong("price");
        OfferStatus status = OfferStatus.valueOf(rs.getString("status"));
        Instant expiresAt = Instant.ofEpochSecond(rs.getLong("expires_at"));
        Instant createdAt = Instant.ofEpochSecond(rs.getLong("created_at"));
        boolean delivered = rs.getInt("delivered") == 1;
        return new OfferRecord(id, seller, sellerName, buyer == null ? null : UUID.fromString(buyer), item, amount, price,
                status, expiresAt, createdAt, delivered);
    }
}
