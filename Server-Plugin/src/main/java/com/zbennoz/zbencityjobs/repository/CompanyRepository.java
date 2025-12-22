package com.zbennoz.zbencityjobs.repository;

import com.zbennoz.zbencityjobs.model.Company;
import com.zbennoz.zbencityjobs.model.CompanyRole;
import com.zbennoz.zbencityjobs.storage.DatabaseManager;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CompanyRepository {
    private final DatabaseManager database;

    public CompanyRepository(DatabaseManager database) {
        this.database = database;
    }

    public void init() throws SQLException {
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS companies (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT UNIQUE NOT NULL, " +
                    "owner_uuid TEXT NOT NULL" +
                    ")");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS company_members (" +
                    "company_id INTEGER NOT NULL, " +
                    "member_uuid TEXT NOT NULL, " +
                    "role TEXT NOT NULL, " +
                    "PRIMARY KEY(company_id, member_uuid), " +
                    "FOREIGN KEY(company_id) REFERENCES companies(id) ON DELETE CASCADE" +
                    ")");
        }
    }

    public int insert(Company company) throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement("INSERT INTO companies(name, owner_uuid) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, company.getName());
            ps.setString(2, company.getOwner().toString());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                company.setId(id);
                for (Map.Entry<UUID, CompanyRole> entry : company.getMembers().entrySet()) {
                    upsertMember(id, entry.getKey(), entry.getValue());
                }
                return id;
            }
        }
        return 0;
    }

    public Optional<Company> findByName(String name) throws SQLException {
        String sql = "SELECT * FROM companies WHERE LOWER(name)=LOWER(?)";
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Company company = new Company(rs.getInt("id"), rs.getString("name"), UUID.fromString(rs.getString("owner_uuid")));
                loadMembers(company);
                return Optional.of(company);
            }
        }
        return Optional.empty();
    }

    public Optional<Company> findByOwner(UUID owner) throws SQLException {
        String sql = "SELECT * FROM companies WHERE owner_uuid=?";
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Company company = new Company(rs.getInt("id"), rs.getString("name"), UUID.fromString(rs.getString("owner_uuid")));
                loadMembers(company);
                return Optional.of(company);
            }
        }
        return Optional.empty();
    }

    public Optional<Company> findByMember(UUID member) throws SQLException {
        String sql = "SELECT c.* FROM companies c " +
                "LEFT JOIN company_members m ON c.id = m.company_id " +
                "WHERE c.owner_uuid=? OR m.member_uuid=?";
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, member.toString());
            ps.setString(2, member.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Company company = new Company(rs.getInt("id"), rs.getString("name"), UUID.fromString(rs.getString("owner_uuid")));
                loadMembers(company);
                return Optional.of(company);
            }
        }
        return Optional.empty();
    }

    public void upsertMember(int companyId, UUID uuid, CompanyRole role) throws SQLException {
        String sql = "INSERT INTO company_members(company_id, member_uuid, role) VALUES(?,?,?) " +
                "ON CONFLICT(company_id, member_uuid) DO UPDATE SET role=excluded.role";
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, companyId);
            ps.setString(2, uuid.toString());
            ps.setString(3, role.name());
            ps.executeUpdate();
        }
    }

    public void removeMember(int companyId, UUID uuid) throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement("DELETE FROM company_members WHERE company_id=? AND member_uuid=?")) {
            ps.setInt(1, companyId);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

    private void loadMembers(Company company) throws SQLException {
        String sql = "SELECT * FROM company_members WHERE company_id=?";
        Map<UUID, CompanyRole> members = new HashMap<>();
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, company.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                members.put(UUID.fromString(rs.getString("member_uuid")), CompanyRole.valueOf(rs.getString("role")));
            }
        }
        members.forEach((uuid, role) -> company.getMembers().put(uuid, role));
    }
}
