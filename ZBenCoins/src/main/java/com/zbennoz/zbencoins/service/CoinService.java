package com.zbennoz.zbencoins.service;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.database.PlayerDao;
import com.zbennoz.zbencoins.database.PlayerRecord;
import com.zbennoz.zbencoins.database.TransactionDao;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * Service für Coin-Operationen mit asynchroner Schreib-Queue.
 */
public class CoinService {

    private final ZBenCoinsPlugin plugin;
    private final PlayerDao playerDao;
    private final TransactionDao transactionDao;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public CoinService(ZBenCoinsPlugin plugin, PlayerDao playerDao, TransactionDao transactionDao) {
        this.plugin = plugin;
        this.playerDao = playerDao;
        this.transactionDao = transactionDao;
    }

    public long getBalance(UUID uuid) {
        try {
            PlayerRecord record = playerDao.find(uuid);
            return record != null ? record.getCoins() : 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Lesen des Kontostands", e);
            return 0;
        }
    }

    public void addCoins(UUID uuid, long amount, String type, String note) {
        executorService.submit(() -> {
            try {
                playerDao.addCoins(uuid, amount);
                transactionDao.insert(uuid, type, amount, note);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Fehler beim Aktualisieren des Kontostands", e);
            }
        });
    }

    public boolean transfer(UUID from, UUID to, long amount) {
        if (amount <= 0) {
            return false;
        }
        long balance = getBalance(from);
        if (balance < amount) {
            return false;
        }
        executorService.submit(() -> {
            try {
                playerDao.addCoins(from, -amount);
                playerDao.addCoins(to, amount);
                transactionDao.insert(from, "PAY_OUT", -amount, "An " + to);
                transactionDao.insert(to, "PAY_IN", amount, "Von " + from);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Fehler beim Überweisen von Coins", e);
            }
        });
        return true;
    }

    public List<PlayerRecord> topBalances(int limit) {
        try {
            return playerDao.topBalances(limit);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Laden der Topliste", e);
            return List.of();
        }
    }

    public long countTransactionsLastDays(UUID uuid, int days) {
        try {
            return transactionDao.countLastDays(uuid, days);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Zählen der Transaktionen", e);
            return 0;
        }
    }

    public void shutdown() {
        executorService.shutdownNow();
    }
}
