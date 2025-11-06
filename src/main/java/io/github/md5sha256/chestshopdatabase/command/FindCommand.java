package io.github.md5sha256.chestshopdatabase.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.md5sha256.chestshopdatabase.ChestShopState;
import io.github.md5sha256.chestshopdatabase.ItemDiscoverer;
import io.github.md5sha256.chestshopdatabase.database.FindTaskFactory;
import io.github.md5sha256.chestshopdatabase.gui.FindState;
import io.github.md5sha256.chestshopdatabase.gui.ShopComparators;
import io.github.md5sha256.chestshopdatabase.gui.ShopResultsGUI;
import io.github.md5sha256.chestshopdatabase.gui.dialog.FindDialog;
import io.github.md5sha256.chestshopdatabase.model.ChestshopItem;
import io.github.md5sha256.chestshopdatabase.util.BlockPosition;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public record FindCommand(@Nonnull ChestShopState shopState,
                          @Nonnull ItemDiscoverer discoverer,
                          @Nonnull FindTaskFactory taskFactory,
                          @Nonnull ShopResultsGUI gui) implements CommandBean.Single {


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
                );
    }

    private void processCommandWithItem(@Nonnull Player player, @Nonnull ItemStack itemStack) {
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
            Dialog dialog = FindDialog.createMainPageDialog(findState, taskFactory, gui);
            player.showDialog(dialog);
        });
    }


    private void processCommandWithItemCode(@Nonnull Player player, @Nonnull String itemCode) {
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
            Dialog dialog = FindDialog.createMainPageDialog(findState, taskFactory, gui);
            player.showDialog(dialog);
        });
    }
}
