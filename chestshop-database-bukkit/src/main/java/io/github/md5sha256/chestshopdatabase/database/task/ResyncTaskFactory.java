package io.github.md5sha256.chestshopdatabase.database.task;

import com.Acrobot.Breeze.Utils.PriceUtil;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.Acrobot.ChestShop.Utils.uBlock;
import com.google.common.collect.Sets;
import io.github.md5sha256.chestshopdatabase.ChestShopState;
import io.github.md5sha256.chestshopdatabase.ExecutorState;
import io.github.md5sha256.chestshopdatabase.ItemDiscoverer;
import io.github.md5sha256.chestshopdatabase.database.DatabaseMapper;
import io.github.md5sha256.chestshopdatabase.database.DatabaseSession;
import io.github.md5sha256.chestshopdatabase.model.ChestshopItem;
import io.github.md5sha256.chestshopdatabase.model.HydratedShop;
import io.github.md5sha256.chestshopdatabase.model.ShopStockUpdate;
import io.github.md5sha256.chestshopdatabase.task.TaskProgress;
import io.github.md5sha256.chestshopdatabase.util.BlockPosition;
import io.github.md5sha256.chestshopdatabase.util.ChunkPosition;
import io.github.md5sha256.chestshopdatabase.util.InventoryUtil;
import io.github.md5sha256.chestshopdatabase.util.TickUtil;
import org.bukkit.Chunk;
import org.bukkit.Server;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ResyncTaskFactory {

    private final ChestShopState chestShopState;
    private final ItemDiscoverer discoverer;
    private final Supplier<DatabaseSession> sessionSupplier;
    private final ExecutorState executorState;
    private final Plugin plugin;
    private final BukkitScheduler scheduler;

    public ResyncTaskFactory(@NotNull ChestShopState chestShopState,
                             @NotNull ItemDiscoverer discoverer,
                             @NotNull Supplier<DatabaseSession> sessionSupplier,
                             @NotNull ExecutorState executorState,
                             @NotNull Plugin plugin) {
        this.chestShopState = chestShopState;
        this.discoverer = discoverer;
        this.sessionSupplier = sessionSupplier;
        this.executorState = executorState;
        this.plugin = plugin;
        this.scheduler = plugin.getServer().getScheduler();
    }

    private static List<Bucket<ChunkPosition>> toBuckets(@NotNull List<BlockPosition> positions) {
        Map<ChunkPosition, List<BlockPosition>> map = new HashMap<>();
        for (BlockPosition position : positions) {
            map.computeIfAbsent(position.chunkPosition(), unused -> new ArrayList<>())
                    .add(position);
        }
        List<Bucket<ChunkPosition>> buckets = new ArrayList<>(map.size());
        for (Map.Entry<ChunkPosition, List<BlockPosition>> entry : map.entrySet()) {
            buckets.add(new Bucket<>(entry.getKey(), entry.getValue()));
        }
        return buckets;
    }

    private static double toDouble(BigDecimal decimal) {
        return decimal.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().doubleValue();
    }

    private <T> CompletableFuture<T> toHydratedShop(@NotNull Sign sign,
                                                    @NotNull String[] lines,
                                                    @NotNull Container container,
                                                    Function<HydratedShop, T> callback) {
        UUID world = sign.getWorld().getUID();
        int posX = sign.getX();
        int posY = sign.getY();
        int posZ = sign.getZ();
        String itemCode = ChestShopSign.getItem(lines);
        String owner = ChestShopSign.getOwner(lines);
        int quantity = ChestShopSign.getQuantity(lines);
        String priceLine = ChestShopSign.getPrice(lines);
        BigDecimal buyPriceDecimal = PriceUtil.getExactBuyPrice(priceLine);
        BigDecimal sellPriceDecimal = PriceUtil.getExactSellPrice(priceLine);
        Double buyPrice = buyPriceDecimal.equals(PriceUtil.NO_PRICE) ? null : toDouble(
                buyPriceDecimal);
        Double sellPrice = sellPriceDecimal.equals(PriceUtil.NO_PRICE) ? null : toDouble(
                sellPriceDecimal);
        CompletableFuture<T> future = new CompletableFuture<>();
        this.discoverer.discoverItemStackFromCode(itemCode, itemStack -> {
            if (itemStack == null || itemStack.isEmpty()) {
                // FIXME log warning
                future.complete(callback.apply(null));
                return;
            }
            HydratedShop shop = new HydratedShop(world,
                    posX,
                    posY,
                    posZ,
                    new ChestshopItem(itemStack, itemCode),
                    owner,
                    buyPrice,
                    sellPrice,
                    quantity,
                    InventoryUtil.countItems(itemStack, container.getInventory()),
                    InventoryUtil.remainingCapacity(itemStack, container.getInventory()));
            future.complete(callback.apply(shop));
        });
        return future;
    }

    private <T> CompletableFuture<T> toUpdateShopStock(@NotNull Sign sign,
                                                       @NotNull String[] lines,
                                                       @NotNull Container container,
                                                       Function<ShopStockUpdate, T> callback) {
        UUID world = sign.getWorld().getUID();
        int posX = sign.getX();
        int posY = sign.getY();
        int posZ = sign.getZ();
        String itemCode = ChestShopSign.getItem(lines);
        CompletableFuture<T> future = new CompletableFuture<>();
        this.discoverer.discoverItemStackFromCode(itemCode, itemStack -> {
            if (itemStack == null || itemStack.isEmpty()) {
                // FIXME log warning
                future.complete(callback.apply(null));
                return;
            }
            ShopStockUpdate shopStockUpdate = new ShopStockUpdate(world,
                    posX,
                    posY,
                    posZ,
                    InventoryUtil.countItems(itemStack, container.getInventory()),
                    InventoryUtil.remainingCapacity(itemStack, container.getInventory()));
            future.complete(callback.apply(shopStockUpdate));
        });
        return future;
    }


    private void processChunks(@NotNull List<Bucket<Chunks>> buckets,
                               @NotNull TaskProgress progress) {
        for (Bucket<Chunks> bucket : buckets) {
            processChunk(bucket.chunk(), bucket.blocks(), progress);
        }
    }

    private void processChunk(@NotNull Chunks chunks,
                              @NotNull List<BlockPosition> blocks,
                              @NotNull TaskProgress progress) {
        Chunk chunk = chunks.mainChunk();

        if (chunk == null) {
            for (BlockPosition blockPosition : blocks) {
                chestShopState.queueShopDeletion(blockPosition).thenRun(progress::markCompleted);
            }
            for (Chunk neighbour : chunks.neighbours()) {
                if (neighbour != null) {
                    neighbour.removePluginChunkTicket(this.plugin);
                }
            }
            return;
        }
        UUID world = chunk.getWorld().getUID();
        Set<BlockPosition> known = new HashSet<>(blocks);
        Set<BlockPosition> knownProcessed = new HashSet<>(known.size());
        for (BlockState state : chunk.getTileEntities(block -> Tag.SIGNS.isTagged(block.getType()),
                false)) {
            Sign sign = (Sign) state;
            BlockPosition position = new BlockPosition(world,
                    sign.getX(),
                    sign.getY(),
                    sign.getZ());
            String[] lines = sign.getLines();
            if (!ChestShopSign.isValid(lines)) {
                continue;
            }
            Container container = uBlock.findConnectedContainer(sign);
            if (container == null) {
                continue;
            }

            if (known.contains(position)) {
                toUpdateShopStock(sign, lines, container, update -> {
                    if (update != null) {
                        return this.chestShopState.queueShopUpdate(update);
                    }
                    return CompletableFuture.<Void>completedFuture(null);
                }).thenCompose(Function.identity()).thenRun(progress::markCompleted);
                knownProcessed.add(position);
            } else {
                progress.incrementTotal();
                toHydratedShop(sign, lines, container, shop -> {
                    if (shop != null) {
                        return this.chestShopState.queueShopCreation(shop);
                    }
                    return CompletableFuture.<Void>completedFuture(null);
                }).thenCompose(Function.identity()).thenRun(progress::markCompleted);
            }
        }
        Set<BlockPosition> toDelete = Sets.difference(known, knownProcessed);
        for (BlockPosition blockPosition : toDelete) {
            chestShopState.queueShopDeletion(blockPosition).thenRun(progress::markCompleted);
        }
        chunk.removePluginChunkTicket(this.plugin);
        for (Chunk neighbour : chunks.neighbours()) {
            if (neighbour != null) {
                neighbour.removePluginChunkTicket(this.plugin);
            }
        }
    }


    private void processBuckets(@NotNull List<Bucket<ChunkPosition>> buckets,
                                @NotNull TaskProgress progress,
                                @NotNull TickUtil<Bucket<Chunks>> chunkProcessor) {
        Server server = this.plugin.getServer();
        for (Bucket<ChunkPosition> bucket : buckets) {
            ChunkPosition chunkPosition = bucket.chunk();
            World world = server.getWorld(chunkPosition.worldId());
            List<BlockPosition> blocks = bucket.blocks();
            if (world == null) {
                for (BlockPosition position : blocks) {
                    chestShopState.queueShopDeletion(position).thenRun(progress::markCompleted);
                }
                continue;
            }
            CompletableFuture<Chunk> mainFuture = world.getChunkAtAsync(chunkPosition.chunkX(),
                    chunkPosition.chunkZ(),
                    false);
            int chunkX = chunkPosition.chunkX();
            int chunkZ = chunkPosition.chunkZ();
            List<CompletableFuture<Chunk>> neighbourFutures = Stream.of(
                    world.getChunkAtAsync(chunkX + 1, chunkZ, false),
                    world.getChunkAtAsync(chunkX - 1, chunkZ, false),
                    world.getChunkAtAsync(chunkX, chunkZ + 1, false),
                    world.getChunkAtAsync(chunkX, chunkZ - 1, false)
            ).map(future -> future.thenApply(chunk -> {
                if (chunk != null) {
                    chunk.addPluginChunkTicket(this.plugin);
                }
                return chunk;
            })).toList();
            List<CompletableFuture<Chunk>> allFutures = new ArrayList<>();
            allFutures.add(mainFuture);
            allFutures.addAll(neighbourFutures);
            CompletableFuture.allOf(allFutures.toArray(CompletableFuture[]::new)).thenRun(() -> {
                Chunk chunk = mainFuture.getNow(null);
                if (chunk != null) {
                    chunk.addPluginChunkTicket(this.plugin);
                }
                List<Chunk> neighbours = neighbourFutures.stream()
                        .map(future -> future.getNow(null))
                        .toList();
                chunkProcessor.queueElement(new Bucket<>(new Chunks(chunk, neighbours), blocks));
            });
        }
    }

    @NotNull
    public CompletableFuture<TaskProgress> triggerResync(int chunksPerInterval, int intervalTicks) {
        CompletableFuture<TaskProgress> future = new CompletableFuture<>();
        this.executorState.dbExec().submit(() -> {
            List<Bucket<ChunkPosition>> buckets;
            int numBlocks;
            try (DatabaseSession session = sessionSupplier.get()) {
                DatabaseMapper mapper = session.mapper();
                List<BlockPosition> blocks = mapper.selectShopsPositionsByWorld(null);
                numBlocks = blocks.size();
                buckets = toBuckets(blocks);
            } catch (Exception ex) {
                future.completeExceptionally(ex);
                return;
            }

            TaskProgress progress = new TaskProgress(numBlocks);
            future.complete(progress);

            TickUtil<Bucket<Chunks>> processUtil = new TickUtil<>(list -> processChunks(list,
                    progress));
            TickUtil<Bucket<ChunkPosition>> loadUtil = new TickUtil<>(list -> processBuckets(list,
                    progress,
                    processUtil));
            loadUtil.queueElements(buckets);
            loadUtil.schedulePollTask(this.plugin,
                    this.scheduler,
                    chunksPerInterval,
                    intervalTicks);
            processUtil.schedulePollTask(this.plugin,
                    this.scheduler,
                    chunksPerInterval,
                    intervalTicks);
            progress.chainOnComplete(() -> {
                loadUtil.cancelPollTask();
                processUtil.cancelPollTask();
            });
        });
        return future;
    }

    private record Chunks(@Nullable Chunk mainChunk, @NotNull List<@Nullable Chunk> neighbours) {
    }

    private record Bucket<T>(@NotNull T chunk, @NotNull List<BlockPosition> blocks) {
    }

}
