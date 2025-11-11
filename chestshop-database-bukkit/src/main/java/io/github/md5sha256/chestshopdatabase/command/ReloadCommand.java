package io.github.md5sha256.chestshopdatabase.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.md5sha256.chestshopdatabase.ChestshopDatabasePlugin;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

public record ReloadCommand(@NotNull ChestshopDatabasePlugin plugin) implements CommandBean.Single {

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("csdb.reload"))
                .executes(ctx -> {
                    Audience audience = ctx.getSource().getSender();
                    audience.sendMessage(Component.text("Reloading CSDB messages and settings",
                            NamedTextColor.GREEN));
                    plugin.reload().whenComplete((success, error) -> {
                        if (error != null) {
                            error.printStackTrace();
                        }
                        if (!success || error != null) {
                            audience.sendMessage(Component.text(
                                    "Reload unsuccessful, check console for errors",
                                    NamedTextColor.RED));
                        } else {
                            audience.sendMessage(Component.text("Reload successful",
                                    NamedTextColor.GREEN));
                        }
                    });
                    return Command.SINGLE_SUCCESS;
                });
    }
}
