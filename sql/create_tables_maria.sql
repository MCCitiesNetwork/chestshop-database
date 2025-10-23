CREATE TABLE Item
(
    item_code  VARCHAR(255) PRIMARY KEY,
    item_bytes VARBINARY(60000) NOT NULL,
    INDEX `idx_item_item_code` (item_code)
);

CREATE TABLE Shop
(
    world_uuid   UUID         NOT NULL,
    pos_x        INT          NOT NULL,
    pos_y        INT          NOT NULL,
    pos_z        INT          NOT NULL,
    item_code    VARCHAR(255) NOT NULL,
    owner_name   VARCHAR(255) NOT NULL,
    buy_price    DECIMAL(10, 2),
    sell_price   DECIMAL(10, 2),
    quantity     INT          NOT NULL,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (world_uuid, pos_x, pos_y, pos_z),
    INDEX `idx_shop_item_code` (item_code)
);

ALTER TABLE Shop
    ADD CONSTRAINT chk_price_not_null
        CHECK (buy_price IS NOT NULL OR sell_price IS NOT NULL),
    ADD CONSTRAINT chk_quantity_greater_than_zero
        CHECK (quantity > 0),
    ADD CONSTRAINT fk_shop_item_item_code
        FOREIGN KEY (`item_code`) REFERENCES `Item` (`item_code`) ON DELETE CASCADE
;

CREATE TRIGGER trigger_shop_update_timestamp
    BEFORE UPDATE
    ON Shop
    FOR EACH ROW
BEGIN
    SET NEW.last_updated = CURRENT_TIMESTAMP;
END;