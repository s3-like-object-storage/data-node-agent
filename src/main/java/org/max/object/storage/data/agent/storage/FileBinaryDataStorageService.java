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
import java.util.concurrent.CompletionStage;
import org.max.object.storage.data.agent.domain.BinaryDataStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileBinaryDataStorageService implements BinaryDataStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final File appendFileName;
    private final RandomAccessFile appendFile;

    private long appendOffset;

    private final DbClient dbClient;

    public FileBinaryDataStorageService(DbClient dbClient, Config config) {

        this.dbClient = dbClient;

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
    public UUID saveData(byte[] binaryData) {
        final UUID id = UUID.randomUUID();

        try {
            appendFile.seek(appendOffset);
            appendFile.write(binaryData);
            // IMPORTANT: use sync() here to flush all in-memory buffers inside OS kernel
            // https://stackoverflow.com/questions/7550190/how-do-i-flush-a-randomaccessfile-java
            appendFile.getFD().sync();

            insertDbMapping(new BinaryDataDetails(id, appendFileName.toString(), appendOffset, binaryData.length));

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
        try {
            BinaryDataDetails details = getMappingById(id).toCompletableFuture().get();
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
        catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    private void insertDbMapping(BinaryDataDetails dataDetails) {
        dbClient.execute(exec ->
                             exec.insert("INSERT INTO object_mapping (id, file_name, offset, size) VALUES(?, ?, ?, ?)",
                                         dataDetails.id.toString(), dataDetails.fileName, dataDetails.offset, dataDetails.size)).
            thenAccept(notUsedCount -> LOG.info("object_mapping inserted for ID {}", dataDetails.id));
    }


    private CompletionStage<BinaryDataDetails> getMappingById(UUID id) {
        return dbClient.execute(exec -> exec.createGet("SELECT id, file_name, offset, size FROM object_mapping WHERE id = ?").
                addParam(id).
                execute())
            .thenApply(maybeRow -> maybeRow.map(dbRow -> {
                UUID idFromDb = UUID.fromString(dbRow.column("id").as(String.class));
                String fileName = dbRow.column("file_name").as(String.class);
                long offset = (long)dbRow.column("offset").as(Integer.class);
                Integer size = dbRow.column("size").as(Integer.class);
                return new BinaryDataDetails(idFromDb, fileName, offset, size);
            }).orElseThrow());
    }


    private static final class BinaryDataDetails {

        final UUID id;

        final String fileName;

        final long offset;
        final int size;

        BinaryDataDetails(UUID id, String fileName, long offset, int size) {
            this.id = id;
            this.fileName = fileName;
            this.offset = offset;
            this.size = size;
        }
    }
}
