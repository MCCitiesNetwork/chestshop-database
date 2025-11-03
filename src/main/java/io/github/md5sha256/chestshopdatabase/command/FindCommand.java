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
import io.github.md5sha256.chestshopdatabase.model.Shop;
import io.github.md5sha256.chestshopdatabase.util.BlockPosition;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public record FindCommand(@Nonnull ChestShopState shopState,
                          @Nonnull ItemDiscoverer discoverer,
                          @Nonnull FindTaskFactory taskFactory,
                          @Nonnull ShopResultsGUI gui) implements CommandBean.Single {

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("find")
                .requires(sourceStack -> sourceStack.getSender() instanceof Player player && player.hasPermission("csdb.find"))
                .then(Commands.argument("itemCode", new ItemCodesArgumentType(shopState))
                        .executes(ctx -> {
                            if (!(ctx.getSource().getSender() instanceof Player player)) {
                                return Command.SINGLE_SUCCESS;
                            }
                            String itemCode = ctx.getArgument("itemCode", String.class);
                            processCommand(player, itemCode);
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    private void processCommand(@Nonnull Player player, @Nonnull String itemCode) {
        var loc = player.getLocation();
        BlockPosition position = new BlockPosition(player.getWorld().getUID(),
                loc.blockX(),
                loc.blockY(),
                loc.blockX()
        );
        this.discoverer.discoverItemCode(itemCode, item -> {
            if (item == null || item.isEmpty()) {
                player.sendMessage(Component.text("Unknown item: " + itemCode, NamedTextColor.RED));
                return;
            }
            FindState findState = new FindState(
                    new ChestshopItem(item, itemCode),
                    new ShopComparators().withDistance(position).build()
            );
            findState.setWorld(position.world());
            Dialog dialog = FindDialog.createMainPageDialog(findState, taskFactory, gui);
            player.showDialog(dialog);
        });
    }

    private String priceToString(Double price) {
        return price == null ? "N/A" : price.toString();
    }

    private String capacityToString(int cap) {
        return cap == -1 ? "infinity" : String.valueOf(cap);
    }

    private Component formatShop(@Nonnull Shop shop) {
        return Component.text()
                .append(Component.text("Owner: " + shop.ownerName() + ",", NamedTextColor.GREEN))
                .appendNewline()
                .append(Component.text(String.format("Buy Price: %s, Sell Price: %s",
                        priceToString(shop.buyPrice()),
                        priceToString(shop.sellPrice())), NamedTextColor.AQUA))
                .appendNewline()
                .append(Component.text(String.format("Unit Buy Price: %s, Unit Sell Price: %s",
                        priceToString(shop.unitBuyPrice()),
                        priceToString(shop.unitSellPrice())), NamedTextColor.AQUA))
                .appendNewline()
                .append(Component.text(String.format("Quantity: %d", shop.quantity()),
                        NamedTextColor.LIGHT_PURPLE))
                .appendNewline()
                .append(Component.text(String.format("Stock: %d", shop.stock()),
                        NamedTextColor.YELLOW))
                .appendNewline()
                .append(Component.text(String.format("Remaining Capacity: %s",
                        capacityToString(shop.estimatedCapacity())), NamedTextColor.YELLOW))
                .appendNewline()
                .append(Component.text(String.format("Location: %d, %d, %d",
                        shop.posX(),
                        shop.posY(),
                        shop.posZ()), NamedTextColor.RED))
                .appendNewline()
                .build();
    }
}
