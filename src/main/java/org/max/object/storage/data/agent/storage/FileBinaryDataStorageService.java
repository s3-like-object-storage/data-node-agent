package org.max.object.storage.data.agent.storage;

import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.max.object.storage.data.agent.domain.BinaryDataStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileBinaryDataStorageService implements BinaryDataStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final File appendFileName;
    private final RandomAccessFile appendFile;

    private long appendOffset;

    private final ObjectMappingDao dao;

    public FileBinaryDataStorageService(ObjectMappingDao dao, Config config) {

        this.dao = dao;

        String dataFolder = Objects.requireNonNull(config.get("binary.storage.folder").asString().get(), "NULL data folder");

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
    public CompletionStage<UUID> saveData(byte[] binaryData) {
        return CompletableFuture.completedFuture(UUID.randomUUID()).
            thenApply(id -> {
                try {
                    appendFile.seek(appendOffset);
                    appendFile.write(binaryData);
                    // IMPORTANT: use sync() here to flush all in-memory buffers inside OS kernel
                    // https://stackoverflow.com/questions/7550190/how-do-i-flush-a-randomaccessfile-java
                    appendFile.getFD().sync();
                    return id;
                }
                catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
            }).
            thenCompose( id -> dao.insertDbMapping(new BinaryDataDetails(id, appendFileName.toString(), appendOffset, binaryData.length))).
            thenApply(uuidFromDb -> {
                appendOffset += binaryData.length;
                LOG.info("ID: {}, size: {} bytes, append file: {}", uuidFromDb, binaryData.length, appendFileName.getAbsolutePath());
                return uuidFromDb;
            });
    }

    @Override
    public CompletionStage<Optional<byte[]>> getBinaryData(UUID id) {
        return dao.getMappingById(id).
            thenApply(details -> {
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
            });
    }
}
