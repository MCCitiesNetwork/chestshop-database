package io.github.md5sha256.chestshopdatabase.gui;

import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.gui.type.util.Gui;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.github.stefvanschie.inventoryframework.pane.component.PagingButtons;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import io.github.md5sha256.chestshopdatabase.ReplacementRegistry;
import io.github.md5sha256.chestshopdatabase.database.DatabaseSession;
import io.github.md5sha256.chestshopdatabase.ExecutorState;
import io.github.md5sha256.chestshopdatabase.model.Shop;
import io.github.md5sha256.chestshopdatabase.model.ShopType;
import io.github.md5sha256.chestshopdatabase.settings.MessageContainer;
import io.github.md5sha256.chestshopdatabase.settings.Settings;
import io.github.md5sha256.chestshopdatabase.util.BlockPosition;
import io.github.md5sha256.chestshopdatabase.util.SimpleItemStack;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public record ShopResultsGUI(@NotNull Plugin plugin,
                             @NotNull ReplacementRegistry replacements,
                             @NotNull Supplier<Settings> settings,
                             @NotNull Supplier<MessageContainer> messages,
                             @NotNull Supplier<DatabaseSession> sessionSupplier,
                             @NotNull ExecutorState executorState) {


    private static String distanceString(Shop shop, @Nullable BlockPosition queryPosition) {
        if (queryPosition == null) return "∞";
        long squaredDistance = shop.blockPosition().distanceSquared(queryPosition);
        if (squaredDistance == Long.MAX_VALUE) return "∞";
        return String.format("%d", (long) Math.floor(Math.sqrt(squaredDistance)));
    }

    private static Component stripItalics(Component lore) {
        return lore.decoration(TextDecoration.ITALIC, false);
    }

    private ItemStack template(@NotNull ShopType shopType) {
        Settings instance = this.settings.get();
        SimpleItemStack itemStack = switch (shopType) {
            case BUY -> instance.buyShopTemplate();
            case SELL -> instance.sellShopTemplate();
            case BOTH -> instance.bothShopTemplate();
        };
        return itemStack.asItemStack();
    }


    private ItemStack shopToIcon(@NotNull Shop shop,
                                 @Nullable BlockPosition queryPosition,
                                 @Nullable String queriedItemCode) {
        ReplacementRegistry forked = this.replacements.fork()
                .stringReplacement("%distance%", s -> distanceString(s, queryPosition));
        ItemStack itemStack = template(shop.shopType());
        boolean isSimilar = queriedItemCode != null && !shop.itemCode().equals(queriedItemCode);
        itemStack.editMeta(meta -> {
                    Component displayName = stripItalics(forked.applyReplacements(shop,
                            itemStack.effectiveName()));
                    List<Component> lore = Objects.requireNonNullElse(meta.lore(), Collections.<Component>emptyList())
                            .stream()
                            .map(component -> forked.applyReplacements(shop, component))
                            .map(ShopResultsGUI::stripItalics)
                            .toList();
                    meta.displayName(displayName);
                    meta.lore(lore);
                }
        );
        if (isSimilar) {
            itemStack.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return itemStack;
    }

    private GuiItem shopItemPreview(@NotNull ItemStack item) {
        return new GuiItem(item, event -> event.setCancelled(true), this.plugin);
    }

    public ChestGui createGui(@NotNull Component title,
                              @NotNull List<Shop> shops,
                              @NotNull ItemStack shopItem,
                              @Nullable BlockPosition queryPosition,
                              @Nullable String queriedItemCode) {
        return createGui(title, shops, shopItem, queryPosition, queriedItemCode, null);
    }

    private void runClickCommand(@NotNull Player player, @NotNull String command, @NotNull Shop shop) {
        player.performCommand(injectPlaceholders(command, shop));
        player.closeInventory();
    }

    @NotNull
    private String injectPlaceholders(@NotNull String s, @NotNull Shop shop) {
        BlockPosition pos = shop.blockPosition();
        return s.replace("<x>", String.valueOf(pos.x()))
                .replace("<y>", String.valueOf(pos.y()))
                .replace("<z>", String.valueOf(pos.z()));
    }

    private GuiItem shopToGuiItem(@NotNull Shop shop,
                                  @Nullable BlockPosition queryPosition,
                                  @Nullable String queriedItemCode,
                                  @NotNull Map<String, ItemStack> itemCache,
                                  @NotNull Gui resultsGui) {
        Settings settings = settings().get();
        String leftClickCommand = settings.leftClickCommand();
        String rightClickCommand = settings.rightClickCommand();
        ItemStack icon = shopToIcon(shop, queryPosition, queriedItemCode);

        return new GuiItem(icon, (event) -> {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            ClickType click = event.getClick();
            if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
                if (rightClickCommand != null && !rightClickCommand.isEmpty()) {
                    runClickCommand(player, rightClickCommand, shop);
                    return;
                }
                lookupItemStack(shop.itemCode(), itemCache)
                        .thenAcceptAsync(itemStack -> {
                            if (itemStack == null) {
                                return;
                            }
                            ChestGui previewGui = createItemPreviewGui(
                                    shop.itemCode(), itemStack, resultsGui);
                            previewGui.show(player);
                        }, task -> plugin.getServer().getScheduler()
                                .runTaskLater(plugin, task, 1))
                        .exceptionally(ex -> {
                            plugin.getLogger().warning(
                                    "Failed to lookup item stack for " + shop.itemCode() + ": " + ex.getMessage());
                            return null;
                        });
                return;
            }
            if (leftClickCommand != null && !leftClickCommand.isEmpty()) {
                runClickCommand(player, leftClickCommand, shop);
            }
        }, this.plugin);
    }

    @NotNull
    private ChestGui createItemPreviewGui(@NotNull String itemCode,
                                          @NotNull ItemStack itemStack,
                                          @NotNull Gui parent) {
        Component title = Component.text("Item Preview: " + itemCode);
        ChestGui gui = new ChestGui(3, ComponentHolder.of(title), this.plugin);

        StaticPane pane = new StaticPane(9, 3);
        pane.addItem(new GuiItem(itemStack, event -> event.setCancelled(true), this.plugin), 4, 1);

        ItemStack backItem = ItemStack.of(Material.ARROW);
        backItem.editMeta(meta -> {
            Component displayName = messages.get().messageFor("gui.results.back")
                    .decoration(TextDecoration.ITALIC, false);
            meta.displayName(displayName);
        });
        GuiItem backButton = new GuiItem(backItem, event -> {
            event.getView().close();
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> parent.show(event.getWhoClicked()), 1);
        }, this.plugin);
        pane.addItem(backButton, 0, 2);

        ItemStack fillItem = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE);
        fillItem.editMeta(meta -> meta.displayName(Component.empty()));
        pane.fillWith(fillItem, null, this.plugin);

        gui.addPane(Slot.fromXY(0, 0), pane);
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        gui.setOnClose(event -> {
            if (event.getReason() == InventoryCloseEvent.Reason.OPEN_NEW) {
                return;
            }
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> parent.show(event.getPlayer()), 1);
        });
        return gui;
    }

    private CompletableFuture<@Nullable ItemStack> lookupItemStack(@NotNull String itemCode,
                                                                    @NotNull Map<String, ItemStack> cache) {
        ItemStack cached = cache.get(itemCode);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return CompletableFuture.supplyAsync(() -> {
            try (DatabaseSession session = sessionSupplier.get()) {
                byte[] bytes = session.chestshopMapper().selectItemBytes(itemCode);
                if (bytes == null) {
                    return null;
                }
                ItemStack item = ItemStack.deserializeBytes(bytes);
                cache.put(itemCode, item);
                return item;
            }
        }, executorState.dbExec());
    }

    public ChestGui createGui(@NotNull Component title,
                              @NotNull List<Shop> shops,
                              @NotNull ItemStack shopItem,
                              @Nullable BlockPosition queryPosition,
                              @Nullable String queriedItemCode,
                              @Nullable Gui parent) {
        ChestGui gui = new ChestGui(6, ComponentHolder.of(title), this.plugin);
        Map<String, ItemStack> itemCache = new ConcurrentHashMap<>();
        if (queriedItemCode != null) {
            itemCache.put(queriedItemCode, shopItem);
        }
        List<GuiItem> items = new ArrayList<>();
        for (Shop shop : shops) {
            GuiItem item = shopToGuiItem(shop, queryPosition, queriedItemCode, itemCache, gui);
            items.add(item);
        }
        PaginatedPane mainPane = new PaginatedPane(9, 5);
        mainPane.populateWithGuiItems(items);

        StaticPane footerPane = getFooterPane(parent);
        footerPane.addItem(shopItemPreview(shopItem), 4, 0);

        ItemStack fillItem = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE);
        fillItem.editMeta(meta -> meta.displayName(Component.empty()));
        footerPane.fillWith(fillItem, null, this.plugin);

        PagingButtons pagingButtons = getPagingButtons(mainPane);

        gui.addPane(Slot.fromXY(0, 0), mainPane);
        gui.addPane(Slot.fromXY(0, 5), footerPane);
        gui.addPane(Slot.fromXY(3, 5), pagingButtons);
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        if (parent != null) {
            gui.setOnClose(event -> {
                // Don't force-open the parent gui if the reason is OPEN_NEW
                if (event.getReason() == InventoryCloseEvent.Reason.OPEN_NEW) {
                    return;
                }
                // Delay opening the ui 1 tick later otherwise all IF listeners will break
                this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                    parent.show(event.getPlayer());
                }, 1);
            });
        }
        return gui;
    }

    private @NotNull StaticPane getFooterPane(@Nullable Gui parent) {
        StaticPane footerPane = new StaticPane(9, 1, Pane.Priority.LOWEST);
        ItemStack backItem = ItemStack.of(Material.ARROW);
        backItem.editMeta(meta -> {
            Component displayName = messages.get().messageFor("gui.results.back")
                    .decoration(TextDecoration.ITALIC, false);
            meta.displayName(displayName);
        });
        GuiItem backButton = new GuiItem(backItem, event -> {
            event.getView().close();
            if (parent != null) {
                parent.show(event.getWhoClicked());
            }
        }, this.plugin);
        footerPane.addItem(backButton, 0, 0);
        return footerPane;
    }

    private @NotNull PagingButtons getPagingButtons(PaginatedPane mainPane) {
        PagingButtons pagingButtons = new PagingButtons(3,
                Pane.Priority.HIGH,
                mainPane,
                this.plugin);
        Component nextPageComp = messages.get().messageFor("gui.results.next-page")
                .decoration(TextDecoration.ITALIC, false);
        Component prevPageComp = messages.get().messageFor("gui.results.prev-page")
                .decoration(TextDecoration.ITALIC, false);
        ItemStack nextButton = ItemStack.of(Material.PAPER);
        nextButton.editMeta(meta -> meta.displayName(nextPageComp));
        ItemStack prevButton = ItemStack.of(Material.PAPER);
        prevButton.editMeta(meta -> meta.displayName(prevPageComp));
        pagingButtons.setForwardButton(new GuiItem(nextButton, this.plugin));
        pagingButtons.setBackwardButton(new GuiItem(prevButton, this.plugin));
        return pagingButtons;
    }

}
