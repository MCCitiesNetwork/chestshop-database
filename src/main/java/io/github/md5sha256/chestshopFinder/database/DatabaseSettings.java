package io.github.md5sha256.chestshopFinder.database;

import javax.annotation.Nonnull;

public record DatabaseSettings(@Nonnull String url, @Nonnull String username, @Nonnull String password) {
}
