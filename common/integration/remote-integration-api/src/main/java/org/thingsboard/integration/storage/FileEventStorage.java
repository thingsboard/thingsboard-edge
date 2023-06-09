/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.server.gen.integration.UplinkMsg;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
@Data
public class FileEventStorage implements EventStorage {

    @Autowired
    private FileEventStorageSettings settings;

    private final ReentrantLock readLock = new ReentrantLock();
    private final ReentrantLock writeLock = new ReentrantLock();

    private List<File> dataFiles;
    private File stateFile;

    private EventStorageWriter storageWriter;
    private EventStorageReader storageReader;

    @PostConstruct
    public void init() {
        initDataFolderIfNotExist();
        EventStorageFiles eventStorageFiles = initDataFiles();
        dataFiles = eventStorageFiles.getDataFiles();
        stateFile = eventStorageFiles.getStateFile();
        storageWriter = new EventStorageWriter(eventStorageFiles, settings);
        storageReader = new EventStorageReader(eventStorageFiles, settings);
    }

    @PreDestroy
    public void destroy() throws IOException {
        storageReader.destroy();
        storageWriter.destroy();
    }

    @Override
    public void write(UplinkMsg msg, IntegrationCallback<Void> callback) {
        writeLock.lock();
        try {
            storageWriter.write(msg, callback);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public List<UplinkMsg> readCurrentBatch() {
        writeLock.lock();
        try {
            storageWriter.flushIfNeeded();
        } finally {
            writeLock.unlock();
        }

        readLock.lock();
        try {
            return storageReader.read();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void discardCurrentBatch() {
        readLock.lock();
        try {
            storageReader.discardBatch();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void sleep() {
        try {
            Thread.sleep(settings.getNoRecordsSleepInterval());
        } catch (InterruptedException e) {
            log.warn("Sleep interrupted!");
        }
    }

    private void initDataFolderIfNotExist() {
        Path path = Paths.get(settings.getDataFolderPath());
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create data folder!", e);
            }
        }
    }

    private EventStorageFiles initDataFiles() {
        CopyOnWriteArrayList<File> dataFiles = new CopyOnWriteArrayList<>();
        File stateFile = null;

        File dir = new File(settings.getDataFolderPath());
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (file.getName().startsWith("data_")) {
                    dataFiles.add(file);
                } else if (file.getName().startsWith("state_")) {
                    stateFile = file;
                }
            }
            Collections.sort(dataFiles);
            if (dataFiles.size() == 0) {
                dataFiles.add(createNewDataFile());
            }

            if (stateFile == null) {
                stateFile = createFile("/state_", "file");
            }
            return new EventStorageFiles(stateFile, dataFiles);
        } else {
            log.error("[{}] The specified path is not referred to the directory!", settings.getDataFolderPath());
            throw new RuntimeException("The specified path is not referred to the directory! " + settings.getDataFolderPath());
        }
    }

    private File createNewDataFile() {
        return createFile("/data_", Long.toString(System.currentTimeMillis()));
    }

    private File createFile(String prefix, String fileName) {
        Path filePath = Paths.get(settings.getDataFolderPath() + prefix + fileName + ".txt");
        try {
            return Files.createFile(filePath).toFile();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create a new file!", e);
        }
    }

}
