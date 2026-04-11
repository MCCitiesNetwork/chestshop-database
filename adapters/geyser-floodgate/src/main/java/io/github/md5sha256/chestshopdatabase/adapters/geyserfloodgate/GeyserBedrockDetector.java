package io.github.md5sha256.chestshopdatabase.adapters.geyserfloodgate;

import org.geysermc.geyser.api.GeyserApi;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Bedrock detection via Geyser when the Geyser plugin is present on this server.
 */
public final class GeyserBedrockDetector {

    private GeyserBedrockDetector() {
    }

    public static boolean isBedrockPlayer(@NotNull UUID uuid) {
        var api = GeyserApi.api();
        return api != null && api.isBedrockPlayer(uuid);
    }
}
