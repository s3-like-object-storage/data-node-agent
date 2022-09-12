package org.max.object.storage.data.agent;


import io.helidon.common.reactive.Single;
import io.helidon.config.Config;
import io.helidon.health.HealthSupport;
import io.helidon.health.checks.HealthChecks;
import io.helidon.media.jackson.JacksonSupport;
import io.helidon.metrics.MetricsSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;
import java.util.logging.LogManager;
import org.max.object.storage.data.agent.api.DataController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;


/**
 * The application main class.
 */
public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(DataController.class.getName());

    /**
     * Cannot be instantiated.
     */
    private Main() {
    }

    /**
     * Application main entry point.
     *
     * @param args command line arguments.
     */
    public static void main(final String[] args) {
        startServer();
    }

    /**
     * Install sl4j and logback, instead of of JUL.
     * See <a href="https://www.borischistov.com/articles/6">Helidon. Part 1: Creating simple web service and configuring logging</a>
     */
    private static void installLogbackLogger() {
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    /**
     * Start the server.
     */
    public static Single<WebServer> startServer() {

        installLogbackLogger();

        // By default this will pick up application.yaml from the classpath
        Config config = Config.create();

        WebServer server = WebServer.builder(createRouting(config))
            .config(config.get("server"))
            .addMediaSupport(JacksonSupport.create())
            .build();

        Single<WebServer> webserver = server.start();

        // Try to start the server. If successful, print some info and arrange to
        // print a message at shutdown. If unsuccessful, print the exception.
        webserver.thenAccept(ws -> {
                LOG.info("WEB server is up and running at {}", serverUrl(ws));
                ws.whenShutdown().thenRun(() -> LOG.info("WEB server is DOWN."));
            })
            .exceptionallyAccept(ex -> {
                LOG.error("Startup failed: " + ex.getMessage(), ex);
            });

        return webserver;
    }

    private static String serverUrl(WebServer ws) {
        return String.format("%s://%s:%d", (ws.hasTls() ? "https" : "http"),
                             ws.configuration().bindAddress().getHostName(),
                             ws.port());
    }

    /**
     * Creates new {@link Routing}.
     *
     * @param config configuration of this server
     * @return routing configured with JSON support, a health check, and a service
     */
    private static Routing createRouting(Config config) {
        final DataController dataController = new DataController(config);

        final HealthSupport health = HealthSupport.builder()
            .addLiveness(HealthChecks.healthChecks()) // Adds a convenient set of checks
            .build();

        final Routing.Builder builder = Routing.builder()
            .register(MetricsSupport.create()) // Metrics at "/metrics"
            .register(health) // Health at "/health"
            .register("/data", dataController);

        return builder.build();
    }
}
