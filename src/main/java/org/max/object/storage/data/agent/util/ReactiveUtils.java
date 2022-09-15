package org.max.object.storage.data.agent.util;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public final class ReactiveUtils {

    private ReactiveUtils() {
        throw new AssertionError("Can't instantiate utility-only class");
    }

    public static <T> T waitForStageCompletion(CompletionStage<T> stage) {
        try {
            return stage.toCompletableFuture().get();
        }
        catch (InterruptedException | ExecutionException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }
}
