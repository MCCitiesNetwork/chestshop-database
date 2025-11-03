package io.github.md5sha256.chestshopdatabase.model;

import io.github.md5sha256.chestshopdatabase.util.BlockPosition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public record Shop(
        @Nonnull UUID worldId,
        int posX,
        int posY,
        int posZ,
        @Nonnull String itemCode,
        @Nonnull String ownerName,
        @Nullable Double buyPrice,
        @Nullable Double sellPrice,
        int quantity,
        int stock,
        int estimatedCapacity
) {

    public Shop {
        if (buyPrice == null && sellPrice == null) {
            throw new IllegalArgumentException("Shop cannot have both buyPrice and sellPrice be null!");
        }
    }

    @Nonnull
    public BlockPosition blockPosition() {
        return new BlockPosition(this.worldId, this.posX, this.posY, this.posZ);
    }

    public ShopType shopType() {
        if (this.buyPrice != null && this.sellPrice != null) {
            return ShopType.BOTH;
        } else if (this.sellPrice != null) {
            return ShopType.SELL;
        }
        return ShopType.BUY;
    }

    public Double unitBuyPrice() {
        if (buyPrice() == null) return null;
        return buyPrice() / quantity();
    }

    public Double unitSellPrice () {
        if (sellPrice() == null) return null;
        return sellPrice() / quantity();
    }
}
