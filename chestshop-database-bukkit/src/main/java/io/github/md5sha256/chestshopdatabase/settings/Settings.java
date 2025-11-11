package io.github.md5sha256.chestshopdatabase.settings;

import io.github.md5sha256.chestshopdatabase.util.SimpleItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public record Settings(
        @Setting("shop-template-buy") @Required SimpleItemStack buyShopTemplate,
        @Setting("shop-template-sell") @Required SimpleItemStack sellShopTemplate,
        @Setting("shop-template-both") @Required SimpleItemStack bothShopTemplate,
        @Setting("shop-icon-click-command") @Nullable String clickCommand
) {
}
