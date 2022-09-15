package org.max.object.storage.data.agent.storage.sqlite;

import io.helidon.dbclient.DbClient;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppendFileDao {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final DbClient dbClient;

    public AppendFileDao(DbClient dbClient) {
        this.dbClient = dbClient;
    }

    public CompletionStage<String> findLastAppendFilePath(int appenderId) {
        return dbClient.execute(exec -> exec.createGet("SELECT file_path FROM append_file WHERE id = ?").
                addParam(appenderId).
                execute())
            .thenApply(maybeRow -> maybeRow.map(dbRow -> dbRow.column("file_path").as(String.class)).
                orElse(null));
    }

    public CompletionStage<String> insertAppendFile(String filePath) {
        return dbClient.execute(exec -> exec.createInsert("INSERT INTO append_file(id, file_path) VALUES(1, ?)").
                addParam(filePath).
                execute())
            .thenApply(insertedRowsCount -> {
                LOG.info("append_file table updated for id  1 and path {}", filePath);
                return filePath;
            });
    }
}
