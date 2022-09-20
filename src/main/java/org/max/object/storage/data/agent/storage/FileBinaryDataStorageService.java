package org.max.object.storage.data.agent.storage;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.max.object.storage.data.agent.domain.BinaryDataStorageService;
import org.max.object.storage.data.agent.storage.file.AppendFile;
import org.max.object.storage.data.agent.storage.sqlite.ObjectMappingDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileBinaryDataStorageService implements BinaryDataStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ObjectMappingDao dao;

    private final AppendFile appendFile;

    public FileBinaryDataStorageService(ObjectMappingDao dao, AppendFile appendFile) {
        this.dao = dao;
        this.appendFile = appendFile;
    }


    @Override
    public CompletionStage<UUID> saveData(byte[] binaryData) {
        return CompletableFuture.completedFuture(UUID.randomUUID()).
            thenCompose(id -> CompletableFuture.supplyAsync(() -> {
                try {
                    appendFile.saveBinaryData(binaryData);
                    return id;
                }
                catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
            })).
            thenCompose(id -> dao.insertDbMapping(
                new BinaryDataDetails(id, appendFile.fileNameAsStr(), appendFile.offset(), binaryData.length))).
            thenApply(uuidFromDb -> {
                appendFile.addToOffset(binaryData.length);
                LOG.info("ID: {}, size: {} bytes, append file: {}", uuidFromDb, binaryData.length, appendFile.fileNameAsStr());
                return uuidFromDb;
            });
    }

    @Override
    public CompletionStage<Optional<byte[]>> getBinaryData(UUID id) {
        return dao.getMappingById(id).
            thenCompose(binaryDetails -> CompletableFuture.supplyAsync(
                () -> FileBinaryDataStorageService.readChunkFromFile(binaryDetails)));
    }

    private static Optional<byte[]> readChunkFromFile(BinaryDataDetails details) {
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
}
