package io.github.md5sha256.chestshopFinder.database;

import io.github.md5sha256.chestshopFinder.model.ChestshopItem;
import io.github.md5sha256.chestshopFinder.model.HydratedShop;
import io.github.md5sha256.chestshopFinder.model.Shop;
import io.github.md5sha256.chestshopFinder.model.ShopType;
import io.github.md5sha256.chestshopFinder.util.BlockPosition;
import org.apache.ibatis.annotations.Flush;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DatabaseInterface {

    void deleteOrphanedItems();

    void insertItem(String itemCode, byte[] itemBytes);

    default void insertItems(@Nonnull List<ChestshopItem> items) {
        Map<String, byte[]> itemBytes = new HashMap<>();
        for (ChestshopItem item : items) {
            itemBytes.computeIfAbsent(item.itemCode(),
                    unused -> item.itemStack().serializeAsBytes());
        }
        for (Map.Entry<String, byte[]> entry : itemBytes.entrySet()) {
            insertItem(entry.getKey(), entry.getValue());
        }
    }


    void insertShop(
            @Nonnull UUID worldUUID,
            int x,
            int y,
            int z,
            @Nonnull String itemCode,
            @Nonnull String ownerName,
            @Nullable Double buyPrice,
            @Nullable Double sellPrice,
            int quantity);

    default void insertShop(@Nonnull Shop shop) {
        insertShop(shop.worldId(),
                shop.posX(),
                shop.posY(),
                shop.posZ(),
                shop.itemCode(),
                shop.ownerName(),
                shop.buyPrice(),
                shop.sellPrice(),
                shop.quantity());
    }


    default void insertShop(@Nonnull HydratedShop shop) {
        insertShop(shop.worldId(),
                shop.posX(),
                shop.posY(),
                shop.posZ(),
                shop.item().itemCode(),
                shop.ownerName(),
                shop.buyPrice(),
                shop.sellPrice(),
                shop.quantity());
    }

    default void insertShops(@Nonnull List<HydratedShop> shops) {
        List<ChestshopItem> items = shops.stream().map(HydratedShop::item).toList();
        // make sure items exist before inserting shops...
        insertItems(items);
        shops.forEach(this::insertShop);
    }

    void deleteShopByPos(@Nonnull UUID world, int x, int y, int z);

    default void deleteShopByPos(@Nonnull BlockPosition position) {
        deleteShopByPos(position.world(), position.x(), position.y(), position.z());
    }

    @Nonnull
    List<Shop> selectShopsByItem(@Nonnull ShopType shopType, @Nonnull String itemCode);

    @Nonnull
    List<Shop> selectShopsByWorldAndItem(@Nonnull ShopType shopType,
                                         @Nonnull UUID world,
                                         @Nonnull String itemCode);

    @Nonnull
    List<Shop> selectShopsByWorldItemDistance(
            @Nonnull ShopType shopType,
            @Nonnull UUID world,
            @Nonnull String itemCode,
            int x,
            int y,
            int z,
            double maxDistanceSquared
    );

    @Flush
    void flushSession();
}
