package io.github.md5sha256.chestshopFinder.model;

import io.github.md5sha256.chestshopFinder.util.BlockPosition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public record HydratedShop(
        @Nonnull UUID worldId,
        int posX,
        int posY,
        int posZ,
        @Nonnull ChestshopItem item,
        @Nonnull String ownerName,
        @Nullable Double buyPrice,
        @Nullable Double sellPrice,
        int quantity
) {

    public HydratedShop {
        if (buyPrice == null && sellPrice == null) {
            throw new IllegalArgumentException("Shop cannot have both buyPrice and sellPrice be null!");
        }
    }

    @Nonnull
    public BlockPosition blockPosition() {
        return new BlockPosition(this.worldId, this.posX, this.posY, this.posZ);
    }
}
