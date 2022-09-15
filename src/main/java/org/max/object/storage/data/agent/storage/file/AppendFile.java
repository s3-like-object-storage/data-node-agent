package org.max.object.storage.data.agent.storage.file;

import io.helidon.config.Config;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import org.max.object.storage.data.agent.storage.sqlite.AppendFileDao;
import org.max.object.storage.data.agent.util.ReactiveUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppendFile {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final File appendFileName;
    private final RandomAccessFile appendFile;
    private long appendOffset;

    public AppendFile(File appendFileName, long appendOffset) {
        this.appendFileName = appendFileName;
        this.appendOffset = appendOffset;
        try {
            appendFile = new RandomAccessFile(appendFileName, "rw");
        }
        catch (FileNotFoundException ex) {
            throw new ExceptionInInitializerError("Can't open append file " + appendFileName + " for 'rwd'");
        }
    }

    private static final int APPENDER_ID = 1;

    public static AppendFile create(AppendFileDao appendFileDao, Config config) {

        String dataFolder = Objects.requireNonNull(config.get("binary.storage.folder").asString().get(), "NULL data folder");

        CompletionStage<AppendFile> appendFileFuture = appendFileDao.findLastAppendFilePath(APPENDER_ID).
            thenApply(path -> {
                if (path == null) {
                    File newAppendFile = Path.of(dataFolder, "file-" + UUID.randomUUID()).toFile();
                    LOG.info("New append file generated {}", newAppendFile);
                    ReactiveUtils.waitForStageCompletion(appendFileDao.insertAppendFile(newAppendFile.toString()));
                    return new AppendFile(newAppendFile, 0);
                }
                File fileFromDb = Path.of(path).toFile();
                LOG.info("Existing append file will be used {}", fileFromDb);
                return new AppendFile(fileFromDb, fileFromDb.length());
            });

        return ReactiveUtils.waitForStageCompletion(appendFileFuture);
    }

    public void saveBinaryData(byte[] binaryData) {
        try {
            appendFile.seek(appendOffset);
            appendFile.write(binaryData);
            // IMPORTANT: use sync() here to flush all in-memory buffers inside OS kernel
            // https://stackoverflow.com/questions/7550190/how-do-i-flush-a-randomaccessfile-java
            appendFile.getFD().sync();
        }
        catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public void addToOffset(int length) {
        appendOffset += length;
    }

    public String fileNameAsStr() {
        return appendFileName.toString();
    }

    public long offset() {
        return appendOffset;
    }
}
