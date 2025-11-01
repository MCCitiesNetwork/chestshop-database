package io.github.md5sha256.chestshopdatabase.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.md5sha256.chestshopdatabase.ChestShopState;
import io.github.md5sha256.chestshopdatabase.database.DatabaseSession;
import io.github.md5sha256.chestshopdatabase.model.Shop;
import io.github.md5sha256.chestshopdatabase.model.ShopType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

public record DebugFindCommand(@Nonnull ChestShopState shopState,
                               @Nonnull Supplier<DatabaseSession> databaseSupplier,
                               @Nonnull ExecutorService dbExec,
                               @Nonnull Executor mainThreadExec) implements CommandBean.Single {

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("debugfind")
                .requires(sourceStack -> sourceStack.getSender() instanceof Player)
                .then(Commands.argument("itemCode", new ItemCodesArgumentType(shopState))
                        .executes(ctx -> {
                            if (!(ctx.getSource().getSender() instanceof Player player)) {
                                return Command.SINGLE_SUCCESS;
                            }
                            UUID world = player.getWorld().getUID();
                            String itemCode = ctx.getArgument("itemCode", String.class);
                            CompletableFuture.supplyAsync(() -> {
                                        try (DatabaseSession session = databaseSupplier.get()) {
                                            return session.mapper().selectShopsByShopTypeWorldItem(
                                                    EnumSet.allOf(ShopType.class),
                                                    world,
                                                    itemCode);
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                            // FIXME log warning
                                            return Collections.<Shop>emptyList();
                                        }
                                    }, dbExec)
                                    .thenAcceptAsync((shops) -> {
                                        for (Shop shop : shops) {
                                            player.sendMessage(formatShop(shop));
                                        }
                                    }, mainThreadExec);
                            return Command.SINGLE_SUCCESS;
                        })
                );
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
                .append(Component.text(String.format("Quantity: %d", shop.quantity()),
                        NamedTextColor.LIGHT_PURPLE))
                .appendNewline()
                .append(Component.text(String.format("Stock: %d", shop.stock()),
                        NamedTextColor.YELLOW))
                .appendNewline()
                .append(Component.text(String.format("Remaining Capacity: %s", capacityToString(shop.remainingCapacity())), NamedTextColor.YELLOW))
                .appendNewline()
                .append(Component.text(String.format("Location: %d, %d, %d",
                        shop.posX(),
                        shop.posY(),
                        shop.posZ()), NamedTextColor.RED))
                .appendNewline()
                .build();
    }
}
