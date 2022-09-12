package org.max.object.storage.data.agent.domain;


import io.helidon.config.Config;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class BinaryDataStorageService {

    private final Map<UUID, byte[]> fileData = new HashMap<>();

    private final String dataFolder;

    public BinaryDataStorageService(Config config) {
        dataFolder = config.get("app.data.folder").asString().get();
    }

    public UUID saveData(byte[] binaryData) {
        UUID id = UUID.randomUUID();
        fileData.put(id, binaryData);
        return id;
    }

    public Optional<byte[]> getBinaryData(UUID id) {
        return Optional.ofNullable(fileData.get(id));
    }
}
