package org.max.object.storage.data.agent.storage;

import java.util.UUID;

public class BinaryDataDetails {

    public final UUID id;

    public final String fileName;

    public final long offset;

    public final int size;

    public BinaryDataDetails(UUID id, String fileName, long offset, int size) {
        this.id = id;
        this.fileName = fileName;
        this.offset = offset;
        this.size = size;
    }
}
