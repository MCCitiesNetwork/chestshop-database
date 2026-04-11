package io.github.md5sha256.chestshopdatabase.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.md5sha256.chestshopdatabase.database.task.ResyncTaskFactory;
import io.github.md5sha256.chestshopdatabase.task.TaskProgress;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.util.Tick;
import io.github.md5sha256.chestshopdatabase.settings.MessageContainer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public record ResyncCommand(
        @NotNull Plugin plugin,
        @NotNull ResyncTaskFactory taskFactory,
        @NotNull MessageContainer messages
) implements CommandBean.Single {


    @Override
    public @NotNull LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("resync")
                .requires(sourceStack -> sourceStack.getSender().hasPermission("csdb.resync"))
                .then(Commands.argument("chunksPerTick", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            BukkitScheduler scheduler = plugin.getServer().getScheduler();
                            sender.sendMessage(messages.messageFor("resync.queued"));
                            int chunksPerInterval = ctx.getArgument("chunksPerTick", Integer.class);
                            taskFactory.triggerResync(chunksPerInterval, 1).thenAccept(progress -> {
                                int ticks = Tick.tick().fromDuration(Duration.ofSeconds(30));
                                BukkitTask bukkitTask = scheduler.runTaskTimer(plugin, () -> {
                                    sender.sendMessage(formatProgress(progress));
                                }, ticks, ticks);
                                progress.chainOnComplete(() -> {
                                    bukkitTask.cancel();
                                    sender.sendMessage(messages.messageResolving("resync.complete",
                                            Placeholder.unparsed("total", String.valueOf(progress.total()))));
                                });
                            });
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    private Component formatProgress(@NotNull TaskProgress progress) {
        int done = progress.completed();
        int total = progress.total();
        double percent = total == 0 ? 0 : 100D * done / total;
        return messages.messageResolving("resync.progress",
                Placeholder.unparsed("done", String.valueOf(done)),
                Placeholder.unparsed("total", String.valueOf(total)),
                Placeholder.unparsed("percent", String.format("%.2f", percent)));
    }

}
