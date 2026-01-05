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
                                           Integer minAmount, Integer maxAmount, String creator)
            throws SQLException, IOException {
        String serialized = ItemSerializer.serialize(stack);
        long now = Instant.now().getEpochSecond();
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO server_offers(type, item_data, price_per_item, enabled, min_amount, max_amount, created_by, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, type.name());
            stmt.setString(2, serialized);
            stmt.setLong(3, pricePerItem);
            stmt.setInt(4, enabled ? 1 : 0);
            stmt.setObject(5, minAmount);
            stmt.setObject(6, maxAmount);
            stmt.setString(7, creator);
            stmt.setLong(8, now);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return new ServerOffer(keys.getInt(1), type, stack, pricePerItem, enabled, minAmount, maxAmount,
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
                "UPDATE server_offers SET type = ?, item_data = ?, price_per_item = ?, enabled = ?, min_amount = ?, max_amount = ? " +
                        "WHERE id = ?")) {
            stmt.setString(1, offer.getType().name());
            stmt.setString(2, ItemSerializer.serialize(offer.getItemStack()));
            stmt.setLong(3, offer.getPricePerItem());
            stmt.setInt(4, offer.isEnabled() ? 1 : 0);
            stmt.setObject(5, offer.getMinAmount().orElse(null));
            stmt.setObject(6, offer.getMaxAmount().orElse(null));
            stmt.setInt(7, offer.getId());
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
        String creator = rs.getString("created_by");
        Instant createdAt = rs.getObject("created_at") == null ? null : Instant.ofEpochSecond(rs.getLong("created_at"));
        return new ServerOffer(id, type, stack, price, enabled, min, max, creator, createdAt);
    }
}
