package io.github.md5sha256.chestshopdatabase.command;

import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.md5sha256.chestshopdatabase.ChestShopState;
import io.github.md5sha256.chestshopdatabase.ExecutorState;
import io.github.md5sha256.chestshopdatabase.ItemDiscoverer;
import io.github.md5sha256.chestshopdatabase.database.ChestshopMapper;
import io.github.md5sha256.chestshopdatabase.database.DatabaseSession;
import io.github.md5sha256.chestshopdatabase.database.task.FindTaskFactory;
import io.github.md5sha256.chestshopdatabase.gui.FindState;
import io.github.md5sha256.chestshopdatabase.gui.ShopComparators;
import io.github.md5sha256.chestshopdatabase.gui.ShopResultsGUI;
import io.github.md5sha256.chestshopdatabase.gui.dialog.FindDialog;
import io.github.md5sha256.chestshopdatabase.model.ChestshopItem;
import io.github.md5sha256.chestshopdatabase.preview.PreviewHandler;
import io.github.md5sha256.chestshopdatabase.util.BlockPosition;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public record FindCommand(@NotNull ChestShopState shopState,
                          @NotNull ItemDiscoverer discoverer,
                          @NotNull FindTaskFactory taskFactory,
                          @NotNull ShopResultsGUI gui,
                          @NotNull Plugin plugin,
                          @NotNull PreviewHandler previewHandler,
                          @NotNull Supplier<DatabaseSession> session,
                          @NotNull ExecutorState executorState) implements CommandBean.Single {


    @Override
    public @NotNull LiteralArgumentBuilder<CommandSourceStack> command() {
        return baseFindCommand();
    }

    private LiteralArgumentBuilder<CommandSourceStack> baseFindCommand() {
        return Commands.literal("find")
                .requires(sourceStack -> sourceStack.getSender() instanceof Player player && player.hasPermission(
                        "csdb.find"))
                .executes(ctx -> {
                    if (!(ctx.getSource().getSender() instanceof Player player)) {
                        return Command.SINGLE_SUCCESS;
                    }
                    ItemStack inMainHand = player.getInventory().getItemInMainHand().asOne();
                    if (inMainHand.isEmpty()) {
                        player.sendMessage(Component.text(
                                "You must hold an item in your hand or specify an item code!",
                                NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }
                    processCommandWithItem(player, inMainHand);
                    return Command.SINGLE_SUCCESS;
                }).then(Commands.argument("itemCode", new ItemCodesArgumentType(shopState))
                        .executes(ctx -> {
                            if (!(ctx.getSource().getSender() instanceof Player player)) {
                                return Command.SINGLE_SUCCESS;
                            }
                            String itemCode = ctx.getArgument("itemCode", String.class);
                            processCommandWithItemCode(player, itemCode);
                            return Command.SINGLE_SUCCESS;
                        })
                ).then(buildToggle());
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildToggle() {
        return Commands.literal("toggle")
                .then(buildTogglePreview())
                .then(buildToggleVisibility())
                .then(buildToggleHologram());
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildToggleHologram() {
        return Commands.literal("hologram")
                .then(Commands.argument("visible", BoolArgumentType.bool())
                        .requires(sourceStack -> sourceStack.getSender() instanceof Player player
                                && player.hasPermission("csdb.hologram.toggle"))
                        .executes(ctx -> {
                            if (!(ctx.getSource().getSender() instanceof Player player)) {
                                return Command.SINGLE_SUCCESS;
                            }
                            Block block = player.getTargetBlockExact(5);
                            if (block == null || !Tag.SIGNS.isTagged(block.getType())) {
                                player.sendMessage(Component.text(
                                        "You must be looking at a shop sign!",
                                        NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                            Sign sign = (Sign) block.getState(false);
                            if (!ChestShopSign.isValid(sign)) {
                                player.sendMessage(Component.text(
                                        "You must be looking at a shop sign!",
                                        NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                            if (!ChestShopSign.canAccess(player, sign)) {
                                player.sendMessage(Component.text(
                                        "You do not have access to this shop sign!",
                                        NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                            World world = sign.getWorld();
                            UUID worldId = world.getUID();
                            int x = sign.getX();
                            int y = sign.getY();
                            int z = sign.getZ();
                            boolean visible = ctx.getArgument("visible", Boolean.class);
                            CompletableFuture.supplyAsync(() -> {
                                        try (DatabaseSession session = this.session.get()) {
                                            ChestshopMapper mapper = session.chestshopMapper();
                                            mapper.updateHologramVisibility(worldId, x, y, z, visible);
                                            if (!visible) {
                                                return null;
                                            }
                                            return mapper.selectShopByPosition(worldId,
                                                    x,
                                                    y,
                                                    z,
                                                    null,
                                                    true);
                                        }
                                    }, executorState.dbExec())
                                    .thenApplyAsync(shop -> {
                                        if (visible && shop != null) {
                                            previewHandler.renderPreview(world,
                                                    shop.fullyHydrate());
                                            return Boolean.TRUE;
                                        } else if (!visible) {
                                            previewHandler.destroyPreview(new BlockPosition(worldId,
                                                    x,
                                                    y,
                                                    z));
                                            return Boolean.TRUE;
                                        } else {
                                            return Boolean.FALSE;
                                        }
                                    }, executorState.mainThreadExec())
                                    .whenComplete((success, ex) -> {
                                        if (ex != null) {
                                            ex.printStackTrace();
                                            player.sendMessage(Component.text(
                                                    "Internal error occurred!",
                                                    NamedTextColor.RED));
                                            return;
                                        }
                                        player.sendMessage(Component.text(
                                                "Visibility toggled to " + visible,
                                                NamedTextColor.AQUA));
                                        if (!success) {
                                            player.sendMessage(Component.text(
                                                    "Failed to update hologram!",
                                                    NamedTextColor.RED));
                                        }
                                    });
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildToggleVisibility() {
        return Commands.literal("visibility")
                .then(Commands.argument("visible", BoolArgumentType.bool())
                        .requires(sourceStack -> sourceStack.getSender() instanceof Player player
                                && player.hasPermission("csdb.visibility.toggle"))
                        .executes(ctx -> {
                            if (!(ctx.getSource().getSender() instanceof Player player)) {
                                return Command.SINGLE_SUCCESS;
                            }
                            Block block = player.getTargetBlockExact(5);
                            if (block == null || !Tag.SIGNS.isTagged(block.getType())) {
                                player.sendMessage(Component.text(
                                        "You must be looking at a shop sign!",
                                        NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                            Sign sign = (Sign) block.getState(false);
                            if (!ChestShopSign.isValid(sign)) {
                                player.sendMessage(Component.text(
                                        "You must be looking at a shop sign!",
                                        NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                            if (!ChestShopSign.canAccess(player, sign)) {
                                player.sendMessage(Component.text(
                                        "You do not have access to this shop sign!",
                                        NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                            UUID world = sign.getWorld().getUID();
                            int x = sign.getX();
                            int y = sign.getY();
                            int z = sign.getZ();
                            boolean visible = ctx.getArgument("visible", Boolean.class);
                            CompletableFuture.runAsync(() -> {
                                        try (DatabaseSession session = this.session.get()) {
                                            ChestshopMapper mapper = session.chestshopMapper();
                                            mapper.updateShopVisibility(world, x, y, z, visible);
                                        }
                                    }, executorState.dbExec())
                                    .whenComplete((unused, ex) -> {
                                        if (ex != null) {
                                            ex.printStackTrace();
                                            player.sendMessage(Component.text(
                                                    "Internal error occurred!",
                                                    NamedTextColor.RED));
                                            return;
                                        }
                                        player.sendMessage(Component.text(
                                                "Visibility toggled to " + visible,
                                                NamedTextColor.AQUA));
                                    });
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildTogglePreview() {
        return Commands.literal("preview")
                .then(Commands.argument("visible", BoolArgumentType.bool())
                        .requires(sourceStack -> sourceStack.getSender() instanceof Player player
                                && player.hasPermission("csdb.preview.toggle"))
                        .executes(ctx -> {
                            if (!(ctx.getSource().getSender() instanceof Player player)) {
                                return Command.SINGLE_SUCCESS;
                            }
                            boolean visible = ctx.getArgument("visible", Boolean.class);
                            previewHandler.setVisible(player, visible).whenComplete((unused, ex) -> {
                                if (ex != null) {
                                    ex.printStackTrace();
                                    player.sendMessage(Component.text(
                                            "Internal error occurred!",
                                            NamedTextColor.RED));
                                    return;
                                }
                                player.sendMessage(Component.text(
                                        "Preview visibility toggled to " + visible,
                                        NamedTextColor.AQUA));
                            });
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    private void processCommandWithItem(@NotNull Player player, @NotNull ItemStack itemStack) {
        var loc = player.getLocation();
        BlockPosition queryPosition = new BlockPosition(player.getWorld().getUID(),
                loc.blockX(),
                loc.blockY(),
                loc.blockZ()
        );
        this.discoverer.discoverCodeFromItemStack(itemStack, code -> {
            if (code == null || code.isEmpty()) {
                player.sendMessage(Component.text("Unknown item: ", NamedTextColor.RED)
                        .append(itemStack.effectiveName()));
                return;
            }
            FindState findState = new FindState(
                    new ChestshopItem(itemStack, code),
                    new ShopComparators()
                            .withDefaults()
                            .withDistance(queryPosition)
                            .build()
            );
            findState.setWorld(queryPosition.world());
            findState.setQueryPosition(queryPosition);
            Dialog dialog = FindDialog.createMainPageDialog(findState, taskFactory, gui, plugin);
            player.showDialog(dialog);
        });
    }


    private void processCommandWithItemCode(@NotNull Player player, @NotNull String itemCode) {
        var loc = player.getLocation();
        BlockPosition queryPosition = new BlockPosition(player.getWorld().getUID(),
                loc.blockX(),
                loc.blockY(),
                loc.blockZ()
        );
        this.discoverer.discoverItemStackFromCode(itemCode, item -> {
            if (item == null || item.isEmpty()) {
                player.sendMessage(Component.text("Unknown item: " + itemCode, NamedTextColor.RED));
                return;
            }
            FindState findState = new FindState(
                    new ChestshopItem(item, itemCode),
                    new ShopComparators()
                            .withDefaults()
                            .withDistance(queryPosition).build()
            );
            findState.setWorld(queryPosition.world());
            findState.setQueryPosition(queryPosition);
            Dialog dialog = FindDialog.createMainPageDialog(findState, taskFactory, gui, plugin);
            player.showDialog(dialog);
        });
    }
}
