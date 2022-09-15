package org.max.object.storage.data.agent.util;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ReactiveUtils {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ReactiveUtils() {
        throw new AssertionError("Can't instantiate utility-only class");
    }

    /**
     * This method should be used only in places, where we MUST wait for stage completeness,
     * b/c the whole nio thread will be blocked (which considered as anti-pattern for event loop applications).
     */
    public static <T> T waitForStageCompletion(CompletionStage<T> stage) {
        try {
            return stage.toCompletableFuture().get();
        }
        catch (InterruptedException | ExecutionException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Ordinary thread sleep, but without InterruptedException
     */
    public static void sleep(long sleepInterval, TimeUnit units) {
        try {
            units.sleep(sleepInterval);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOG.info("Sleep interrupted", ex);
        }
    }
}
