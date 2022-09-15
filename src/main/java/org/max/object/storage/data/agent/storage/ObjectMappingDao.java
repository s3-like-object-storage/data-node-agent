package org.max.object.storage.data.agent.storage;

import io.helidon.dbclient.DbClient;
import java.lang.invoke.MethodHandles;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectMappingDao {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final DbClient dbClient;

    public ObjectMappingDao(DbClient dbClient) {
        this.dbClient = dbClient;
    }

    public CompletionStage<Void> insertDbMapping(BinaryDataDetails dataDetails) {
        return dbClient.execute(exec ->
                             exec.insert("INSERT INTO object_mapping (id, file_name, offset, size) VALUES(?, ?, ?, ?)",
                                         dataDetails.id.toString(), dataDetails.fileName, dataDetails.offset, dataDetails.size)).
            thenAccept(notUsedCount -> LOG.info("object_mapping inserted for ID {}", dataDetails.id));
    }

    public CompletionStage<BinaryDataDetails> getMappingById(UUID id) {
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
}
