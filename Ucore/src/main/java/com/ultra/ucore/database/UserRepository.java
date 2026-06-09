package com.ultra.ucore.database;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Example user repository contract.
 */
public interface UserRepository extends Repository {

    CompletableFuture<Void> createTables();

    CompletableFuture<Void> saveUser(UUID uuid, String name);

    CompletableFuture<Optional<String>> findName(UUID uuid);
}
