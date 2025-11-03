package io.github.md5sha256.chestshopdatabase.gui;

import io.github.md5sha256.chestshopdatabase.model.Shop;
import io.github.md5sha256.chestshopdatabase.model.ShopAttribute;
import io.github.md5sha256.chestshopdatabase.util.BlockPosition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

public class ShopComparators {

    private static final Map<ShopAttribute, Comparator<Shop>> DEFAULTS = new EnumMap<>(ShopAttribute.class);

    static {
        DEFAULTS.put(ShopAttribute.QUANTITY, Comparator.comparingInt(Shop::quantity));
        DEFAULTS.put(ShopAttribute.STOCK, Comparator.comparingInt(Shop::stock));
        DEFAULTS.put(ShopAttribute.REMAINING_CAPACITY,
                Comparator.comparingInt(Shop::estimatedCapacity));
        DEFAULTS.put(ShopAttribute.BUY_PRICE,
                Comparator.comparing(Shop::buyPrice, ShopComparators::comparingDouble));
        DEFAULTS.put(ShopAttribute.SELL_PRICE,
                Comparator.comparing(Shop::sellPrice, ShopComparators::comparingDouble));
        DEFAULTS.put(ShopAttribute.UNIT_BUY_PRICE,
                Comparator.comparing(Shop::unitBuyPrice, ShopComparators::comparingDouble));
        DEFAULTS.put(ShopAttribute.UNIT_SELL_PRICE,
                Comparator.comparing(Shop::unitSellPrice, ShopComparators::comparingDouble));
    }

    private static int comparingDouble(@Nullable Double d1, @Nullable Double d2) {
        if (d1 == null && d2 == null) {
            return 0;
        }
        if (d1 != null && d2 == null) {
            return -1;
        }
        if (d1 == null) {
            return 1;
        }
        return d1.compareTo(d2);
    }


    @Nonnull
    public static Map<ShopAttribute, Comparator<Shop>> defaults() {
        return Collections.unmodifiableMap(DEFAULTS);
    }

    private final Map<ShopAttribute, Comparator<Shop>> comparators = new EnumMap<>(ShopAttribute.class);

    public ShopComparators() {

    }

    public ShopComparators(Map<ShopAttribute, Comparator<Shop>> comparators) {
        this.comparators.putAll(comparators);
    }

    public ShopComparators withDefaults() {
        this.comparators.putAll(DEFAULTS);
        return this;
    }

    @Nonnull
    public ShopComparators withDistance(@Nonnull BlockPosition relativePosition) {
        this.comparators.put(ShopAttribute.DISTANCE, Comparator.comparingLong(shop -> shop.blockPosition()
                        .distanceSquared(relativePosition)));
        return this;
    }

    @Nonnull
    public Map<ShopAttribute, Comparator<Shop>> build() {
        return new EnumMap<>(this.comparators);
    }

}
