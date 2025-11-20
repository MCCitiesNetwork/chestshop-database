package io.github.md5sha256.chestshopdatabase.preview;

import io.github.md5sha256.chestshopdatabase.ExecutorState;
import io.github.md5sha256.chestshopdatabase.database.ChestshopMapper;
import io.github.md5sha256.chestshopdatabase.database.DatabaseSession;
import io.github.md5sha256.chestshopdatabase.database.PreferenceMapper;
import io.github.md5sha256.chestshopdatabase.model.HydratedShop;
import io.github.md5sha256.chestshopdatabase.model.PartialHydratedShop;
import io.github.md5sha256.chestshopdatabase.settings.Settings;
import io.github.md5sha256.chestshopdatabase.util.BlockPosition;
import io.github.md5sha256.chestshopdatabase.util.ChunkPosition;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class PreviewHandler {

    private final Map<ChunkPosition, Map<BlockPosition, ItemDisplay>> displayEntities = new HashMap<>();
    private final List<ItemDisplay> allDisplays = new ArrayList<>();
    private final Set<UUID> hideRequested = new HashSet<>();

    private final Plugin plugin;
    private final Supplier<DatabaseSession> session;
    private final Supplier<Settings> settings;
    private final ExecutorState executorState;

    public PreviewHandler(@NotNull Plugin plugin,
                          @NotNull Supplier<DatabaseSession> session,
                          @NotNull ExecutorState executorState,
                          @NotNull Supplier<Settings> settings) {
        this.plugin = plugin;
        this.session = session;
        this.executorState = executorState;
        this.settings = settings;
    }

    public void resizeScale() {
        float scale = this.settings.get().shopPreviewScale();
        float sanitized = scale <= 0 ? 0.5f : scale;
        Matrix4f matrix = new Matrix4f().scale(sanitized);
        for (ItemDisplay display : allDisplays) {
            display.setTransformationMatrix(matrix);
        }
    }

    public void loadPreviewsInChunk(@NotNull Chunk chunk) {
        World world = chunk.getWorld();
        UUID worldId = world.getUID();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        CompletableFuture.supplyAsync(() -> {
                    try (DatabaseSession session = this.session.get()) {
                        ChestshopMapper mapper = session.chestshopMapper();
                        return mapper.selectShopsInChunk(worldId, chunkX, chunkZ, null, Boolean.TRUE);
                    }
                }, executorState.dbExec())
                .thenApply(shops -> shops.stream().map(PartialHydratedShop::fullyHydrate).toList())
                .whenCompleteAsync((shops, ex) -> {
                    if (ex != null) {
                        plugin.getLogger()
                                .warning(String.format("Failed to load shops in chunk: %d, %d",
                                        chunkX,
                                        chunkZ));
                        ex.printStackTrace();
                        return;
                    }
                    for (HydratedShop shop : shops) {
                        renderPreview(world, shop);
                    }
                }, executorState.mainThreadExec());
    }

    public void loadVisibility(@NotNull Player player) {
        CompletableFuture.supplyAsync(() -> {
                    try (DatabaseSession session = this.session.get()) {
                        PreferenceMapper mapper = session.preferenceMapper();
                        return Optional.ofNullable(mapper.selectPreference(player.getUniqueId()));
                    }
                }, this.executorState.dbExec())
                .thenApplyAsync(mapper -> mapper.orElse(this.settings.get()
                        .previewDefaultVisibility()), executorState.mainThreadExec())
                .whenCompleteAsync((visible, exception) -> {
                    if (exception != null) {
                        exception.printStackTrace();
                        return;
                    }
                    renderVisibility(player, visible);
                }, this.executorState.mainThreadExec());
    }

    public void clearCache(@NotNull UUID player) {
        this.hideRequested.remove(player);
    }

    public CompletableFuture<Void> setVisible(@NotNull Player player, boolean visible) {
        renderVisibility(player, visible);
        UUID playerId = player.getUniqueId();
        return CompletableFuture.runAsync(() -> {
            try (DatabaseSession session = this.session.get()) {
                PreferenceMapper mapper = session.preferenceMapper();
                mapper.insertPreference(playerId, visible);
            }
        });
    }

    private void renderVisibility(@NotNull Player player, boolean visible) {
        if (!visible && this.hideRequested.add(player.getUniqueId())) {
            hideToPlayer(player);
        } else if (visible && this.hideRequested.remove(player.getUniqueId())) {
            unhideToPlayer(player);
        }
    }

    private void hideToPlayer(@NotNull Player player) {
        for (ItemDisplay itemDisplay : allDisplays) {
            player.hideEntity(plugin, itemDisplay);
        }
    }

    private void unhideToPlayer(@NotNull Player player) {
        for (ItemDisplay itemDisplay : allDisplays) {
            player.showEntity(plugin, itemDisplay);
        }
    }

    private Optional<ItemDisplay> getExistingDisplay(@NotNull BlockPosition position) {
        ChunkPosition chunkPos = position.chunkPosition();
        Map<BlockPosition, ItemDisplay> map = this.displayEntities.get(chunkPos);
        if (map == null) {
            return Optional.empty();
        }
        ItemDisplay display = map.get(position);
        return Optional.ofNullable(display);
    }

    public void renderPreview(@NotNull World world, @NotNull HydratedShop shop) {
        ItemStack item = shop.item().itemStack();
        BlockPosition pos = shop.blockPosition();
        Optional<ItemDisplay> existing = getExistingDisplay(pos);
        if (existing.isPresent()) {
            existing.get().setItemStack(item);
            return;
        }
        Location location = new Location(world,
                shop.posX() + 0.5,
                shop.posY() + 1,
                shop.posZ() + 0.5);
        float scale = this.settings.get().shopPreviewScale();
        float sanitized = scale <= 0 ? 0.5f : scale;
        ItemDisplay display = world.spawn(location, ItemDisplay.class);
        display.setVisibleByDefault(true);
        display.setPersistent(false);
        display.setItemStack(item);
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
        display.setTransformationMatrix(new Matrix4f()
                .scale(sanitized));
        display.setBillboard(Display.Billboard.CENTER);
        displayEntities.computeIfAbsent(pos.chunkPosition(), x -> new HashMap<>())
                .put(pos, display);
        this.allDisplays.add(display);
        Server server = plugin.getServer();
        for (UUID playerId : this.hideRequested) {
            Player player = server.getPlayer(playerId);
            if (player != null) {
                player.hideEntity(plugin, display);
            }
        }
    }


    public void destroyPreview(@NotNull BlockPosition position) {
        ChunkPosition chunkPos = position.chunkPosition();
        Map<BlockPosition, ItemDisplay> map = this.displayEntities.get(chunkPos);
        if (map == null) {
            return;
        }
        ItemDisplay display = map.remove(position);
        if (display == null) {
            return;
        }
        display.remove();
        if (map.isEmpty()) {
            this.displayEntities.remove(chunkPos);
        }
        this.allDisplays.remove(display);
    }

    public void destroyPreviews(@NotNull ChunkPosition chunkPosition) {
        Map<BlockPosition, ItemDisplay> map = this.displayEntities.remove(chunkPosition);
        if (map == null || map.isEmpty()) {
            return;
        }
        for (ItemDisplay display : map.values()) {
            display.remove();
            this.allDisplays.remove(display);
        }
    }

}
