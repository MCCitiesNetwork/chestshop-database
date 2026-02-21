package io.github.md5sha256.chestshopdatabase;

import io.github.md5sha256.chestshopdatabase.model.Shop;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;

import javax.annotation.Nonnull;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class ShopReplacements {

    private static final NumberFormat PRICE_FORMAT = new DecimalFormat("$#.##");

    private ShopReplacements() {
    }

    private static String priceToString(Double price) {
        return price == null ? "N/A" : PRICE_FORMAT.format(price);
    }

    private static String capacityToString(int cap) {
        return cap == -1 ? "∞" : String.valueOf(cap);
    }

    @Nonnull
    private static Component ownerName(@Nonnull Shop shop) {
        return Component.text(shop.ownerName());
    }

    private static Component buyPrice(@Nonnull Shop shop) {
        return Component.text(priceToString(shop.buyPrice()));
    }

    private static Component sellPrice(@Nonnull Shop shop) {
        return Component.text(priceToString(shop.sellPrice()));
    }

    private static Component unitSellPrice(@Nonnull Shop shop) {
        return Component.text(priceToString(shop.unitSellPrice()));
    }

    private static Component unitBuyPrice(@Nonnull Shop shop) {
        return Component.text(priceToString(shop.unitBuyPrice()));
    }

    private static Component remainingCapacity(@Nonnull Shop shop) {
        return Component.text(capacityToString(shop.estimatedCapacity()));
    }

    private static Component stock(@Nonnull Shop shop) {
        return Component.text(shop.stock());
    }

    private static Component quantity(@Nonnull Shop shop) {
        return Component.text(shop.quantity());
    }

    private static Component blockX(@Nonnull Shop shop) {
        return Component.text(shop.posX());
    }

    private static Component blockY(@Nonnull Shop shop) {
        return Component.text(shop.posY());
    }

    private static Component blockZ(@Nonnull Shop shop) {
        return Component.text(shop.posZ());
    }

    private static Component worldName(@Nonnull Shop shop) {
        World world = Bukkit.getWorld(shop.worldId());
        if (world == null) {
            return Component.text("unknown world");
        }
        return Component.text(world.getName());
    }

    public static void registerDefaults(@Nonnull ReplacementRegistry registry) {
        registry.replacement("%owner%", ShopReplacements::ownerName)
                .replacement("%buy-price%", ShopReplacements::buyPrice)
                .replacement("%sell-price%", ShopReplacements::sellPrice)
                .replacement("%sell-price-unit%", ShopReplacements::unitSellPrice)
                .replacement("%buy-price-unit%", ShopReplacements::unitBuyPrice)
                .replacement("%capacity%", ShopReplacements::remainingCapacity)
                .replacement("%stock%", ShopReplacements::stock)
                .replacement("%quantity%", ShopReplacements::quantity)
                .replacement("%x%", ShopReplacements::blockX)
                .replacement("%y%", ShopReplacements::blockY)
                .replacement("%z%", ShopReplacements::blockZ)
                .replacement("%world%", ShopReplacements::worldName);
    }
}
