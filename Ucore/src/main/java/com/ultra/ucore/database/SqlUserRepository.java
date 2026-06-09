package com.ultra.ucore.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * SQL implementation of the user repository.
 */
public final class SqlUserRepository implements UserRepository {

    private final DatabaseManager databaseManager;

    public SqlUserRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public CompletableFuture<Void> createTables() {
        return databaseManager.executeAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS ucore_users (
                        uuid VARCHAR(36) PRIMARY KEY,
                        name VARCHAR(16) NOT NULL
                    )
                    """)) {
                statement.executeUpdate();
                return null;
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Override
    public CompletableFuture<Void> saveUser(UUID uuid, String name) {
        return databaseManager.executeAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO ucore_users(uuid, name)
                    VALUES(?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET name = excluded.name
                    """)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, name);
                statement.executeUpdate();
                return null;
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Override
    public CompletableFuture<Optional<String>> findName(UUID uuid) {
        return databaseManager.executeAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT name FROM ucore_users WHERE uuid = ?
                    """)) {
                statement.setString(1, uuid.toString());

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(resultSet.getString("name"));
                    }

                    return Optional.empty();
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }
}
