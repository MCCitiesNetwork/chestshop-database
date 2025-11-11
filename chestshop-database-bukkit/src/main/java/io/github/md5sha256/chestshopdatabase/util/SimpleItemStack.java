package io.github.md5sha256.chestshopdatabase.util;

import io.papermc.paper.registry.keys.DataComponentTypeKeys;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@ConfigSerializable
public record SimpleItemStack(
        @Setting @Required @Nonnull Material material,
        @Setting @Nullable Component displayName,
        @Setting @Nullable List<Component> lore
) {

    public SimpleItemStack(@Nonnull Material material,
                           @Nullable Component displayName,
                           @Nullable List<Component> lore) {
        this.material = material;
        this.displayName = displayName;
        if (lore != null) {
            this.lore = List.copyOf(lore);
        } else {
            this.lore = null;
        }
    }

    @Nonnull
    public static SimpleItemStack fromItemStack(@Nonnull ItemStack itemStack) {
        return new SimpleItemStack(itemStack.getType(),
                itemStack.getItemMeta().displayName(),
                itemStack.lore());
    }

    @Nonnull
    public ItemStack asItemStack() {
        ItemStack itemStack = ItemStack.of(this.material);
        if (this.displayName != null || this.lore != null) {
            itemStack.editMeta(meta -> {
                if (this.displayName != null) {
                    meta.displayName(this.displayName);
                }
                if (this.lore != null) {
                    meta.lore(this.lore);
                }
            });
        }
        return itemStack;
    }

}
