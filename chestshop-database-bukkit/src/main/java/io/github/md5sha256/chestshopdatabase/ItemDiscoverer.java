package io.github.md5sha256.chestshopdatabase;

import com.Acrobot.ChestShop.Events.ItemParseEvent;
import com.Acrobot.ChestShop.Events.ItemStringQueryEvent;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.md5sha256.chestshopdatabase.util.TickUtil;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class ItemDiscoverer {

    private final TickUtil<String> itemCodes;
    private final TickUtil<ItemStack> itemStacks;
    private final Set<ItemStack> queuedItemStacks = new HashSet<>();
    private final Set<String> queuedItemCodes = new HashSet<>();
    private final Cache<String, ItemStack> cachedItemCodes;
    private final Cache<ItemStack, String> cachedItemStacks;
    private final Map<String, List<Consumer<ItemStack>>> itemCodeCallbacks = new HashMap<>();
    private final Map<ItemStack, List<Consumer<String>>> itemStackCallbacks = new HashMap<>();
    private final Server server;
    private final Logger logger;

    public ItemDiscoverer(int bufferSize,
                          @NotNull Duration cacheTime,
                          int cacheSize,
                          @NotNull Server server,
                          @NotNull Logger logger) {
        this.server = server;
        this.itemCodes = new TickUtil<>(bufferSize, this::discoverItemStacks);
        this.itemStacks = new TickUtil<>(bufferSize, this::discoverItemCodes);
        this.cachedItemCodes = CacheBuilder.newBuilder()
                .expireAfterAccess(cacheTime)
                .initialCapacity(cacheSize)
                .build();
        this.cachedItemStacks = CacheBuilder.newBuilder()
                .expireAfterAccess(cacheTime)
                .initialCapacity(cacheSize)
                .build();
        this.logger = logger;
    }

    public void schedulePollTask(@NotNull Plugin plugin,
                                 @NotNull BukkitScheduler scheduler,
                                 int elementsPerTick,
                                 int intervalTicks) {
        this.itemCodes.schedulePollTask(plugin, scheduler, elementsPerTick, intervalTicks);
        this.itemStacks.schedulePollTask(plugin, scheduler, elementsPerTick, intervalTicks);
    }

    private void discoverItemStacks(@NotNull List<String> itemCodes) {
        PluginManager pluginManager = this.server.getPluginManager();
        for (String code : itemCodes) {
            this.queuedItemCodes.remove(code);
            ItemStack itemStack = null;
            try {
                ItemParseEvent parseEvent = new ItemParseEvent(code);
                pluginManager.callEvent(parseEvent);
                itemStack = parseEvent.getItem();
            } catch (Exception ex) {
                this.logger.warning("Failed to lookup item stack from item code!");
                ex.printStackTrace();
            }
            markDiscovery(code, itemStack);
        }
    }


    private void discoverItemCodes(@NotNull List<ItemStack> stacks) {
        PluginManager pluginManager = this.server.getPluginManager();
        for (ItemStack item : stacks) {
            this.queuedItemStacks.remove(item);
            String itemCode = null;
            try {
                ItemStringQueryEvent parseEvent = new ItemStringQueryEvent(item);
                pluginManager.callEvent(parseEvent);
                itemCode = parseEvent.getItemString();
            } catch (Exception ex) {
                this.logger.warning("Failed to lookup item code from item stack!");
                ex.printStackTrace();
            }
            markDiscovery(itemCode, item);
        }
    }



    private void markDiscovery(@Nullable String code, @Nullable ItemStack itemStack) {
        if (code != null && itemStack != null) {
            this.cachedItemCodes.put(code, itemStack);
            this.cachedItemStacks.put(itemStack, code);
        }
        if (code != null) {
            List<Consumer<ItemStack>> itemStackConsumers = this.itemCodeCallbacks.remove(code);
            if (itemStackConsumers != null) {
                itemStackConsumers.forEach(consumer -> consumer.accept(itemStack));
            }
        }
        if (itemStack != null) {
            List<Consumer<String>> itemCodeConsumers = this.itemStackCallbacks.remove(itemStack);
            if (itemCodeConsumers != null) {
                itemCodeConsumers.forEach(consumer -> consumer.accept(code));
            }
        }
    }

    public void discoverItemStackFromCode(@NotNull String code,
                                          @NotNull Consumer<ItemStack> callback) {
        ItemStack cached = this.cachedItemCodes.getIfPresent(code);
        if (cached != null) {
            callback.accept(cached);
            return;
        }
        if (this.queuedItemCodes.add(code)) {
            this.itemCodes.queueElement(code);
        }
        this.itemCodeCallbacks.computeIfAbsent(code, unused -> new ArrayList<>()).add(callback);
    }

    public void discoverCodeFromItemStack(@NotNull ItemStack item,
                                          @NotNull Consumer<String> callback) {
        String cached = this.cachedItemStacks.getIfPresent(item);
        if (cached != null) {
            callback.accept(cached);
            return;
        }
        if (this.queuedItemStacks.add(item)) {
            this.itemStacks.queueElement(item);
        }
        this.itemStackCallbacks.computeIfAbsent(item, unused -> new ArrayList<>()).add(callback);
    }

}
