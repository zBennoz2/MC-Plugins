package com.zbennoz.zbenlootr.database;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public interface Database {
    void init() throws SQLException;

    Optional<Inventory> loadPlayerInventory(String containerId, UUID playerId) throws SQLException;

    void saveContainer(String containerId, Location location, String type) throws SQLException;

    void savePlayerInventory(String containerId, UUID playerId, Inventory inventory) throws SQLException;

    void close();
}
