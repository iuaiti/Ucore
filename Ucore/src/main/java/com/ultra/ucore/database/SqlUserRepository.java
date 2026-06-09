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
            String uuidString = uuid.toString();

            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE ucore_users SET name = ? WHERE uuid = ?
                    """)) {
                update.setString(1, name);
                update.setString(2, uuidString);

                if (update.executeUpdate() > 0) {
                    return null;
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }

            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO ucore_users(uuid, name) VALUES(?, ?)
                    """)) {
                insert.setString(1, uuidString);
                insert.setString(2, name);
                insert.executeUpdate();
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
