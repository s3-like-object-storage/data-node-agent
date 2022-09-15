package org.max.object.storage.data.agent.storage.sqlite;

import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbRow;
import java.lang.invoke.MethodHandles;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import org.max.object.storage.data.agent.storage.BinaryDataDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectMappingDao {

    private static final String INSERT_OBJECT_MAPPING_SQL =
        "INSERT INTO object_mapping (id, file_name, offset, size) VALUES(?, ?, ?, ?)";

    private static final String SELECT_OBJECT_MAPPING_BY_ID =
        "SELECT id, file_name, offset, size FROM object_mapping WHERE id = ?";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final DbClient dbClient;

    public ObjectMappingDao(DbClient dbClient) {
        this.dbClient = dbClient;
    }

    public CompletionStage<UUID> insertDbMapping(BinaryDataDetails dataDetails) {
        return dbClient.execute(exec ->
                                    exec.insert(INSERT_OBJECT_MAPPING_SQL, dataDetails.id.toString(), dataDetails.fileName,
                                                dataDetails.offset, dataDetails.size)).
            thenApply( insertedCount -> {
                LOG.info("object_mapping inserted for ID {}", dataDetails.id);
                return dataDetails.id;
            });
    }

    public CompletionStage<BinaryDataDetails> getMappingById(UUID id) {
        return dbClient.execute(exec -> exec.createGet(SELECT_OBJECT_MAPPING_BY_ID).
                addParam(id).
                execute())
            .thenApply(maybeRow -> maybeRow.map(ObjectMappingDao::toDto).
                orElse(null));
    }

    private static BinaryDataDetails toDto(DbRow dbRow) {
        UUID idFromDb = UUID.fromString(dbRow.column("id").as(String.class));
        String fileName = dbRow.column("file_name").as(String.class);
        long offset = (long) dbRow.column("offset").as(Integer.class);
        Integer size = dbRow.column("size").as(Integer.class);
        return new BinaryDataDetails(idFromDb, fileName, offset, size);
    }
}
