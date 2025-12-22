package com.zbennoz.zbencityjobs.service;

import com.zbennoz.zbencityjobs.ZBenCityJobs;
import org.bukkit.Bukkit;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight economy replacement backed by SQLite.
 * Reads are served from a cache, writes are performed asynchronously via the Bukkit scheduler.
 */
public class CoinService {
    private final ZBenCityJobs plugin;
    private final Map<UUID, Long> balanceCache = new ConcurrentHashMap<>();
    private DataSource dataSource;
    private boolean allowNegative;
    private boolean transactionLog;
    private String currencyName;
    private String currencySymbol;
    private long startingBalance;
    private long maxAmount;

    public CoinService(ZBenCityJobs plugin) {
        this.plugin = plugin;
    }

    public boolean init() {
        this.allowNegative = plugin.getConfig().getBoolean("coins.allow-negative", false);
        this.transactionLog = plugin.getConfig().getBoolean("coins.transaction-log", true);
        this.currencyName = plugin.getConfig().getString("coins.currency-name", "ZBenCoins");
        this.currencySymbol = plugin.getConfig().getString("coins.currency-symbol", "⛃");
        this.startingBalance = plugin.getConfig().getLong("coins.starting-balance", 0L);
        this.maxAmount = plugin.getConfig().getLong("coins.max-amount", 1_000_000_000L);

        try {
            this.dataSource = createDataSource();
            createTables();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize ZBenCoins storage: " + e.getMessage());
            return false;
        }
    }

    private DataSource createDataSource() {
        File dbFile = new File(plugin.getDataFolder(), "zbencoins.db");
        if (!dbFile.getParentFile().exists() && !dbFile.getParentFile().mkdirs()) {
            plugin.getLogger().warning("Could not create data folder for ZBenCoins");
        }
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        config.setPragma(SQLiteConfig.Pragma.JOURNAL_MODE, "WAL");
        config.setPragma(SQLiteConfig.Pragma.SYNCHRONOUS, "NORMAL");
        config.setBusyTimeout(3000);
        SQLiteDataSource source = new SQLiteDataSource(config);
        source.setUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        return source;
    }

    private void createTables() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS coins_accounts (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "balance INTEGER NOT NULL DEFAULT 0" +
                    ")");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS coins_transactions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid TEXT NOT NULL, " +
                    "amount INTEGER NOT NULL, " +
                    "reason TEXT, " +
                    "created_at INTEGER NOT NULL" +
                    ")");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_coins_transactions_uuid ON coins_transactions(uuid)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_coins_transactions_created ON coins_transactions(created_at)");
        }
    }

    public String formatAmount(long amount) {
        return amount + " " + currencySymbol;
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public long getMaxAmount() {
        return maxAmount;
    }

    /**
     * Loads an account balance into the cache or creates a new account with the starting balance.
     */
    public long loadAccount(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        long balance = fetchBalance(uuid);
        balanceCache.put(uuid, balance);
        return balance;
    }

    private long fetchBalance(UUID uuid) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT balance FROM coins_accounts WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("balance");
            }
            // Initialize account with starting balance when not present
            saveAccountSync(uuid, startingBalance);
            return startingBalance;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to fetch balance: " + e.getMessage());
            return 0L;
        }
    }

    public long getBalance(UUID uuid) {
        return balanceCache.computeIfAbsent(uuid, this::fetchBalance);
    }

    public boolean has(UUID uuid, long amount) {
        if (amount < 0) return false;
        if (allowNegative) return true;
        return getBalance(uuid) >= amount;
    }

    public void setBalance(UUID uuid, long amount, String reason) {
        if (amount > maxAmount) amount = maxAmount;
        final long newBalance = amount;
        final long previous = getBalance(uuid);
        balanceCache.put(uuid, newBalance);
        long delta = newBalance - previous;
        writeAsync(() -> {
            saveAccount(uuid, newBalance);
            logTransaction(uuid, delta, reason);
        });
    }

    public boolean add(UUID uuid, long amount, String reason) {
        if (!isValidAmount(amount)) return false;
        long current = getBalance(uuid);
        long newBalance = Math.min(maxAmount, current + amount);
        balanceCache.put(uuid, newBalance);
        long delta = newBalance - current;
        writeAsync(() -> {
            saveAccount(uuid, newBalance);
            logTransaction(uuid, delta, reason);
        });
        return true;
    }

    public boolean remove(UUID uuid, long amount, String reason) {
        if (!isValidAmount(amount)) return false;
        long current = getBalance(uuid);
        if (!allowNegative && current < amount) {
            return false;
        }
        long newBalance = current - amount;
        balanceCache.put(uuid, newBalance);
        writeAsync(() -> {
            saveAccount(uuid, newBalance);
            logTransaction(uuid, -amount, reason);
        });
        return true;
    }

    public boolean transfer(UUID from, UUID to, long amount, String reason) {
        if (!isValidAmount(amount)) return false;
        if (from.equals(to)) return false;
        long fromBalance = getBalance(from);
        long toBalance = getBalance(to);
        if (!allowNegative && fromBalance < amount) {
            return false;
        }
        long updatedFrom = fromBalance - amount;
        long updatedTo = Math.min(maxAmount, toBalance + amount);
        balanceCache.put(from, updatedFrom);
        balanceCache.put(to, updatedTo);
        writeAsync(() -> {
            saveAccount(from, updatedFrom);
            saveAccount(to, updatedTo);
            logTransaction(from, -amount, reason);
            logTransaction(to, amount, reason);
        });
        return true;
    }

    public void shutdown() {
        // Persist cached balances synchronously to avoid data loss on shutdown.
        for (Map.Entry<UUID, Long> entry : balanceCache.entrySet()) {
            saveAccountSync(entry.getKey(), entry.getValue());
        }
    }

    private boolean isValidAmount(long amount) {
        return amount > 0 && amount <= maxAmount;
    }

    private void writeAsync(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    private void saveAccount(UUID uuid, long balance) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("INSERT INTO coins_accounts(uuid, balance) VALUES(?,?) " +
                     "ON CONFLICT(uuid) DO UPDATE SET balance=excluded.balance")) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, balance);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to persist account: " + e.getMessage());
        }
    }

    private void saveAccountSync(UUID uuid, long balance) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("INSERT INTO coins_accounts(uuid, balance) VALUES(?,?) " +
                     "ON CONFLICT(uuid) DO UPDATE SET balance=excluded.balance")) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, balance);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to persist account: " + e.getMessage());
        }
    }

    private void logTransaction(UUID uuid, long amount, String reason) {
        if (!transactionLog) return;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("INSERT INTO coins_transactions(uuid, amount, reason, created_at) VALUES(?,?,?,?)")) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, amount);
            ps.setString(3, reason);
            ps.setLong(4, Instant.now().toEpochMilli());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to log transaction: " + e.getMessage());
        }
    }
}
