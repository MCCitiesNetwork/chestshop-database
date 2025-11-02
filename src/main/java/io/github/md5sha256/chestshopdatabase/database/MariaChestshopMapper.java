package io.github.md5sha256.chestshopdatabase.database;

import io.github.md5sha256.chestshopdatabase.model.Shop;
import io.github.md5sha256.chestshopdatabase.model.ShopType;
import io.github.md5sha256.chestshopdatabase.util.BlockPosition;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mapper
public interface MariaChestshopMapper extends DatabaseMapper {

    @Override
    @Delete("""
            DELETE
            FROM Item
            WHERE NOT EXISTS (SELECT 1
                              FROM Shop
                              WHERE Shop.item_code = Item.item_code)
            """)
    void deleteOrphanedItems();

    @Override
    @Select("""  
            INSERT INTO Item (item_code, item_bytes)
            VALUES (#{item_code}, #{item_bytes})
            ON DUPLICATE KEY UPDATE item_bytes = VALUES(item_bytes);
            """)
    void insertItem(@Param("item_code") String itemCode, @Param("item_bytes") byte[] itemBytes);

    @Override
    @Select("SELECT item_code FROM Item")
    @Nonnull
    List<String> selectItemCodes();

    @Override
    @Insert("""
            INSERT INTO Shop (world_uuid,
                              pos_x,
                              pos_y,
                              pos_z,
                              item_code,
                              owner_name,
                              buy_price,
                              sell_price,
                              quantity,
                              stock,
                              estimated_capacity)
            VALUES (CAST(#{world_uuid} AS UUID),
                    #{x},
                    #{y},
                    #{z},
                    #{item_code},
                    #{owner_name},
                    #{buy_price},
                    #{sell_price},
                    #{quantity},
                    #{stock},
                    #{capacity})
            ON DUPLICATE KEY UPDATE item_code          = VALUES(item_code),
                                    owner_name         = VALUES(owner_name),
                                    buy_price          = VALUES(buy_price),
                                    sell_price         = VALUES(sell_price),
                                    quantity           = VALUES(quantity),
                                    stock              = VALUES(stock),
                                    estimated_capacity = VALUES(estimated_capacity)
            """)
    void insertShop(
            @Param("world_uuid") @Nonnull UUID worldUUID,
            @Param("x") int x,
            @Param("y") int y,
            @Param("z") int z,
            @Param("item_code") @Nonnull String itemCode,
            @Param("owner_name") @Nonnull String ownerName,
            @Param("buy_price") @Nullable Double buyPrice,
            @Param("sell_price") @Nullable Double sellPrice,
            @Param("quantity") int quantity,
            @Param("stock") int stock,
            @Param("capacity") int estimatedCapacity);

    @Override
    @Delete("""
            DELETE FROM Shop
            WHERE world_uuid = CAST(#{world_uuid} AS UUID) AND pos_x = #{x} AND pos_y = #{y} AND pos_z = #{z}
            """)
    void deleteShopByPos(
            @Param("world_uuid") @Nonnull UUID world,
            @Param("x") int x,
            @Param("y") int y,
            @Param("z") int z);

    @Override
    @SelectProvider(type = MariaDatabaseUtil.class, method = "selectShopsByShopTypeWorldItem")
    @Nonnull
    List<Shop> selectShopsByShopTypeWorldItem(@Nonnull Set<ShopType> shopTypes,
                                              @Param("world_uuid") @Nullable UUID world,
                                              @Param("item_code") @Nullable String itemCode);

    @Override
    @Select("""
            SELECT
                CAST(world_uuid AS BINARY(16)) as world
                pos_x AS posX,
                pos_y AS posY,
                pos_z AS posZ
            WHERE
                world_uuid = CAST(#{world_uuid} AS UUID)
            """)
    @Nonnull
    List<BlockPosition> selectShopsPositionsByWorld(@Nonnull @Param("world_uuid") UUID world);
}
