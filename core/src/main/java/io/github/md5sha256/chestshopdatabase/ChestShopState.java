package io.github.md5sha256.chestshopdatabase;

import io.github.md5sha256.chestshopdatabase.model.HydratedShop;
import io.github.md5sha256.chestshopdatabase.model.ShopStockUpdate;
import io.github.md5sha256.chestshopdatabase.util.BlockPosition;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface ChestShopState {

    @NotNull Set<String> itemCodes();

    boolean cachedShopRegistered(@NotNull BlockPosition position);

    @NotNull CompletableFuture<Void> queueShopCreation(@NotNull HydratedShop shop);

    @NotNull CompletableFuture<Void> queueShopUpdate(@NotNull ShopStockUpdate shop);

    @NotNull CompletableFuture<Void> queueShopDeletion(@NotNull BlockPosition position);

    @NotNull String normalizeItemCode(@NotNull String itemCode);
}
