package io.github.md5sha256.chestshopdatabase.listener;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import io.github.md5sha256.chestshopdatabase.ChestShopState;
import io.github.md5sha256.chestshopdatabase.util.BlockPosition;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

public class WorldEditHandler implements Listener {

    private final Queue<BlockPosition> regionQueue = new ConcurrentLinkedDeque<>();
    private final Plugin plugin;
    private final ChestShopState shopState;
    private boolean isFAWE = false;

    public WorldEditHandler(@Nonnull Plugin plugin, @Nonnull ChestShopState shopState) {
        this.plugin = plugin;
        this.shopState = shopState;
        initialize();
    }

    private void schedulePollTask() {
        this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> {
            int size = regionQueue.size();
            for (int i = 0; i < size; i++) {
                BlockPosition pos = regionQueue.poll();
                if (pos == null) {
                    break;
                }
                this.shopState.queueShopDeletion(pos);
            }
        }, 20, 20);
    }

    private boolean isSupported() {
        return (Bukkit.getPluginManager()
                .getPlugin("WorldEdit") != null || Bukkit.getPluginManager()
                .getPlugin("FastAsyncWorldEdit") != null);
    }

    private void initialize() {
        if (!isSupported()) {
            return;
        }
        WorldEdit.getInstance().getEventBus().register(this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        isFAWE = (Bukkit.getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") != null);
        schedulePollTask();
    }

    @Subscribe
    public void onEditSession(EditSessionEvent event) {
        if (event.getWorld() == null) {
            return;
        }
        Actor actor = event.getActor();
        UUID worldId = BukkitAdapter.adapt(event.getWorld()).getUID();
        if (actor != null && event.getStage() == (isFAWE ? EditSession.Stage.BEFORE_HISTORY : EditSession.Stage.BEFORE_CHANGE)) {
            event.setExtent(new ChestShopDatabaseLogger(this.regionQueue,
                    event.getExtent(),
                    worldId));
        }
    }

    @EventHandler
    public void onWorldEditDisable(PluginDisableEvent event) {
        if (!event.getPlugin().equals(this.plugin)) return;

        WorldEdit.getInstance().getEventBus().unregister(this);
    }

}
