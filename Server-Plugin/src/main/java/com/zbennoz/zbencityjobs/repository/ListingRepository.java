package com.zbennoz.zbencityjobs.repository;

import com.zbennoz.zbencityjobs.model.Listing;
import com.zbennoz.zbencityjobs.storage.DatabaseManager;
import com.zbennoz.zbencityjobs.util.ItemSerializer;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ListingRepository {
    private final DatabaseManager database;

    public ListingRepository(DatabaseManager database) {
        this.database = database;
    }

    public void init() throws SQLException {
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS listings (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "seller_uuid TEXT NOT NULL, " +
                    "item TEXT NOT NULL, " +
                    "price REAL NOT NULL, " +
                    "created_at INTEGER NOT NULL" +
                    ")");
        }
    }

    public List<Listing> loadAll() throws SQLException {
        List<Listing> listings = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM listings")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                listings.add(mapRow(rs));
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new SQLException("Failed to deserialize item", e);
        }
        return listings;
    }

    public int insert(Listing listing) throws SQLException {
        String sql = "INSERT INTO listings(seller_uuid, item, price, created_at) VALUES(?,?,?,?)";
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, listing.getSeller().toString());
            ps.setString(2, ItemSerializer.toBase64(listing.getItem()));
            ps.setDouble(3, listing.getPrice());
            ps.setLong(4, listing.getCreatedAt());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (IOException e) {
            throw new SQLException("Failed to serialize item", e);
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement("DELETE FROM listings WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Listing mapRow(ResultSet rs) throws SQLException, IOException, ClassNotFoundException {
        int id = rs.getInt("id");
        UUID seller = UUID.fromString(rs.getString("seller_uuid"));
        ItemStack item = ItemSerializer.fromBase64(rs.getString("item"));
        double price = rs.getDouble("price");
        long createdAt = rs.getLong("created_at");
        return new Listing(id, seller, price, item, createdAt);
    }
}
