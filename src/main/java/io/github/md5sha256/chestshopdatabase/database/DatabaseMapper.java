package io.github.md5sha256.chestshopdatabase.database;

import io.github.md5sha256.chestshopdatabase.model.ChestshopItem;
import io.github.md5sha256.chestshopdatabase.model.HydratedShop;
import io.github.md5sha256.chestshopdatabase.model.Shop;
import io.github.md5sha256.chestshopdatabase.model.ShopType;
import io.github.md5sha256.chestshopdatabase.util.BlockPosition;
import org.apache.ibatis.annotations.Flush;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface DatabaseMapper {

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
        flushSession();
    }

    @Nonnull
    List<String> selectItemCodes();


    void insertShop(
            @Nonnull UUID worldUUID,
            int x,
            int y,
            int z,
            @Nonnull String itemCode,
            @Nonnull String ownerName,
            @Nullable Double buyPrice,
            @Nullable Double sellPrice,
            int quantity,
            int stock,
            int estimatedCapacity);

    default void insertShop(@Nonnull Shop shop) {
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


    default void insertShop(@Nonnull HydratedShop shop) {
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

    default void insertShops(@Nonnull List<HydratedShop> shops) {
        List<ChestshopItem> items = shops.stream().map(HydratedShop::item).toList();
        // make sure items exist before inserting shops...
        insertItems(items);
        shops.forEach(this::insertShop);
        flushSession();
    }

    void deleteShopByPos(@Nonnull UUID world, int x, int y, int z);

    default void deleteShopByPos(@Nonnull BlockPosition position) {
        deleteShopByPos(position.world(), position.x(), position.y(), position.z());
    }

    @Nonnull
    List<Shop> selectShopsByShopTypeWorldItem(@Nonnull Set<ShopType> shopTypes,
                                              @Nullable UUID world,
                                              @Nullable String itemCode);

    @Nonnull
    List<BlockPosition> selectShopsPositionsByWorld(@Nonnull UUID world);

    @Flush
    void flushSession();
}
