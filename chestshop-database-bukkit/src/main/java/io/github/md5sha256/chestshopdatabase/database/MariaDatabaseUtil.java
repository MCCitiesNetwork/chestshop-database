package io.github.md5sha256.chestshopdatabase.database;

import io.github.md5sha256.chestshopdatabase.database.util.ConditionBuilder;
import io.github.md5sha256.chestshopdatabase.model.ShopType;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.jdbc.SQL;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public class MariaDatabaseUtil {

    private static String getShopConditions(@NotNull Set<ShopType> shopTypes) {
        if (shopTypes.isEmpty()) {
            return "FALSE";
        }
        boolean buyOnly = shopTypes.contains(ShopType.BUY);
        boolean sellOnly = shopTypes.contains(ShopType.SELL);
        boolean bothOnly = shopTypes.contains(ShopType.BOTH);
        return new ConditionBuilder()
                .applyIf(buyOnly,
                        cond -> cond.or(cond.newAnd("buy_price IS NOT NULL",
                                "sell_price IS NULL")))
                .applyIf(sellOnly,
                        cond -> cond.or(cond.newAnd("sell_price IS NOT NULL",
                                "buy_price IS NULL")))
                .applyIf(bothOnly,
                        cond -> cond.or(cond.newAnd("buy_price IS NOT NULL",
                                "sell_price IS NOT NULL")))
                .toString();
    }

    @NotNull
    public String selectShopsByShopTypeWorldItem(@NotNull Set<ShopType> shopTypes,
                                                 @Param("world_uuid") @Nullable UUID world,
                                                 @Param("item_code") @Nullable String itemCode) {
        return new SQL()
                .SELECT("""
                        CAST(world_uuid AS BINARY(16)) AS worldID,
                        pos_x AS posX,
                        pos_y AS posY,
                        pos_z AS posZ,
                        item_code AS itemCode,
                        owner_name AS ownerName,
                        buy_price AS buyPrice,
                        sell_price AS sellPrice,
                        quantity,
                        stock,
                        estimated_capacity AS estimatedCapacity
                        """)
                .FROM("Shop")
                .applyIf(itemCode != null, sql -> sql.WHERE("item_code = #{item_code}"))
                .applyIf(world != null,
                        sql -> sql.WHERE(
                                "world_uuid = #{world_uuid, javaType=java.util.UUID, jdbcType=OTHER}"))
                .WHERE(getShopConditions(shopTypes))
                .toString();
    }

    @NotNull
    public String selectShopsByShopTypeWorldItemDistance(
            @NotNull ShopType shopType,
            @Param("world_uuid") @NotNull UUID world,
            @Param("item_code") @NotNull String itemCode,
            @Param("x") int x,
            @Param("y") int y,
            @Param("z") int z,
            @Param("distance") double distance
    ) {
        return """
                """ +
                new SQL()
                        .SELECT("""
                                CAST(world_uuid AS BINARY(16)) AS worldID,
                                pos_x AS posX,
                                pos_y AS posY,
                                pos_z AS posZ,
                                item_code AS itemCode,
                                owner_name AS ownerName,
                                buy_price AS buyPrice,
                                sell_price AS sellPrice,
                                quantity,
                                stock,
                                estimated_capacity AS estimatedCapacity,
                                #{distance} * #{distance} AS distanceSquared
                                """)
                        .FROM("Shop")
                        .WHERE("item_code = #{item_code}",
                                "world_uuid = #{world_uuid, javaType=java.util.UUID, jdbcType=OTHER}")
                        .applyIf(shopType == ShopType.BUY,
                                sql -> sql.WHERE("buy_price IS NOT NULL"))
                        .applyIf(shopType == ShopType.SELL,
                                sql -> sql.WHERE("sell_price IS NOT NULL"))
                        .WHERE("pow(pos_x - #{x}, 2) + pow(pos_y - #{y}, 2) + pow(pos_z - #{z}, 2) <= distanceSquared")
                        .toString();
    }


}
