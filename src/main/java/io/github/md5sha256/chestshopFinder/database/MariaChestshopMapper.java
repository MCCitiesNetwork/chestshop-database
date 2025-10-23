package io.github.md5sha256.chestshopFinder.database;

import io.github.md5sha256.chestshopFinder.model.Shop;
import io.github.md5sha256.chestshopFinder.model.ShopType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

@Mapper
public interface MariaChestshopMapper extends DatabaseInterface {

    @Delete("""
            DELETE
            FROM Item
            WHERE NOT EXISTS (SELECT 1
                              FROM Shop
                              WHERE Shop.item_code = Item.item_code)
            """)
    @Override
    void deleteOrphanedItems();

    @Select("""
            @item_code = #{item_code};
            @item_bytes = #{item_bytes};
            
            INSERT INTO Item (item_code, item_bytes)
            VALUES (@item_code, @item_bytes)
            ON DUPLICATE KEY UPDATE item_bytes = @item_bytes
            """)
    @Override
    void insertItem(@Param("item_code") String itemCode, @Param("item_bytes") byte[] itemBytes);

    @Insert("""
            @world_uuid = #{world_uuid};
            @x = #{x};
            @y = #{y};
            @z = #{z};
            @item_code = #{item_code};
            @owner_name = #{owner_name};
            @buy_price = #{buy_price};
            @sell_price = #{sell_price};
            @quantity = #{quantity};
            INSERT INTO Shop (world_uuid,
                              pos_x,
                              pos_y,
                              pos_z,
                              item_code,
                              owner_name,
                              buy_price,
                              sell_price,
                              quantity)
            VALUES (@world_uuid, @x, @y, @z, @item_code, @owner_name, @buy_price, @sell_price, @quantity)
            ON DUPLICATE KEY UPDATE item_code  = @item_code,
                                    owner_name = @owner_name,
                                    buy_price  = @buy_price,
                                    sell_price = @sell_price,
                                    quantity   = @quantity
            """)
    @Override
    void insertShop(
            @Param("world_uuid") @Nonnull UUID worldUUID,
            @Param("x") int x,
            @Param("y") int y,
            @Param("z") int z,
            @Param("item_code") @Nonnull String itemCode,
            @Param("owner_name") @Nonnull String ownerName,
            @Param("buy_price") @Nullable Double buyPrice,
            @Param("sell_price") @Nullable Double sellPrice,
            @Param("quantity") int quantity);

    @Override
    @Delete("""
            DELETE FROM Shop
            WHERE world_uuid = #{world_uuid} AND pos_x = #{x} AND pos_y = #{y} AND pos_z = #{z}
            """)
    void deleteShopByPos(
            @Param("world_uuid") @Nonnull UUID world,
            @Param("x") int x,
            @Param("y") int y,
            @Param("z") int z);

    @Override
    @SelectProvider(type = MariaDatabaseUtil.class, method = "selectShopsByItem")
    @Nonnull
    List<Shop> selectShopsByItem(@Nonnull ShopType shopType,
                                 @Param("item_code") @Nonnull String itemCode);

    @Override
    @SelectProvider(type = MariaDatabaseUtil.class, method = "selectShopsByWorldAndItem")
    @Nonnull
    List<Shop> selectShopsByWorldAndItem(@Nonnull ShopType shopType,
                                         @Param("world_uuid") @Nonnull UUID world,
                                         @Param("item_code") @Nonnull String itemCode);

    @Override
    @SelectProvider(type = MariaDatabaseUtil.class, method = "selectShopByWorldItemDistance")
    @Nonnull
    List<Shop> selectShopsByWorldItemDistance(
            @Nonnull ShopType shopType,
            @Param("world_uuid") @Nonnull UUID world,
            @Param("item_code") @Nonnull String itemCode,
            @Param("x") int x,
            @Param("y") int y,
            @Param("z") int z,
            @Param("distance") double maxDistance
    );
}
