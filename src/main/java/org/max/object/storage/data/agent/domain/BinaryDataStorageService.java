package org.max.object.storage.data.agent.domain;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface BinaryDataStorageService {

    CompletionStage<UUID> saveData(byte[] binaryData);

    CompletionStage<Optional<byte[]>> getBinaryData(UUID id);
}
