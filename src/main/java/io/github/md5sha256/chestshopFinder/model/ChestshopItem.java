package io.github.md5sha256.chestshopFinder.model;

import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

public record ChestshopItem(
        @Nonnull ItemStack itemStack,
        @Nonnull String itemCode) {
}
