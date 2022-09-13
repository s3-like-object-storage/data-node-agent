package org.max.object.storage.data.agent.domain;

import java.util.Optional;
import java.util.UUID;

public interface BinaryDataStorageService {

    UUID saveData(byte[] binaryData);

    Optional<byte[]> getBinaryData(UUID id);
}
