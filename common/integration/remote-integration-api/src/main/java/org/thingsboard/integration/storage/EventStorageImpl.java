/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.integration.storage;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.gen.integration.UplinkMsg;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
@Data
// TODO: 6/13/19 implement state storage
public class EventStorageImpl implements EventStorage {

    @Value("${integrations.remote.data_folder_path}")
    private String dataFolderPath;
    @Value("${integrations.remote.max_records_per_file}")
    private int maxRecordsPerFile;
    @Value("${integrations.remote.max_records_between_fsync}")
    private int maxRecordsBetweenFsync;
    @Value("${integrations.remote.max_file_count}")
    private int maxFileCount;
    @Value("${integrations.remote.read_interval}")
    private long readInterval;
    @Value("${integrations.remote.max_read_records_count}")
    private int maxReadRecordsCount;

    private final ReentrantLock readLock = new ReentrantLock();
    private final ReentrantLock writeLock = new ReentrantLock();

    private ExecutorService readExecutor;
    private ExecutorService writeExecutor;
    private ScheduledExecutorService scheduler;
    private List<File> files;
    private long currentFileWriteSize;
    private int currentLineToRead;

    @PostConstruct
    public void init() {
        readExecutor = Executors.newCachedThreadPool(); // TODO: 6/13/19 use?
        writeExecutor = Executors.newCachedThreadPool(); // TODO: 6/13/19 use?
        scheduler = Executors.newSingleThreadScheduledExecutor(); // TODO: 6/13/19 use?
        initDataFolderIfNotExist();
        files = initDataFiles();
    }

    @PreDestroy
    public void stop() {
        if (readExecutor != null) {
            readExecutor.shutdownNow();
        }
        if (writeExecutor != null) {
            writeExecutor.shutdownNow();
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    // TODO: 6/13/19 validate logic
    @Override
    public void write(UplinkMsg msg) {
        writeLock.lock();
        try {
            File lastFile = files.get(files.size() - 1);
            long recordsCount = getNumberOfRecordsInFile(lastFile);
            if (isFileFull(recordsCount)) {
                if (log.isDebugEnabled()) {
                    log.debug("Records count: [{}] exceeds the allowed value![{}]", recordsCount, maxRecordsPerFile);
                }
                lastFile = createNewFile();
                files.add(lastFile);
                currentFileWriteSize = 0;
            }
            String encoded = Base64.getEncoder().encodeToString(msg.toByteArray());
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(lastFile, true))) {
                writer.write(encoded);
                currentFileWriteSize++;
                if (currentFileWriteSize % maxRecordsBetweenFsync == 0) {
                    writer.flush();
                }
            } catch (IOException e) {
                log.warn("Failed to...!");
            }
        } finally {
            writeLock.unlock();
        }
    }

    // TODO: 6/13/19 validate logic
    @Override
    public List<UplinkMsg> readCurrentBatch() {
        readLock.lock();
        try {
            List<UplinkMsg> uplinkMsgs = new ArrayList<>();

            int currentFileIdx = 0;
            int recordsToRead = maxReadRecordsCount;

            while (recordsToRead > 0) {
                File currentFile = files.get(currentFileIdx);
                try (BufferedReader br = Files.newBufferedReader(currentFile.toPath())) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (currentLineToRead != 0) {
                            currentLineToRead--;
                            continue;
                        }
                        if (recordsToRead == 0) {
                            break;
                        }
                        uplinkMsgs.add(UplinkMsg.parseFrom(Base64.getDecoder().decode(line)));
                        currentLineToRead++;
                        recordsToRead--;
                    }
                    if (line == null) {
                        currentLineToRead = 0;
                    }
                } catch (IOException e) {
                    log.warn("Failed to ...!");
                }
                currentFileIdx++;
            }
            return uplinkMsgs;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void discardCurrentBatch() {
        int currentFileIdx = 0;
        File currentFile = files.get(currentFileIdx);

        // TODO: 6/13/19 implement

    }

    private void initDataFolderIfNotExist() {
        Path path = Paths.get(dataFolderPath);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create data folder!", e);
            }
        }
    }

    private List<File> initDataFiles() {
        try (Stream<Path> paths = Files.walk(Paths.get(dataFolderPath))) {
            List<File> files = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith("data_"))
                    .map(Path::toFile).collect(Collectors.toList());
            Collections.sort(files);
            if (files.size() == 0) {
                files.add(createNewFile());
            }
            return files;
        } catch (IOException e) {
            throw new RuntimeException("", e);
        }
    }

    private File createNewFile() {
        int filesCount = files.size();
        if (filesCount == maxFileCount) {
            log.error("Files count: [{}] exceeds the allowed value![{}]", filesCount, maxFileCount);
            throw new RuntimeException("Files count exceeds the allowed value!");
        }
        Path newFilePath = Paths.get(dataFolderPath + "/data_" + Long.toString(System.currentTimeMillis()) + ".txt");
        try {
            return Files.createFile(newFilePath).toFile();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create a new file!", e);
        }
    }

    private long getNumberOfRecordsInFile(File file) {
        if (currentFileWriteSize == 0) {
            try {
                currentFileWriteSize = Files.lines(Paths.get(file.toURI())).count();
            } catch (IOException e) {
                log.error("...");
            }
        }
        return currentFileWriteSize;
    }

    private boolean isFileFull(long currentFileSize) {
        return currentFileSize >= maxRecordsPerFile;
    }

    private boolean deleteFile(File file) {
        return file.delete();
    }
}
