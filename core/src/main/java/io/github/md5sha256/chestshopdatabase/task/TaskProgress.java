package io.github.md5sha256.chestshopdatabase.task;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class TaskProgress {

    private final AtomicInteger completed;
    private final AtomicInteger total;
    private final AtomicReference<Runnable> onComplete = new AtomicReference<>();

    public TaskProgress(int total) {
        this.completed = new AtomicInteger(0);
        this.total = new AtomicInteger(total);
    }

    public void chainOnComplete(@NotNull Runnable onComplete) {
        this.onComplete.getAndUpdate(existing -> {
            if (existing == null) {
                return onComplete;
            }
            return () -> {
                existing.run();
                onComplete.run();
            };
        });
        if (isDone()) {
            onComplete.run();
        }
    }

    public void incrementTotal() {
        this.total.getAndIncrement();
    }

    public int total() {
        return this.total.get();
    }

    public boolean isDone() {
        return this.completed.get() == this.total.get();
    }

    public void markCompleted() {
        if (this.completed.incrementAndGet() == this.total.get()) {
            triggerCompleted();
        }
    }

    public void markCompleted(int amount) {
        if (this.completed.addAndGet(amount) == this.total.get()) {
            triggerCompleted();
        }
    }

    public int completed() {
        return this.completed.get();
    }

    public void triggerCompleted() {
        Runnable runnable = this.onComplete.get();
        if (runnable != null) {
            runnable.run();
        }
    }

}
