package org.max.object.storage.data.agent.api;

import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.max.object.storage.data.agent.domain.FileData;


/**
 * Store file data in local file system.
 */
public class DataController implements Service {

    private final Map<String, String> fileToData = new HashMap<>();

    private static final Logger LOGGER = Logger.getLogger(DataController.class.getName());

    private final String dataFolder;

    public DataController(Config config) {
        dataFolder = config.get("app.data.folder").asString().get();
    }

    /**
     * A service registers itself by updating the routing rules.
     *
     * @param rules the routing rules.
     */
    @Override
    public void update(Routing.Rules rules) {
        rules
            .put("/file/{id}", Handler.create(FileData.class, this::updateFile))
            .get("/file/{id}", this::getFileById);
    }

    /**
     * Get file content by ID.
     */
    private void getFileById(ServerRequest request, ServerResponse response) {

        String id = request.path().param("id");

        FileData fileData = new FileData();
        fileData.setId(id);
        fileData.setData(fileToData.get(id));

        response.send(fileData);
    }

    private static <T> T processErrors(Throwable ex, ServerRequest request, ServerResponse response) {

        LOGGER.log(Level.FINE, "Internal error", ex);
        FileData jsonError = new FileData();
        jsonError.setData("Internal error");
        response.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(jsonError);

        return null;
    }

    /**
     * Update file content by ID.
     */
    private void updateFile(ServerRequest request, ServerResponse response, FileData fileData) {
        if (fileData.getData() == null) {
            FileData jsonError = new FileData();
            jsonError.setData("No greeting provided");
            response.status(Http.Status.BAD_REQUEST_400).send(jsonError);
            return;
        }

        String id = request.path().param("id");
        fileToData.put(id, fileData.getData());

        response.status(Http.Status.NO_CONTENT_204).send();
    }
}
