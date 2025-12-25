package com.zbennoz.zbencoins.market;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

/**
 * Protokolliert Marktaktionen für spätere Auswertung.
 */
public class MarketLogDao {

    private final Connection connection;

    public MarketLogDao(Connection connection) {
        this.connection = connection;
    }

    public synchronized void log(int offerId, String action, UUID actor, String actorName, String note) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO market_logs(offer_id, action, actor_uuid, actor_name, note, created_at) VALUES (?, ?, ?, ?, ?, ?" +
                        ")")) {
            stmt.setInt(1, offerId);
            stmt.setString(2, action);
            stmt.setString(3, actor == null ? null : actor.toString());
            stmt.setString(4, actorName);
            stmt.setString(5, note);
            stmt.setLong(6, Instant.now().getEpochSecond());
            stmt.executeUpdate();
        }
    }
}
