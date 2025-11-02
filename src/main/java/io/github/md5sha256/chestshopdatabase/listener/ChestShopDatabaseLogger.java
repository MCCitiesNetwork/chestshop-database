package io.github.md5sha256.chestshopdatabase.listener;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import io.github.md5sha256.chestshopdatabase.util.BlockPosition;

import java.util.Queue;
import java.util.UUID;

public class ChestShopDatabaseLogger extends AbstractDelegateExtent {

    private final Queue<BlockPosition> regionQueue;
    private final UUID world;

    protected ChestShopDatabaseLogger(Queue<BlockPosition> regionQueue, Extent extent, UUID world) {
        super(extent);
        this.regionQueue = regionQueue;
        this.world = world;
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position,
                                                            T block) throws WorldEditException {
        this.regionQueue.add(new BlockPosition(world, position.x(), position.y(), position.z()));
        return super.setBlock(position, block);
    }

}
