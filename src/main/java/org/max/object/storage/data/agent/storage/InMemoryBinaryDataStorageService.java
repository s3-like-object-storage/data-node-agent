package org.max.object.storage.data.agent.storage;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.max.object.storage.data.agent.domain.BinaryDataStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryBinaryDataStorageService implements BinaryDataStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Map<UUID, byte[]> fileData = new HashMap<>();


    @Override
    public CompletionStage<UUID> saveData(byte[] binaryData) {
        UUID id = UUID.randomUUID();
        fileData.put(id, binaryData);

        LOG.info("Binary data {} bytes save with ID {}", binaryData.length, id);

        return CompletableFuture.completedFuture(id);
    }

    @Override
    public CompletionStage<Optional<byte[]>> getBinaryData(UUID id) {
        return  CompletableFuture.completedFuture(Optional.ofNullable(fileData.get(id)));
    }
}
