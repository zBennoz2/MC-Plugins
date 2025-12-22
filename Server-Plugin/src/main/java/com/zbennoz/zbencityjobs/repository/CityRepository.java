package com.zbennoz.zbencityjobs.repository;

import com.zbennoz.zbencityjobs.model.City;
import com.zbennoz.zbencityjobs.storage.DatabaseManager;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

public class CityRepository {
    private final DatabaseManager database;

    public CityRepository(DatabaseManager database) {
        this.database = database;
    }

    public void init() throws SQLException {
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS cities (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT UNIQUE NOT NULL, " +
                    "mayor_uuid TEXT, " +
                    "tax_percent REAL NOT NULL" +
                    ")");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS city_members (" +
                    "city_id INTEGER NOT NULL, " +
                    "member_uuid TEXT NOT NULL, " +
                    "PRIMARY KEY(city_id, member_uuid), " +
                    "FOREIGN KEY(city_id) REFERENCES cities(id) ON DELETE CASCADE" +
                    ")");
        }
    }

    public int insert(City city) throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement("INSERT INTO cities(name, mayor_uuid, tax_percent) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, city.getName());
            ps.setString(2, city.getMayor() != null ? city.getMayor().toString() : null);
            ps.setDouble(3, city.getTaxPercent());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                return keys.getInt(1);
            }
        }
        return 0;
    }

    public Optional<City> findByName(String name) throws SQLException {
        String sql = "SELECT * FROM cities WHERE LOWER(name)=LOWER(?)";
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                UUID mayor = rs.getString("mayor_uuid") != null ? UUID.fromString(rs.getString("mayor_uuid")) : null;
                return Optional.of(new City(rs.getInt("id"), rs.getString("name"), mayor, rs.getDouble("tax_percent")));
            }
        }
        return Optional.empty();
    }

    public void update(City city) throws SQLException {
        String sql = "UPDATE cities SET mayor_uuid=?, tax_percent=? WHERE id=?";
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, city.getMayor() != null ? city.getMayor().toString() : null);
            ps.setDouble(2, city.getTaxPercent());
            ps.setInt(3, city.getId());
            ps.executeUpdate();
        }
    }

    public Optional<City> findByMayor(UUID mayor) throws SQLException {
        String sql = "SELECT * FROM cities WHERE mayor_uuid=?";
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, mayor.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                UUID mayorId = rs.getString("mayor_uuid") != null ? UUID.fromString(rs.getString("mayor_uuid")) : null;
                return Optional.of(new City(rs.getInt("id"), rs.getString("name"), mayorId, rs.getDouble("tax_percent")));
            }
        }
        return Optional.empty();
    }

    public Optional<City> findByMember(UUID member) throws SQLException {
        String sql = "SELECT c.* FROM cities c JOIN city_members m ON c.id = m.city_id WHERE m.member_uuid=?";
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, member.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                UUID mayor = rs.getString("mayor_uuid") != null ? UUID.fromString(rs.getString("mayor_uuid")) : null;
                return Optional.of(new City(rs.getInt("id"), rs.getString("name"), mayor, rs.getDouble("tax_percent")));
            }
        }
        return Optional.empty();
    }
}
