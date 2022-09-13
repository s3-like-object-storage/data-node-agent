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

    private final File appendFileName;
    private final RandomAccessFile appendFile;

    private long appendOffset;

    private final Map<UUID, BinaryDataDetails> binaryDetailsById = new HashMap<>();

    public FileDataDataStorageService(Config config) {
        String dataFolder = config.get("binary.storage.folder").asString().get();

        // TODO: right now we just generate new file for each run
        appendFileName = Path.of(dataFolder, "file-" + UUID.randomUUID()).toFile();

        try {
            appendFile = new RandomAccessFile(appendFileName, "rw");
        }
        catch (Exception ex) {
            throw new ExceptionInInitializerError("Can't open append file " + appendFileName + " for 'rwd'");
        }
    }


    @Override
    public UUID saveData(byte[] binaryData) {
        final UUID id = UUID.randomUUID();

        try {
            appendFile.seek(appendOffset);
            appendFile.write(binaryData);
            // IMPORTANT: use sync() here to flush all in-memory buffers inside OS kernel
            // https://stackoverflow.com/questions/7550190/how-do-i-flush-a-randomaccessfile-java
            appendFile.getFD().sync();

            binaryDetailsById.put(id, new BinaryDataDetails(appendFileName.toString(), appendOffset, binaryData.length));

            appendOffset += binaryData.length;
        }
        catch (IOException ioEx) {
            throw new IllegalStateException(ioEx);
        }

        LOG.info("ID: {}, size: {} bytes, append file: {}", id, binaryData.length, appendFileName.getAbsolutePath());

        return id;
    }

    @Override
    public Optional<byte[]> getBinaryData(UUID id) {

        BinaryDataDetails details = binaryDetailsById.get(id);
        if (details == null) {
            return Optional.empty();
        }

        try {
            try (RandomAccessFile readFile = new RandomAccessFile(details.fileName, "r")) {
                byte[] buf = new byte[details.size];
                readFile.seek(details.offset);
                readFile.read(buf);
                return Optional.of(buf);
            }
        }
        catch (IOException ioEx) {
            throw new IllegalStateException(ioEx);
        }
    }

    private static final class BinaryDataDetails {

        final String fileName;

        final long offset;
        final int size;

        public BinaryDataDetails(String fileName, long offset, int size) {
            this.fileName = fileName;
            this.offset = offset;
            this.size = size;
        }
    }
}
