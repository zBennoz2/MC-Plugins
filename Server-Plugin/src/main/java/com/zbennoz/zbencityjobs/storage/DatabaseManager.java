package com.zbennoz.zbencityjobs.storage;

import com.zbennoz.zbencityjobs.ZBenCityJobs;
import org.sqlite.SQLiteConfig;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {
    private final ZBenCityJobs plugin;
    private final DataSource dataSource;

    public DatabaseManager(ZBenCityJobs plugin) {
        this.plugin = plugin;
        this.dataSource = createDataSource();
    }

    private DataSource createDataSource() {
        File dbFile = new File(plugin.getDataFolder(), plugin.getConfig().getString("storage.database", "cityjobs.db"));
        if (!dbFile.getParentFile().exists()) {
            dbFile.getParentFile().mkdirs();
        }
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        if (plugin.getConfig().getBoolean("storage.use-wal", true)) {
            config.setPragma(SQLiteConfig.Pragma.JOURNAL_MODE, "WAL");
        }
        config.setPragma(SQLiteConfig.Pragma.SYNCHRONOUS, "NORMAL");
        config.setBusyTimeout(3000);
        org.sqlite.SQLiteDataSource source = new org.sqlite.SQLiteDataSource(config);
        source.setUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        return source;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        // SQLiteDataSource does not need explicit close
    }
}
