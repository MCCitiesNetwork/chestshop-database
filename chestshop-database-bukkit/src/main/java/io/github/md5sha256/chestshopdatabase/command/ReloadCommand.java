package io.github.md5sha256.chestshopdatabase.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.md5sha256.chestshopdatabase.ChestshopDatabasePlugin;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;

public record ReloadCommand(@NotNull ChestshopDatabasePlugin plugin) implements CommandBean.Single {

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("csdb.reload"))
                .executes(ctx -> {
                    Audience audience = ctx.getSource().getSender();
                    audience.sendMessage(plugin.messages().messageFor("command.reload.start"));
                    plugin.reload().whenComplete((success, error) -> {
                        if (error != null) {
                            error.printStackTrace();
                        }
                        if (!success || error != null) {
                            audience.sendMessage(plugin.messages().messageFor("command.reload.failure"));
                        } else {
                            audience.sendMessage(plugin.messages().messageFor("command.reload.success"));
                        }
                    });
                    return Command.SINGLE_SUCCESS;
                });
    }
}
