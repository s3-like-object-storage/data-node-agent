package org.max.object.storage.data.agent.api;

import io.helidon.common.http.Http;
import io.helidon.common.reactive.Single;
import io.helidon.config.Config;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.max.object.storage.data.agent.domain.BinaryDataStorageService;
import org.max.object.storage.data.agent.domain.FileData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Store file data in local file system.
 */
public class DataController implements Service {

    private static final Logger LOG = LoggerFactory.getLogger(DataController.class.getName());

    private final BinaryDataStorageService storageService;

    public DataController(BinaryDataStorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * A service registers itself by updating the routing rules.
     *
     * @param rules the routing rules.
     */
    @Override
    public void update(Routing.Rules rules) {
        rules
            .post("/file", this::uploadFile)
            .get("/file/{id}", this::getFileById);
    }

    /**
     * Get file content by ID.
     */
    private void getFileById(ServerRequest request, ServerResponse response) {

        UUID id = UUID.fromString(request.path().param("id"));

        Optional<byte[]> binaryData = storageService.getBinaryData(id);

        if (binaryData.isEmpty()) {
            response.send(Http.Status.NOT_FOUND_404);
            return;
        }

        response.addHeader("Content-Type", "application/octet-stream");

        response.status(Http.Status.OK_200).send(binaryData.get());
    }

    private static <T> T processErrors(Throwable ex, ServerRequest request, ServerResponse response) {
        LOG.error("Internal error", ex);

        FileData jsonError = new FileData();
        jsonError.setData("Internal error");
        response.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(jsonError);

        return null;
    }

    /**
     * Upload binary data
     */
    private void uploadFile(ServerRequest request, ServerResponse response) {

        Single<byte[]> body = request.content().as(byte[].class);

        body.thenAccept(binaryData -> {
            LOG.info("Saving binary data for with size {} bytes", binaryData.length);

            UUID generatedId = storageService.saveData(binaryData);

            LOG.info("New data saved with UUID: {}", generatedId);

            response.addHeader("Location", generatedId.toString()).
                status(Http.Status.CREATED_201).
                send();
        });
    }
}
