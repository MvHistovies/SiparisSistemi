package org.larune.siparis.storage;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AsyncDatabase {

    private final JavaPlugin plugin;
    private final ExecutorService executor;

    public AsyncDatabase(JavaPlugin plugin) {
        this.plugin = plugin;
        int threads = Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors()));
        this.executor = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "SiparisSistemi-DB");
            t.setDaemon(true);
            return t;
        });
    }

    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    public CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, executor);
    }

    public <T> void supplyAsync(Supplier<T> supplier, Consumer<T> callback) {
        CompletableFuture.supplyAsync(supplier, executor)
                .thenAccept(result -> {
                    if (Bukkit.isPrimaryThread()) {
                        callback.accept(result);
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
                    }
                })
                .exceptionally(ex -> {
                    plugin.getLogger().severe("Async DB error: " + ex.getMessage());
                    return null;
                });
    }

    public void runAsync(Runnable runnable, Runnable onComplete) {
        CompletableFuture.runAsync(runnable, executor)
                .thenRun(() -> {
                    if (Bukkit.isPrimaryThread()) {
                        onComplete.run();
                    } else {
                        Bukkit.getScheduler().runTask(plugin, onComplete);
                    }
                })
                .exceptionally(ex -> {
                    plugin.getLogger().severe("Async DB error: " + ex.getMessage());
                    return null;
                });
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void runSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }
}
