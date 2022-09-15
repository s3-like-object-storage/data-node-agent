package org.max.object.storage.data.agent.api;

import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.max.object.storage.data.agent.domain.BinaryDataStorageService;

/**
 * Store file data in local file system.
 */
public class DataController implements Service {

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
            .post("/", this::uploadData)
            .get("/{id}", this::getDataById);
    }

    /**
     * Get binary data content by ID.
     */
    private void getDataById(ServerRequest request, ServerResponse response) {
        CompletableFuture.completedFuture(UUID.fromString(request.path().param("id"))).
            thenCompose(storageService::getBinaryData).
            thenCompose(maybeBinaryData -> {
                if (maybeBinaryData.isEmpty()) {
                    return response.send(Http.Status.NOT_FOUND_404);
                }
                response.headers().contentType(MediaType.APPLICATION_OCTET_STREAM);
                return response.status(Http.Status.OK_200).send(maybeBinaryData.get());
            });
    }

    /**
     * Upload binary data
     */
    private void uploadData(ServerRequest request, ServerResponse response) {
        request.content().as(byte[].class).
            thenCompose(storageService::saveData).
            thenCompose(generatedId -> {
                response.headers().location(URI.create("data/" + generatedId.toString()));
                return response.status(Http.Status.CREATED_201).send();
            });
    }
}
