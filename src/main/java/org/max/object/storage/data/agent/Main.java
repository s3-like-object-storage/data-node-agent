package org.max.object.storage.data.agent;


import io.helidon.common.reactive.Single;
import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import io.helidon.health.HealthSupport;
import io.helidon.health.checks.HealthChecks;
import io.helidon.media.jackson.JacksonSupport;
import io.helidon.metrics.MetricsSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.accesslog.AccessLogSupport;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletionStage;
import java.util.logging.LogManager;
import org.max.object.storage.data.agent.api.DataController;
import org.max.object.storage.data.agent.domain.BinaryDataStorageService;
import org.max.object.storage.data.agent.storage.DataStorageServiceFactory;
import org.max.object.storage.data.agent.storage.file.AppendFile;
import org.max.object.storage.data.agent.storage.sqlite.AppendFileDao;
import org.max.object.storage.data.agent.storage.sqlite.ObjectMappingDao;
import org.max.object.storage.data.agent.util.ReactiveUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;


/**
 * The application main class.
 */
public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
     * See
     * <a href="https://www.borischistov.com/articles/6">Helidon. Part 1: Creating simple web service and configuring logging</a>
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

        // prepare DB
        initDBAndWait(createDbClient(config));

        WebServer server =
            WebServer.builder(createRouting(config)).config(config.get("server")).addMediaSupport(JacksonSupport.create())
                .build();

        Single<WebServer> webserver = server.start();

        // Try to start the server. If successful, print some info and arrange to
        // print a message at shutdown. If unsuccessful, print the exception.
        webserver.thenAccept(ws -> {
                LOG.info("WEB server is up and running at {}", serverUrl(ws));
                ws.whenShutdown().thenRun(() -> LOG.info("WEB server is DOWN."));
            }).
            exceptionallyAccept(ex -> LOG.error("Startup failed: " + ex.getMessage(), ex));

        return webserver;
    }

    private static String serverUrl(WebServer ws) {
        return String.format("%s://%s:%d", (ws.hasTls() ? "https" : "http"), ws.configuration().bindAddress().getHostName(),
                             ws.port());
    }

    /**
     * Creates new {@link Routing}.
     *
     * @param config configuration of this server
     * @return routing configured with JSON support, a health check, and a service
     */
    private static Routing createRouting(Config config) {

        final DbClient dbClient = createDbClient(config);

        // DB layer
        final ObjectMappingDao objectMappingDao = new ObjectMappingDao(dbClient);
        final AppendFileDao appendFileDao = new AppendFileDao(dbClient);

        // file storage layer
        final AppendFile appendFile = AppendFile.create(appendFileDao, config);

        // Services
        final BinaryDataStorageService storageService = DataStorageServiceFactory.newInstance(objectMappingDao, appendFile,
                                                                                              config);

        // Controllers
        final DataController dataController = new DataController(storageService);

        // Health at "/health"
        final HealthSupport health =
            HealthSupport.builder().addLiveness(HealthChecks.healthChecks()) // Adds a convenient set of checks
                .build();

        // Metrics at "/metrics"
        final MetricsSupport metrics = MetricsSupport.create();

        // Utility like
        final AccessLogSupport accessLog = AccessLogSupport.create(config.get("server.access-log"));

        final Routing.Builder builder = Routing.builder().
            register(metrics).
            register(health).
            register(accessLog).
            register(DataController.BASE_URL, dataController);

        return builder.build();
    }

    private static DbClient createDbClient(Config config) {
        Config dbConfig = config.get("db");
        return DbClient.builder(dbConfig).build();
    }

    private static final String CREATE_OBJECT_MAPPING_DDL =
        "CREATE TABLE IF NOT EXISTS object_mapping(id CHAR(36) PRIMARY KEY, file_name VARCHAR(64), " +
            "offset INTEGER, size INTEGER)";

    private static final String CREATE_FILE_APPEND_DDL =
        "CREATE TABLE IF NOT EXISTS append_file(id INTEGER PRIMARY KEY, file_path VARCHAR(256))";

    public static void initDBAndWait(DbClient dbClient) {
        CompletionStage<Void> completionStage1 = dbClient.inTransaction(
                tx -> tx.createDmlStatement(CREATE_OBJECT_MAPPING_DDL).execute()
            ).
            thenAccept(t -> LOG.info("DDL <== table 'object_mapping' created")).
            exceptionallyAccept(ex -> LOG.error("Can't create DB", ex));

        ReactiveUtils.waitForStageCompletion(completionStage1);

        CompletionStage<Void> completionStage2 = dbClient.inTransaction(
                tx -> tx.createDmlStatement(CREATE_FILE_APPEND_DDL).execute()
            ).
            thenAccept(t -> LOG.info("DDL <== table 'file_append' created")).
            exceptionallyAccept(ex -> LOG.error("Can't create DB", ex));


        ReactiveUtils.waitForStageCompletion(completionStage2);
    }
}
