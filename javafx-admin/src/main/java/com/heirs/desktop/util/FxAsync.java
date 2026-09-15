package com.heirs.desktop.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.application.Platform;

/**
 * Reusable asynchronous execution utility for JavaFX.
 * Executes background HTTP operations off the JavaFX Application Thread,
 * delivering success and error callbacks safely via Platform.runLater.
 */
public final class FxAsync {

    private static final int POOL_SIZE = 4;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(POOL_SIZE, new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "heirs-fx-async-" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    });

    private FxAsync() {
    }

    /**
     * Executes a supplier asynchronously on the background thread pool,
     * and routes results/errors back to the JavaFX Application Thread.
     *
     * @param backgroundSupplier operation to perform in background
     * @param onSuccess callback on JavaFX thread with the result
     * @param onError callback on JavaFX thread when an exception occurs
     * @param <T> result type
     * @return CompletableFuture representing the execution
     */
    public static <T> CompletableFuture<T> run(
            Supplier<T> backgroundSupplier,
            Consumer<T> onSuccess,
            Consumer<Throwable> onError) {

        return CompletableFuture.supplyAsync(backgroundSupplier, EXECUTOR)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        Throwable cause = throwable;
                        if (cause instanceof java.util.concurrent.CompletionException && cause.getCause() != null) {
                            cause = cause.getCause();
                        }
                        final Throwable finalError = cause;
                        if (onError != null) {
                            Platform.runLater(() -> onError.accept(finalError));
                        }
                    } else {
                        if (onSuccess != null) {
                            Platform.runLater(() -> onSuccess.accept(result));
                        }
                    }
                });
    }

    /**
     * Gracefully terminates the background executor pool when the application stops.
     */
    public static void shutdown() {
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(1, TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
