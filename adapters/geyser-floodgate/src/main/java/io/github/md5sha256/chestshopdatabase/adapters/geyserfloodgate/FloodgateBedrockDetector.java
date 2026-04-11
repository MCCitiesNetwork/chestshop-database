package io.github.md5sha256.chestshopdatabase.adapters.geyserfloodgate;

import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Bedrock detection via Floodgate (typically when Floodgate is installed on this backend).
 */
public final class FloodgateBedrockDetector {

    private FloodgateBedrockDetector() {
    }

    public static boolean isBedrockPlayer(@NotNull UUID uuid) {
        return FloodgateApi.getInstance().isFloodgatePlayer(uuid);
    }
}
