package io.github.md5sha256.chestshopFinder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public record Shop(
        @Nonnull UUID worldId,
        int posX,
        int posY,
        int posZ,
        int itemId,
        @Nonnull String ownerName,
        @Nullable Double buyPrice,
        @Nullable Double sellPrice,
        int quantity,
        long lastUpdatedEpochSeconds
) {

    public Shop {
        if (buyPrice == null && sellPrice == null) {
            throw new IllegalArgumentException("Shop cannot have both buyPrice and sellPrice be null!");
        }
    }
}
