package io.github.md5sha256.chestshopdatabase.model;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public record ChestshopItem(
        @NotNull ItemStack itemStack,
        @NotNull String itemCode
) {

    public ChestshopItem(@NotNull ChestshopItem other) {
        this(other.itemStack.clone(), other.itemCode());
    }

    @NotNull
    public ItemStack itemStack() {
        return this.itemStack.clone();
    }
}