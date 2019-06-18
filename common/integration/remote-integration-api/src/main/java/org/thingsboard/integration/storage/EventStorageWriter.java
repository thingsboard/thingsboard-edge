package org.thingsboard.integration.storage;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.gen.integration.UplinkMsg;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

@Data
@Slf4j
public class EventStorageWriter {

    private String dataFolderPath;
    private List<File> dataFiles;
    private int maxRecordsPerFile;
    private int maxRecordsBetweenFsync;
    private BufferedWriter bufferedWriter;
    private long currentFileRecordsCount;

    public EventStorageWriter(String dataFolderPath, List<File> dataFiles, int maxRecordsPerFile, int maxRecordsBetweenFsync) {
        this.dataFolderPath = dataFolderPath;
        this.dataFiles = dataFiles;
        this.maxRecordsPerFile = maxRecordsPerFile;
        this.maxRecordsBetweenFsync = maxRecordsBetweenFsync;
        this.bufferedWriter = getOrInitBufferedWriter(dataFiles.get(dataFiles.size() - 1));
    }

    public BufferedWriter getOrInitBufferedWriter(File file) {
        try {
            if (bufferedWriter == null) {
                return new BufferedWriter(new FileWriter(file, true));
            } else {
                return bufferedWriter;
            }
        } catch (IOException e) {
            log.error("Failed to initialize buffered writer!", e);
            throw new RuntimeException("Failed to initialize buffered writer!", e);
        }
    }

    public void write(UplinkMsg msg) {
        int index = dataFiles.size() - 1;
        File lastFile = dataFiles.get(index);
        long recordsCount = getNumberOfRecordsInFile(lastFile);
        if (isFileFull(recordsCount)) {
            if (log.isDebugEnabled()) {
                log.debug("Records count: [{}] exceeds the allowed value![{}]", recordsCount, maxRecordsPerFile);
            }
            lastFile = createDataFile(Long.toString(System.currentTimeMillis()));
            dataFiles.add(index + 1, lastFile);
            currentFileRecordsCount = 0;
            bufferedWriter = null;
        }
        String encoded = Base64.getEncoder().encodeToString(msg.toByteArray());
        try (BufferedWriter writer = getOrInitBufferedWriter(lastFile)) {
            writer.write(encoded);
            writer.newLine();
            currentFileRecordsCount++;
            if (currentFileRecordsCount % maxRecordsBetweenFsync == 0) {
                writer.flush();
            }
        } catch (IOException e) {
            log.warn("Failed to update data file![{}]", lastFile.getName(), e);
        }
    }

    private File createDataFile(String fileName) {
        Path filePath = Paths.get(dataFolderPath + "/data_" + fileName + ".txt");
        try {
            return Files.createFile(filePath).toFile();
        } catch (IOException e) {
            log.error("Failed to create a new file!", e);
            throw new RuntimeException("Failed to create a new file!", e);
        }
    }

    private long getNumberOfRecordsInFile(File file) {
        if (currentFileRecordsCount == 0) {
            try {
                currentFileRecordsCount = Files.lines(Paths.get(file.toURI())).count();
            } catch (IOException e) {
                log.warn("Could not get the records count from the file![{}]", file.getName());
            }
        }
        return currentFileRecordsCount;
    }

    private boolean isFileFull(long currentFileSize) {
        return currentFileSize >= maxRecordsPerFile;
    }

}
