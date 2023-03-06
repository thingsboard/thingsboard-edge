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
                currentFile = createDataFile(Long.toString(System.currentTimeMillis()));
                log.debug("Created new data file: {}", currentFile.getName());
            } catch (IOException e) {
                log.error("Failed to create a new file!", e);
                if (callback != null) {
                    callback.onError(e);
                }
                return;
            }
            while (files.getDataFiles().size() >= settings.getMaxFileCount()) {
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
