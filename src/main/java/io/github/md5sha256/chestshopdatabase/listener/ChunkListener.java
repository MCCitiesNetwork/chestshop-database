package io.github.md5sha256.chestshopdatabase.listener;

import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.Acrobot.ChestShop.Utils.uBlock;
import io.github.md5sha256.chestshopdatabase.ItemDiscoverer;
import io.github.md5sha256.chestshopdatabase.database.ChestShopDatabase;
import io.github.md5sha256.chestshopdatabase.util.BlockPosition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Server;
import org.bukkit.Tag;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class ChunkListener implements Listener {

    private final Logger logger;
    private final Server server;
    private final ItemDiscoverer discoverer;
    private final ChestShopDatabase database;

    public ChunkListener(@Nonnull Logger logger,
                         @Nonnull Server server,
                         @Nonnull ItemDiscoverer discoverer,
                         @Nonnull ChestShopDatabase database) {
        this.logger = logger;
        this.server = server;
        this.discoverer = discoverer;
        this.database = database;
    }

    private String[] getSignLines(@Nonnull Sign sign) {
        PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();
        List<Component> components = sign.getSide(Side.FRONT).lines();
        String[] lines = new String[components.size()];
        for (int i = 0; i < components.size(); i++) {
            lines[i] = serializer.serialize(components.get(i));
        }
        return lines;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        UUID world = event.getWorld().getUID();
        var tileEntities = event.getChunk()
                .getTileEntities(b -> Tag.SIGNS.isTagged(b.getType()), false);

        for (BlockState state : tileEntities) {
            if (!(state instanceof Sign sign)) {
                return;
            }
            String[] lines = getSignLines(sign);
            if (!ChestShopSign.isValid(lines)) {
                return;
            }
            BlockPosition position = new BlockPosition(world,
                    sign.getX(),
                    sign.getY(),
                    sign.getZ());
            Container container = uBlock.findConnectedContainer(sign);
            if (container == null) {
                continue;
            }
            container.getInventory().getContents();
            String itemCode = ChestShopSign.getItem(lines);
            this.discoverer.discoverItemStackFromCode(itemCode,
                    item -> this.database.registerShop(position, item, itemCode, lines, container));
        }
    }

}
