package io.github.md5sha256.chestshopFinder.database;

import io.github.md5sha256.chestshopFinder.model.Shop;
import io.github.md5sha256.chestshopFinder.model.ShopType;
import io.github.md5sha256.chestshopFinder.model.HydratedShop;
import io.github.md5sha256.chestshopFinder.util.BlockPosition;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DatabaseInterface {

    void initializeDatabase(@Nonnull Connection connection) throws SQLException;

    @Nonnull
    Map<String, Integer> knownItemCodes(@Nonnull Connection connection) throws SQLException;

    void registerShops(@Nonnull Connection connection, @Nonnull List<HydratedShop> shops) throws SQLException;

    void updateShops(@Nonnull Connection connection, @Nonnull List<HydratedShop> shops) throws SQLException;

    void deleteShops(@Nonnull Connection connection, @Nonnull List<BlockPosition> positions) throws SQLException;

    void deleteShop(@Nonnull Connection connection, @Nonnull UUID world, int posX, int posY, int posZ)
            throws SQLException;

    @Nonnull
    List<Shop> getShopsWithItemInWorld(
            @Nonnull Connection connection,
            @Nonnull UUID world,
            @Nonnull ShopType shopType,
            int itemId
    ) throws SQLException;

    @Nonnull
    List<Shop> getShopsWithItem(
            @Nonnull Connection connection,
            @Nonnull UUID world,
            @Nonnull ShopType shopType,
            int itemId
    ) throws SQLException;

}
