package io.github.md5sha256.chestshopdatabase.settings;

import io.github.md5sha256.chestshopdatabase.ReplacementRegistry;
import io.github.md5sha256.chestshopdatabase.gui.ShopResultsGUI;
import io.github.md5sha256.chestshopdatabase.model.Shop;
import io.github.md5sha256.chestshopdatabase.model.ShopType;
import io.github.md5sha256.chestshopdatabase.util.BlockPosition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DummyData {

    private static Component shopDisplayName() {
        return Component.text()
                .content("%owner%")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .build();
    }

    private static String distanceString(Shop shop, @Nullable BlockPosition queryPosition) {
        if (queryPosition == null) return "∞";
        long squaredDistance = shop.blockPosition().distanceSquared(queryPosition);
        if (squaredDistance == Long.MAX_VALUE) return "∞";
        return String.format("%d", (long) Math.floor(Math.sqrt(squaredDistance)));
    }

    private static List<Component> shopLore() {
        return List.of(
                        Component.text("Buy Price: %buy-price%, Sell Price: %sell-price%", NamedTextColor.AQUA),
                        Component.text("Unit Buy Price: %buy-price-unit%, Unit Sell Price: %sell-price-unit%", NamedTextColor.AQUA),
                        Component.text("Quantity: %quantity%", NamedTextColor.LIGHT_PURPLE),
                        Component.text("Stock: %stock%", NamedTextColor.YELLOW),
                        Component.text("Remaining Capacity: %capacity%", NamedTextColor.YELLOW),
                        Component.text("Distance: %distance%", NamedTextColor.RED),
                        Component.text("Location: %x%, %y%, %z% (%world%)", NamedTextColor.RED)
                );
    }

    public static ItemStack shopToIcon(@NotNull ShopType shopType) {
        Material material = switch (shopType) {
            case BOTH -> Material.ENDER_CHEST;
            case BUY -> Material.HOPPER_MINECART;
            case SELL -> Material.CHEST_MINECART;
        };
        ItemStack itemStack = ItemStack.of(material);
        itemStack.editMeta(meta -> {
            meta.displayName(shopDisplayName());
            meta.lore(shopLore());
        });
        return itemStack;
    }

}
