package io.github.md5sha256.chestshopdatabase.database.task;

import io.github.md5sha256.chestshopdatabase.ExecutorState;
import io.github.md5sha256.chestshopdatabase.database.DatabaseSession;
import io.github.md5sha256.chestshopdatabase.gui.FindState;
import io.github.md5sha256.chestshopdatabase.model.Shop;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

public record FindTaskFactory(@NotNull Supplier<DatabaseSession> sessionSupplier,
                              @NotNull ExecutorState executorState) {

    public CompletableFuture<List<Shop>> findTask(
            @NotNull FindState findState
    ) {
        if (findState.shopTypes().isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        FindState copy = new FindState(findState);
        return CompletableFuture.supplyAsync(() -> {
                    try (DatabaseSession session = sessionSupplier.get()) {
                        return session.chestshopMapper().selectShopsByShopTypeWorldItem(
                                copy.shopTypes(),
                                copy.world().orElse(null),
                                copy.item().itemCode()
                        );
                    }
                }, executorState.dbExec())
                .thenApply(results -> copy.applyToStream(results.stream()).toList())
                .orTimeout(30, TimeUnit.SECONDS)
                .thenApplyAsync(Function.identity(), executorState.mainThreadExec());
    }

}
