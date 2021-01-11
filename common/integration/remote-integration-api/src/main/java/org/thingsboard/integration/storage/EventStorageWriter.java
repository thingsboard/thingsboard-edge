/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.integration.storage;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.server.gen.integration.UplinkMsg;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

@Data
@Slf4j
class EventStorageWriter {

    private EventStorageFiles files;
    private FileEventStorageSettings settings;

    private File currentFile;
    private long currentFileRecordsCount;
    private BufferedWriter bufferedWriter;
    private boolean newRecordAfterFlush;

    EventStorageWriter(EventStorageFiles files, FileEventStorageSettings settings) {
        this.files = files;
        this.settings = settings;

        currentFile = files.getDataFiles().get(files.getDataFiles().size() - 1);
        currentFileRecordsCount = getNumberOfRecordsInFile(currentFile);
    }

    void write(UplinkMsg msg, IntegrationCallback<Void> callback) {
        newRecordAfterFlush = true;
        if (isFileFull(currentFileRecordsCount)) {
            if (log.isDebugEnabled()) {
                log.debug("File [{}] is full with [{}] records", currentFile.getName(), currentFileRecordsCount);
            }
            try {
                log.debug("Created new data file: {}", currentFile.getName());
                currentFile = createDataFile(Long.toString(System.currentTimeMillis()));
            } catch (IOException e) {
                log.error("Failed to create a new file!", e);
                if (callback != null) {
                    callback.onError(e);
                }
                return;
            }
            if (files.getDataFiles().size() == settings.getMaxFileCount()) {
                File firstFile = files.getDataFiles().get(0);
                if (firstFile.delete()) {
                    files.getDataFiles().remove(0);
                }
                log.info("Cleanup old data file: {}!", firstFile.getName());
            }
            files.getDataFiles().add(currentFile);
            currentFileRecordsCount = 0;
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
            } catch (IOException e) {
                log.warn("Failed to close buffered writer!", e);
                if (callback != null) {
                    callback.onError(e);
                }
                return;
            }
            bufferedWriter = null;
        }
        String encoded = Base64.getEncoder().encodeToString(msg.toByteArray());
        try {
            BufferedWriter writer = getOrInitBufferedWriter(currentFile);
            writer.write(encoded);
            writer.newLine();
            log.debug("Record written to: [{}:{}]", currentFile.getName(), currentFileRecordsCount);
            currentFileRecordsCount++;
            if (currentFileRecordsCount % settings.getMaxRecordsBetweenFsync() == 0) {
                log.debug("Executing flush of the full pack!");
                writer.flush();
                newRecordAfterFlush = false;
            }
        } catch (IOException e) {
            log.warn("Failed to update data file![{}]", currentFile.getName(), e);
            if (callback != null) {
                callback.onError(e);
            }
            return;
        }
        if (callback != null) {
            callback.onSuccess(null);
        }
    }

    void flushIfNeeded() {
        if (newRecordAfterFlush) {
            if (bufferedWriter != null) {
                try {
                    log.debug("Executing flush of the temporary pack!");
                    bufferedWriter.flush();
                    newRecordAfterFlush = false;
                } catch (IOException e) {
                    log.warn("Failed to update data file![{}]", currentFile.getName(), e);
                }
            }
        }
    }

    private BufferedWriter getOrInitBufferedWriter(File file) {
        try {
            if (bufferedWriter == null) {
                bufferedWriter = new BufferedWriter(new FileWriter(file, true));
            }
            return bufferedWriter;
        } catch (IOException e) {
            log.error("Failed to initialize buffered writer!", e);
            throw new RuntimeException("Failed to initialize buffered writer!", e);
        }
    }

    private File createDataFile(String fileName) throws IOException {
        return Files.createFile(Paths.get(settings.getDataFolderPath() + "/data_" + fileName + ".txt")).toFile();
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
        return currentFileSize >= settings.getMaxRecordsPerFile();
    }

    void destroy() throws IOException {
        if (bufferedWriter != null) {
            bufferedWriter.close();
        }
    }
}
