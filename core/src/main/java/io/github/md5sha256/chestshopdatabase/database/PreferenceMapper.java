package io.github.md5sha256.chestshopdatabase.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface PreferenceMapper {

    @Nullable
    Boolean selectPreference(@NotNull UUID player);

    void insertPreference(@NotNull UUID player, @Nullable Boolean preference);
}
