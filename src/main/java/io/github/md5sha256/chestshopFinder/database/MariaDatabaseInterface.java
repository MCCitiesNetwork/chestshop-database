package io.github.md5sha256.chestshopFinder.database;

import io.github.md5sha256.chestshopFinder.model.Shop;
import io.github.md5sha256.chestshopFinder.model.ShopType;
import io.github.md5sha256.chestshopFinder.model.ChestshopItem;
import io.github.md5sha256.chestshopFinder.model.HydratedShop;
import io.github.md5sha256.chestshopFinder.util.BlockPosition;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MariaDatabaseInterface implements DatabaseInterface {

    private static final String CREATE_ITEM = """
            CREATE TABLE Item (
                item_code VARCHAR(255) PRIMARY KEY,
                item_bytes BLOB NOT NULL,
                INDEX `idx_item_item_code` (item_code)
            );
            """;

    private static final String CREATE_SHOP = """
            CREATE TABLE Shop (
                world_uuid UUID NOT NULL,
                pos_x INT NOT NULL,
                pos_y INT NOT NULL,
                pos_z INT NOT NULL,
                item_code INT NOT NULL,
                owner_name VARCHAR(255) NOT NULL,
                buy_price DECIMAL(10, 2),
                sell_price DECIMAL(10, 2),
                quantity INT NOT NULL,
                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY(world_uuid, pos_x, pos_y, pos_z),
                INDEX `idx_shop_item_code` (item_code)
            );
            """;

    private static final String CREATE_SHOP_CONSTRAINTS = """
            ALTER TABLE Shop
            ADD CONSTRAINT chk_price_not_null
                CHECK (buy_price IS NOT NULL OR sell_price IS NOT NULL)
            ADD CONSTRAINT chk_quantity_greater_than_zero
                CHECK (quantity > 0),
            ADD CONSTRAINT fk_shop_item_item_code
                FOREIGN KEY (item_code) REFERENCES Item(item_code) ON DELETE CASCADE
            ;
            """;


    private static final String DELETE_ORPHANED_ITEMS = """
            DELETE FROM Item
            WHERE
                SELECT NOT EXISTS (
                    SELECT 1 FROM Shop WHERE Shop.item_code = Item.item_code
                )
            """;

    private static final String CREATE_SHOP_UPDATE_LAST_UPDATED_ITEM_TRIGGER = """
            CREATE TRIGGER trigger_shop_update_timestamp
            BEFORE UPDATE ON Shop
            FOR EACH ROW
            BEGIN
                SELECT NEW.last_updated = CURRENT_TIMESTAMP
            END;
            """;

    private static final String SELECT_ITEM_AND_CODES_COUNT = "SELECT COUNT(*) FROM Item";

    private static final String SELECT_item_code_AND_CODES = """
            SELECT item_code, item_code FROM Item;
            """;

    private static final String INSERT_ITEM = """
            @item_code = ?;
            @item_bytes = ?;
            
            INSERT INTO Item (item_code, item_bytes)
            VALUES (@item_code, @item_bytes)
            ON DUPLICATE KEY UPDATE item_bytes = @item_bytes
            """;

    private static final String INSERT_SHOP = """
            @world_uuid = ?;
            @x = ?;
            @y = ?;
            @z = ?;
            @item_code = ?;
            @owner_name = ?;
            @buy_price = ?;
            @sell_price = ?;
            @quantity = ?;
            INSERT INTO Shop (
                    world_uuid,
                    pos_x,
                    pos_y,
                    pos_z,
                    item_code,
                    owner_name,
                    buy_price,
                    sell_price,
                    quantity
            ) VALUES (@world_uuid, @x, @y, @z, @item_code, @owner_name, @buy_price, @sell_price, @quantity)
            ON DUPLICATE KEY UPDATE
                item_code = @item_code
                owner_name = @owner_name
                buy_price = @buy_price
                sell_price = @sell_price
                quantity = @quantity
            """;

    private static final String DELETE_SHOP_POS = """
            DELETE FROM Shop
            WHERE world_uuid = ? AND pos_x = ? AND pos_y = ? AND pos_z = ?
            """;

    private static final String SELECT_ANY_SHOP_BY_ITEM = """
            SELECT
                pos_x,
                pos_y,
                pos_z,
                owner_name,
                buy_price,
                sell_price,
                quantity,
                last_updated
            FROM Shop
            WHERE
                item_code = ?
            """;

    private static final String SELECT_SELL_SHOP_BY_ITEM = """
            SELECT
                pos_x,
                pos_y,
                pos_z,
                owner_name,
                sell_price,
                quantity,
            FROM Shop
            WHERE
                sell_price IS NOT NULL AND item_code = ?
            """;

    private static final String SELECT_BUY_SHOP_BY_ITEM = """
            SELECT
                pos_x,
                pos_y,
                pos_z,
                owner_name,
                buy_price,
                quantity,
            FROM Shop
            WHERE
                buy_price IS NOT NULL AND AND item_code = ?
            """;

    private static final String SELECT_ANY_SHOP_BY_WORLD_AND_ITEM = """
            SELECT
                pos_x,
                pos_y,
                pos_z,
                owner_name,
                buy_price,
                sell_price,
                quantity,
            FROM Shop
            WHERE
                world_uuid = ? AND item_code = ?
            LIMIT 1;
            """;

    private static final String SELECT_SELL_SHOP_BY_WORLD_AND_ITEM = """
            SELECT
                pos_x,
                pos_y,
                pos_z,
                owner_name,
                sell_price,
                quantity,
            FROM Shop
            WHERE
                sell_price IS NOT NULL AND world_uuid = ? AND item_code = ?
            """;

    private static final String SELECT_BUY_SHOP_BY_WORLD_AND_ITEM = """
            SELECT
                pos_x,
                pos_y,
                pos_z,
                owner_name,
                buy_price,
                quantity,
            FROM Shop
            WHERE
                buy_price IS NOT NULL AND world_uuid = ? AND item_code = ?
            """;

    @Override
    public void initializeDatabase(@Nonnull Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_ITEM);
            statement.executeUpdate(CREATE_SHOP);
        }
    }

    @Override
    public void updateShops(@NotNull Connection connection,
                            @NotNull List<HydratedShop> shops) throws SQLException {

    }

    @Override
    public void deleteShops(@NotNull Connection connection,
                            @NotNull List<BlockPosition> positions) throws SQLException {

    }

    @Override
    @Nonnull
    public Map<String, Integer> knownItemCodes(@Nonnull Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet countResult = statement.executeQuery(SELECT_ITEM_AND_CODES_COUNT);) {
            if (!countResult.next()) {
                return Collections.emptyMap();
            }
            int size = countResult.getInt(1);
            if (size == 0) {
                return Collections.emptyMap();
            }
            Map<String, Integer> codes = HashMap.newHashMap(size);
            try (ResultSet resultSet = statement.executeQuery(SELECT_item_code_AND_CODES)) {
                while (resultSet.next()) {
                    int itemId = resultSet.getInt(1);
                    String code = resultSet.getString(2);
                    codes.put(code, itemId);
                }
            }
            return codes;
        }
    }

    private void ensureItemsExist(@Nonnull Connection connection, @Nonnull List<ChestshopItem> items) throws SQLException{
        Map<String, byte[]> itemBytes = new HashMap<>();
        for (ChestshopItem item : items) {
            itemBytes.computeIfAbsent(item.itemCode(), unused -> item.itemStack().serializeAsBytes());
        }
        try (PreparedStatement statement = connection.prepareStatement(INSERT_ITEM)) {
            for (Map.Entry<String, byte[]> entry : itemBytes.entrySet()) {
                statement.setString(1, entry.getKey());
                statement.setBytes(2, entry.getValue());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    @Override
    public void registerShops(@Nonnull Connection connection,
                              @Nonnull List<HydratedShop> shops) throws SQLException {
        List<ChestshopItem> items = shops.stream().map(HydratedShop::item).toList();
        ensureItemsExist(connection, items);
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SHOP)) {
            for (HydratedShop shop : shops) {
                statement.setObject(1, shop.worldId());
                statement.setInt(2, shop.posX());
                statement.setInt(3, shop.posY());
                statement.setInt(4, shop.posZ());
                statement.setString(5, shop.item().itemCode());
                statement.setString(6, shop.ownerName());
                if (shop.buyPrice() == null) {
                    statement.setNull(7, Types.DOUBLE);
                } else {
                    statement.setDouble(7, shop.buyPrice());
                }
                if (shop.sellPrice() == null) {
                    statement.setNull(8, Types.DOUBLE);
                } else {
                    statement.setDouble(8, shop.sellPrice());
                }
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    @Override
    public void deleteShop(@Nonnull Connection connection,
                           @Nonnull UUID world,
                           int posX,
                           int posY,
                           int posZ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_SHOP_POS)) {
            statement.setObject(1, world);
            statement.setInt(2, posX);
            statement.setInt(3, posY);
            statement.setInt(4, posZ);
            statement.executeUpdate();
        }
    }

    @Override
    @Nonnull
    public List<Shop> getShopsWithItemInWorld(@Nonnull Connection connection,
                                              @Nonnull UUID world,
                                              @Nonnull ShopType shopType,
                                              int itemId) throws SQLException {
        return List.of();
    }

    @Override
    public @NotNull List<Shop> getShopsWithItem(@NotNull Connection connection,
                                                @NotNull UUID world,
                                                @NotNull ShopType shopType,
                                                int itemId) throws SQLException {
        return List.of();
    }

    private Optional<Shop> getAnyShop(
            @Nonnull Connection connection,
            @Nonnull UUID world,
            @Nonnull String itemCode) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_ANY_SHOP_BY_WORLD_AND_ITEM)) {
            statement.setObject(1, world);
            statement.setString(2, itemCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int posX = resultSet.getInt(1);
                    int posY = resultSet.getInt(2);
                    int posZ = resultSet.getInt(3);
                    String ownerName = resultSet.getString(4);
                    Double buyPrice = resultSet.getObject(5, Double.class);
                    Double sellPrice = resultSet.getObject(6, Double.class);
                    int quantity = resultSet.getInt(7);
                    Shop shop = new Shop(world, posX, posY, posZ, itemCode, ownerName, buyPrice, sellPrice, quantity);
                    return Optional.of(shop);
                }
            }
        }
        return Optional.empty();
    }
}
