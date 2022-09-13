package org.max.object.storage.data.agent.storage;

import io.helidon.config.Config;
import org.max.object.storage.data.agent.domain.BinaryDataStorageService;

public class DataStorageServiceFactory {

    public static BinaryDataStorageService newInstance(Config config) {

        final String dataStorageType = config.get("binary.storage.type").asString().orElse("MEMORY").trim();

        return switch (dataStorageType) {
            case "MEMORY" -> new InMemoryDataDataStorageService();
            case "FILE" -> new FileDataDataStorageService(config);
            default -> throw new IllegalStateException("Undefined binary storage type detected: " + dataStorageType);
        };
    }

}
