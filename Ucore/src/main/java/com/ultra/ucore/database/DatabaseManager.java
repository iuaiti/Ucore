package com.ultra.ucore.database;

import com.ultra.ucore.config.ConfigManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Manages database connections and async execution.
 */
public final class DatabaseManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final ExecutorService executorService;

    private HikariDataSource dataSource;

    public DatabaseManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.executorService = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors() / 2)
        );
    }

    public void initialize() {
        String type = configManager.get("config.yml").getString("database.type", "sqlite");

        if ("sqlite".equalsIgnoreCase(type)) {
            initializeSqlite();
        } else {
            initializeMysql();
        }
    }

    private void initializeSqlite() {
        File file = new File(plugin.getDataFolder(),
                configManager.get("config.yml").getString("database.sqlite-file", "database.db"));

        HikariConfig config = new HikariConfig();
        config.setPoolName("UCore-SQLite");
        config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
        config.setMaximumPoolSize(1);
        config.setConnectionTestQuery("SELECT 1");

        this.dataSource = new HikariDataSource(config);
    }

    private void initializeMysql() {
        var cfg = configManager.get("config.yml");

        HikariConfig config = new HikariConfig();
        config.setPoolName("UCore-MySQL");
        config.setJdbcUrl("jdbc:mysql://"
                + cfg.getString("database.mysql.host")
                + ":"
                + cfg.getInt("database.mysql.port")
                + "/"
                + cfg.getString("database.mysql.database")
                + "?useSSL=false&allowPublicKeyRetrieval=true");
        config.setUsername(cfg.getString("database.mysql.username"));
        config.setPassword(cfg.getString("database.mysql.password"));
        config.setMaximumPoolSize(cfg.getInt("database.mysql.pool-size", 10));

        this.dataSource = new HikariDataSource(config);
    }

    public <T> CompletableFuture<T> executeAsync(Function<Connection, T> function) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                return function.apply(connection);
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        }, executorService);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void shutdown() {
        executorService.shutdown();

        if (dataSource != null) {
            dataSource.close();
        }
    }
}
