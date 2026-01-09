package io.github.md5sha256.chestshopdatabase.settings;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.Collections;
import java.util.List;

@ConfigSerializable
public record ItemCodeGroupings(@Setting List<ItemCodeGrouping> groupings) {

    public ItemCodeGroupings(@Nullable List<ItemCodeGrouping> groupings) {
        this.groupings = groupings == null ? Collections.emptyList() : List.copyOf(groupings);
    }

}
