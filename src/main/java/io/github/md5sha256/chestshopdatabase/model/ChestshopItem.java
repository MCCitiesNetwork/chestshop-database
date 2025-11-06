package io.github.md5sha256.chestshopdatabase.model;

import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

public record ChestshopItem(
        @Nonnull ItemStack itemStack,
        @Nonnull String itemCode
) {

    public ChestshopItem(@Nonnull ChestshopItem other) {
        this(other.itemStack.clone(), other.itemCode());
    }

    @Nonnull
    public ItemStack itemStack() {
        return this.itemStack.clone();
    }

}
