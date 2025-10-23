package io.github.md5sha256.chestshopFinder.database;

import io.github.md5sha256.chestshopFinder.model.ShopType;
import org.apache.ibatis.jdbc.SQL;

import javax.annotation.Nonnull;
import java.util.UUID;

public class MariaDatabaseUtil {

    @Nonnull
    public String selectShopsByItem(@Nonnull ShopType shopType, @Nonnull String itemCode) {
        return new SQL()
                .SELECT("""
                        world_uuid AS worldID,
                        pos_x AS posX,
                        pos_y AS posY,
                        pos_z AS posZ,
                        item_code AS itemCode,
                        owner_name AS ownerName,
                        buy_price AS buyPrice,
                        sell_price AS sellPrice,
                        quantity
                        """)
                .FROM("Shop")
                .WHERE("item_code = #{item_code}")
                .applyIf(shopType == ShopType.BUY, sql -> sql.WHERE("buy_price IS NOT NULL"))
                .applyIf(shopType == ShopType.SELL, sql -> sql.WHERE("sell_price IS NOT NULL"))
                .toString();
    }

    @Nonnull
    public String selectShopsByWorldAndItem(@Nonnull ShopType shopType,
                                            @Nonnull UUID world,
                                            @Nonnull String itemCode) {
        return new SQL()
                .SELECT("""
                        world_uuid AS worldID,
                        pos_x AS posX,
                        pos_y AS posY,
                        pos_z AS posZ,
                        item_code AS itemCode,
                        owner_name AS ownerName,
                        buy_price AS buyPrice,
                        sell_price AS sellPrice,
                        quantity
                        """)
                .FROM("Shop")
                .WHERE("item_code = #{item_code}", "world_uuid = #{world_uuid)}")
                .applyIf(shopType == ShopType.BUY, sql -> sql.WHERE("buy_price IS NOT NULL"))
                .applyIf(shopType == ShopType.SELL, sql -> sql.WHERE("sell_price IS NOT NULL"))
                .toString();
    }

    @Nonnull
    public String selectShopsByWorldItemDistance(
            @Nonnull ShopType shopType,
            @Nonnull UUID world,
            @Nonnull String itemCode,
            int x,
            int y,
            int z,
            double distance
    ) {
        return """
                @distance = #{distance}
                @distanceSquared = @distance * @distance;
                @x = #{x};
                @y = #{y};
                @z = #{z};
                """ +
                new SQL()
                        .SELECT("""
                                world_uuid AS worldID,
                                pos_x AS posX,
                                pos_y AS posY,
                                pos_z AS posZ,
                                item_code AS itemCode,
                                owner_name AS ownerName,
                                buy_price AS buyPrice,
                                sell_price AS sellPrice,
                                quantity
                                """)
                        .FROM("Shop")
                        .WHERE("item_code = #{item_code}", "world_uuid = #{world_uuid)}")
                        .applyIf(shopType == ShopType.BUY,
                                sql -> sql.WHERE("buy_price IS NOT NULL"))
                        .applyIf(shopType == ShopType.SELL,
                                sql -> sql.WHERE("sell_price IS NOT NULL"))
                        .WHERE("pow(pos_x - @x, 2) + pow(pos_y - @y, 2) + pow(pos_z - @z, 2) <= @distanceSquared")
                        .toString();
    }


}
