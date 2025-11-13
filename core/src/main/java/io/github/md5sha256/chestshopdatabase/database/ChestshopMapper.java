package io.github.md5sha256.chestshopdatabase.database;

import io.github.md5sha256.chestshopdatabase.model.ChestshopItem;
import io.github.md5sha256.chestshopdatabase.model.HydratedShop;
import io.github.md5sha256.chestshopdatabase.model.PartialHydratedShop;
import io.github.md5sha256.chestshopdatabase.model.Shop;
import io.github.md5sha256.chestshopdatabase.model.ShopStockUpdate;
import io.github.md5sha256.chestshopdatabase.model.ShopType;
import io.github.md5sha256.chestshopdatabase.util.BlockPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface ChestshopMapper {

    void deleteOrphanedItems();

    void insertItem(String itemCode, byte[] itemBytes);

    default void insertItems(@NotNull List<ChestshopItem> items) {
        Map<String, byte[]> itemBytes = new HashMap<>();
        for (ChestshopItem item : items) {
            itemBytes.computeIfAbsent(item.itemCode(),
                    unused -> item.itemStack().serializeAsBytes());
        }
        for (Map.Entry<String, byte[]> entry : itemBytes.entrySet()) {
            insertItem(entry.getKey(), entry.getValue());
        }
        flushSession();
    }

    @NotNull
    List<String> selectItemCodes();


    void insertShop(
            @NotNull UUID worldUUID,
            int x,
            int y,
            int z,
            @NotNull String itemCode,
            @NotNull String ownerName,
            @Nullable Double buyPrice,
            @Nullable Double sellPrice,
            int quantity,
            int stock,
            int estimatedCapacity);

    default void insertShop(@NotNull Shop shop) {
        insertShop(shop.worldId(),
                shop.posX(),
                shop.posY(),
                shop.posZ(),
                shop.itemCode(),
                shop.ownerName(),
                shop.buyPrice(),
                shop.sellPrice(),
                shop.quantity(),
                shop.stock(),
                shop.estimatedCapacity());
    }


    default void insertShop(@NotNull HydratedShop shop) {
        insertShop(shop.worldId(),
                shop.posX(),
                shop.posY(),
                shop.posZ(),
                shop.item().itemCode(),
                shop.ownerName(),
                shop.buyPrice(),
                shop.sellPrice(),
                shop.quantity(),
                shop.stock(),
                shop.estimatedCapacity());
    }

    default void insertShops(@NotNull List<HydratedShop> shops) {
        List<ChestshopItem> items = shops.stream().map(HydratedShop::item).toList();
        // make sure items exist before inserting shops...
        insertItems(items);
        shops.forEach(this::insertShop);
        flushSession();
    }

    void deleteShopByPos(@NotNull UUID world, int x, int y, int z);

    default void deleteShopByPos(@NotNull BlockPosition position) {
        deleteShopByPos(position.world(), position.x(), position.y(), position.z());
    }

    @NotNull
    List<Shop> selectShopsByShopTypeWorldItem(@NotNull Set<ShopType> shopTypes,
                                              @Nullable UUID world,
                                              @Nullable String itemCode,
                                              @Nullable Boolean visible);

    @NotNull
    List<BlockPosition> selectShopsPositionsByWorld(@Nullable UUID world,
                                                    @Nullable Boolean visible);

    @Nullable
    PartialHydratedShop selectShopByPosition(@NotNull UUID world, int x, int y, int z,
                                              @Nullable Boolean visible,
                                              @Nullable Boolean hologram);

    @NotNull
    List<PartialHydratedShop> selectShopsInChunk(@NotNull UUID world,
                                                 int chunkX,
                                                 int chunkZ,
                                                 @Nullable Boolean visible,
                                                 @Nullable Boolean hologram);

    void updateShop(@NotNull UUID world, int x, int y, int z, int stock, int estimatedCapacity);

    void updateShopVisibility(@NotNull UUID world, int x, int y, int z, boolean visible);

    void updateHologramVisibility(@NotNull UUID world, int x, int y, int z, boolean visible);

    default void updateShop(@NotNull ShopStockUpdate stockUpdate) {
        updateShop(stockUpdate.worldUUID(),
                stockUpdate.x(),
                stockUpdate.y(),
                stockUpdate.z(),
                stockUpdate.stock(),
                stockUpdate.estimatedCapacity());
    }

    default void updateShops(@NotNull List<ShopStockUpdate> stockUpdates) {
        stockUpdates.forEach(this::updateShop);
        flushSession();
    }

    void flushSession();
}
