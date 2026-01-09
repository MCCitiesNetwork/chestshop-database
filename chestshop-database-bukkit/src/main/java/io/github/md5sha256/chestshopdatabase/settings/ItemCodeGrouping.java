package io.github.md5sha256.chestshopdatabase.settings;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.Set;

@ConfigSerializable
public record ItemCodeGrouping(@Setting @Required String itemCode, @Setting @Required Set<String> aliases) {

    public ItemCodeGrouping(@NotNull String itemCode, @NotNull Set<String> aliases) {
        this.itemCode = itemCode;
        this.aliases = Set.copyOf(aliases);
    }
}
