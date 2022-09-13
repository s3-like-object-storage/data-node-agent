package org.max.object.storage.data.agent.storage;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.max.object.storage.data.agent.domain.BinaryDataStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryBinaryDataStorageService implements BinaryDataStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Map<UUID, byte[]> fileData = new HashMap<>();


    @Override
    public UUID saveData(byte[] binaryData) {
        UUID id = UUID.randomUUID();
        fileData.put(id, binaryData);

        LOG.info("Binary data {} bytes save with ID {}", binaryData.length, id);

        return id;
    }

    @Override
    public Optional<byte[]> getBinaryData(UUID id) {
        return Optional.ofNullable(fileData.get(id));
    }
}
