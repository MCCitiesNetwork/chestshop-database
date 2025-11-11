package io.github.md5sha256.chestshopdatabase.database;

import io.github.md5sha256.chestshopdatabase.model.PartialHydratedShop;
import io.github.md5sha256.chestshopdatabase.model.Shop;
import io.github.md5sha256.chestshopdatabase.model.ShopType;
import io.github.md5sha256.chestshopdatabase.util.BlockPosition;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Flush;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mapper
public interface MariaChestshopMapper extends ChestshopMapper {

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
    @NotNull
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
            @Param("world_uuid") @NotNull UUID worldUUID,
            @Param("x") int x,
            @Param("y") int y,
            @Param("z") int z,
            @Param("item_code") @NotNull String itemCode,
            @Param("owner_name") @NotNull String ownerName,
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
            @Param("world_uuid") @NotNull UUID world,
            @Param("x") int x,
            @Param("y") int y,
            @Param("z") int z);

    @Override
    @SelectProvider(type = MariaDatabaseUtil.class, method = "selectShopsByShopTypeWorldItem")
    @NotNull
    List<Shop> selectShopsByShopTypeWorldItem(@NotNull Set<ShopType> shopTypes,
                                              @Param("world_uuid") @Nullable UUID world,
                                              @Param("item_code") @Nullable String itemCode);

    @Override
    @SelectProvider(value = MariaDatabaseUtil.class, method = "selectShopsPositionsByWorld")
    @NotNull
    List<BlockPosition> selectShopsPositionsByWorld(@Nullable @Param("world_uuid") UUID world);

    @Override
    @Select("""
            SELECT CAST(world_uuid AS BINARY(16))      AS worldID,
                   pos_x                               AS posX,
                   pos_y                               AS posY,
                   pos_z                               AS posZ,
                   Shop.item_code                      AS itemCode,
                   Item.item_bytes                     AS item_bytes,
                   owner_name                          AS ownerName,
                   buy_price                           AS buyPrice,
                   sell_price                          AS sellPrice,
                   quantity,
                   stock,
                   estimated_capacity                  AS estimatedCapacity
            FROM Shop
                     INNER JOIN Item ON Shop.item_code = Item.item_code
            WHERE Shop.world_uuid = CAST(#{world_uuid} AS UUID)
              AND pos_x >> 4 = #{chunk_x}
              AND pos_z >> 4 = #{chunk_z}
            ;
            """)
    @NotNull
    List<PartialHydratedShop> selectShopsInChunk(@NotNull @Param("world_uuid") UUID world,
                                                 @Param("chunk_x") int chunkX,
                                                 @Param("chunk_z") int chunkZ);

    @Override
    @Update("""
            UPDATE Shop
            SET
                stock = #{stock},
                estimated_capacity = #{estimated_capacity}
            WHERE world_uuid = CAST(#{world_uuid} AS UUID) AND pos_x = #{x} AND pos_y = #{y} AND pos_z = #{z}
            """)
    void updateShop(
            @Param("world_uuid") @NotNull UUID world,
            @Param("x") int x,
            @Param("y") int y,
            @Param("z") int z,
            @Param("stock") int stock,
            @Param("estimated_capacity") int estimatedCapacity
    );

    @Override
    @Flush
    void flushSession();
}
