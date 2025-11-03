package io.github.md5sha256.chestshopdatabase.model;

import javax.annotation.Nonnull;

public enum ShopAttribute {
    BUY_PRICE("Buy Price"),
    SELL_PRICE("Sell Price"),
    UNIT_BUY_PRICE("Unit Buy Price"),
    UNIT_SELL_PRICE("Unit Sell Price"),
    STOCK("Stock"),
    REMAINING_CAPACITY("Remaining Capacity"),
    QUANTITY("Quantity"),
    DISTANCE("Distance");

    private final String displayName;

    ShopAttribute(@Nonnull String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return this.displayName;
    }
}
