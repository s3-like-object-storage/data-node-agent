package org.max.object.storage.data.agent.storage;

import io.helidon.config.Config;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.max.object.storage.data.agent.domain.BinaryDataStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileDataDataStorageService implements BinaryDataStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final RandomAccessFile appendBaseFile;

    private long offset;

    private final Map<UUID, BinaryDataDetails> binaryDetailsById = new HashMap<>();

    public FileDataDataStorageService(Config config) {
        String dataFolder = config.get("binary.storage.folder").asString().get();

        final File appendFile = Path.of(dataFolder, "file1").toFile();

        try {
            appendBaseFile = new RandomAccessFile(appendFile, "rwd");
        }
        catch (Exception ex) {
            throw new ExceptionInInitializerError("Can't open file " + appendFile + "for rw");
        }
    }


    @Override
    public UUID saveData(byte[] binaryData) {
        UUID id = UUID.randomUUID();

        try {
            appendBaseFile.seek(offset);
            appendBaseFile.write(binaryData);

            binaryDetailsById.put(id, new BinaryDataDetails(offset, binaryData.length));

            offset += binaryData.length;
        }
        catch (IOException ioEx) {
            throw new IllegalStateException(ioEx);
        }

        LOG.info("Binary data {} bytes save with ID {}", binaryData.length, id);

        return id;
    }

    @Override
    public Optional<byte[]> getBinaryData(UUID id) {

        BinaryDataDetails details = binaryDetailsById.get(id);
        if (details == null) {
            return Optional.empty();
        }

        try {
            byte[] buf = new byte[details.size];
            appendBaseFile.seek(details.offset);
            appendBaseFile.read(buf);
            return Optional.of(buf);
        }
        catch (IOException ioEx) {
            throw new IllegalStateException(ioEx);
        }
    }

    private static final class BinaryDataDetails {
        final long offset;
        final int size;

        BinaryDataDetails(long offset, int size) {
            this.offset = offset;
            this.size = size;
        }
    }
}
