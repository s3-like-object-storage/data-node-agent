package org.max.object.storage.data.agent.storage;

import io.helidon.config.Config;
import org.max.object.storage.data.agent.domain.BinaryDataStorageService;
import org.max.object.storage.data.agent.storage.file.AppendFile;
import org.max.object.storage.data.agent.storage.sqlite.ObjectMappingDao;

public class DataStorageServiceFactory {

    public static BinaryDataStorageService newInstance(ObjectMappingDao dao, AppendFile appendFile, Config config) {

        final String dataStorageType = config.get("binary.storage.type").asString().orElse("MEMORY").trim();

        return switch (dataStorageType) {
            case "MEMORY" -> new InMemoryBinaryDataStorageService();
            case "FILE" -> new FileBinaryDataStorageService(dao, appendFile);
            default -> throw new IllegalStateException("Undefined binary storage type detected: " + dataStorageType);
        };
    }
}
