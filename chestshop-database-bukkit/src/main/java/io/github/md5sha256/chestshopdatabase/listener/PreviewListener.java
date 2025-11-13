package io.github.md5sha256.chestshopdatabase.listener;

import io.github.md5sha256.chestshopdatabase.ExecutorState;
import io.github.md5sha256.chestshopdatabase.database.ChestshopMapper;
import io.github.md5sha256.chestshopdatabase.database.DatabaseSession;
import io.github.md5sha256.chestshopdatabase.model.HydratedShop;
import io.github.md5sha256.chestshopdatabase.model.PartialHydratedShop;
import io.github.md5sha256.chestshopdatabase.preview.PreviewHandler;
import io.github.md5sha256.chestshopdatabase.util.ChunkPosition;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Logger;

public record PreviewListener(
        @NotNull Logger logger,
        @NotNull Supplier<DatabaseSession> sessionSupplier,
        @NotNull ExecutorState executorState,
        @NotNull PreviewHandler handler
) implements Listener {

    @EventHandler
    public void onChunkLoad(@NotNull ChunkLoadEvent event) {
        World world = event.getWorld();
        UUID worldId = event.getWorld().getUID();
        int chunkX = event.getChunk().getX();
        int chunkZ = event.getChunk().getZ();
        CompletableFuture.supplyAsync(() -> {
                    try (DatabaseSession session = sessionSupplier.get()) {
                        ChestshopMapper mapper = session.chestshopMapper();
                        return mapper.selectShopsInChunk(worldId, chunkX, chunkZ, null, Boolean.TRUE);
                    }
                })
                .thenApply(shops -> shops.stream().map(PartialHydratedShop::fullyHydrate).toList())
                .whenCompleteAsync((shops, ex) -> {
                    if (ex != null) {
                        logger.warning(String.format("Failed to load shops in chunk: %d, %d",
                                chunkX,
                                chunkZ));
                        ex.printStackTrace();
                        return;
                    }
                    for (HydratedShop shop : shops) {
                        this.handler.renderPreview(world, shop);
                    }
                }, executorState.mainThreadExec());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        ChunkPosition chunkPos = new ChunkPosition(event.getWorld().getUID(),
                chunk.getX(),
                chunk.getZ());
        this.handler.destroyPreviews(chunkPos);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLogin(PlayerJoinEvent event) {
        this.handler.loadVisibility(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLogout(PlayerQuitEvent event) {
        this.handler.clearCache(event.getPlayer().getUniqueId());
    }

}
