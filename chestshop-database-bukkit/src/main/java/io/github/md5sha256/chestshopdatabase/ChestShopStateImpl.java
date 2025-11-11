package io.github.md5sha256.chestshopdatabase;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.md5sha256.chestshopdatabase.database.ChestshopMapper;
import io.github.md5sha256.chestshopdatabase.model.HydratedShop;
import io.github.md5sha256.chestshopdatabase.model.ShopStockUpdate;
import io.github.md5sha256.chestshopdatabase.util.BlockPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class ChestShopStateImpl implements ChestShopState {

    private final Cache<BlockPosition, Boolean> shopCache;

    private final Map<BlockPosition, HydratedShop> createdShops = new HashMap<>();
    private final Set<ShopStockUpdate> updatedShops = new HashSet<>();
    private final Set<BlockPosition> deletedShops = new HashSet<>();
    private final Set<String> knownItemCodes = new HashSet<>();
    private final AtomicReference<CompletableFuture<Void>> nextTask
            = new AtomicReference<>(new CompletableFuture<>());

    public ChestShopStateImpl(@NotNull Duration shopCacheDuration) {
        this.shopCache = CacheBuilder.newBuilder()
                .expireAfterAccess(shopCacheDuration)
                .build();
    }


    public void cacheItemCodes(@NotNull Logger logger, @NotNull ChestshopMapper database) {
        try {
            this.knownItemCodes.addAll(database.selectItemCodes());
        } catch (Exception ex) {
            logger.warning("Failed to cache item codes: " + ex.getMessage());
        }
    }

    @Override
    public @NotNull Set<String> itemCodes() {
        return Collections.unmodifiableSet(this.knownItemCodes);
    }


    public @Nullable Consumer<ChestshopMapper> flushTask() {
        List<HydratedShop> created = List.copyOf(this.createdShops.values());
        List<HydratedShop> toInsert = new ArrayList<>(created.size());
        toInsert.addAll(created);
        List<ShopStockUpdate> toUpdate = List.copyOf(this.updatedShops);
        List<BlockPosition> deleted = List.copyOf(this.deletedShops);
        if (deleted.isEmpty() && toInsert.isEmpty() && toUpdate.isEmpty()) {
            return null;
        }
        this.createdShops.clear();
        this.updatedShops.clear();
        this.deletedShops.clear();
        return (database) -> {
            try {
                deleted.forEach(database::deleteShopByPos);
                database.insertShops(toInsert);
                database.updateShops(toUpdate);
                database.flushSession();
            } finally {
                markCompleteAndReset();
            }
        };
    }

    @Override
    public boolean cachedShopRegistered(@NotNull BlockPosition position) {
        return Objects.requireNonNullElse(this.shopCache.getIfPresent(position), Boolean.FALSE);
    }

    @Override
    public @NotNull CompletableFuture<Void> queueShopCreation(@NotNull HydratedShop shop) {
        BlockPosition position = shop.blockPosition();
        this.deletedShops.remove(position);
        this.createdShops.put(position, shop);
        this.knownItemCodes.add(shop.item().itemCode());
        this.shopCache.put(position, Boolean.TRUE);
        return this.nextTask.get();
    }

    @Override
    public @NotNull CompletableFuture<Void> queueShopUpdate(@NotNull ShopStockUpdate shop) {
        this.updatedShops.add(shop);
        return this.nextTask.get();
    }

    @Override
    public @NotNull CompletableFuture<Void> queueShopDeletion(@NotNull BlockPosition position) {
        this.createdShops.remove(position);
        this.deletedShops.add(position);
        this.shopCache.invalidate(position);
        return this.nextTask.get();
    }

    private void markCompleteAndReset() {
        this.nextTask.getAndUpdate(future -> {
            future.complete(null);
            return new CompletableFuture<>();
        });
    }


}
