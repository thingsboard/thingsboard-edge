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
import org.thingsboard.integration.api.IntegrationCallback;
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
    private int maxFileCount;
    private int maxRecordsPerFile;
    private int maxRecordsBetweenFsync;
    private BufferedWriter bufferedWriter;
    private long currentFileRecordsCount;

    public EventStorageWriter(String dataFolderPath, List<File> dataFiles, int maxFileCount, int maxRecordsPerFile, int maxRecordsBetweenFsync) {
        this.dataFolderPath = dataFolderPath;
        this.dataFiles = dataFiles;
        this.maxFileCount = maxFileCount;
        this.maxRecordsPerFile = maxRecordsPerFile;
        this.maxRecordsBetweenFsync = maxRecordsBetweenFsync;
        this.bufferedWriter = getOrInitBufferedWriter(dataFiles.get(dataFiles.size() - 1));
    }

    public BufferedWriter getOrInitBufferedWriter(File file) {
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

    public void write(UplinkMsg msg, IntegrationCallback<Void> callback) {
        File lastFile = dataFiles.get(dataFiles.size() - 1);
        long recordsCount = getNumberOfRecordsInFile(lastFile);
        if (isFileFull(recordsCount)) {
            if (log.isDebugEnabled()) {
                log.debug("Records count: [{}] exceeds the allowed value![{}]", recordsCount, maxRecordsPerFile);
            }
            try {
                lastFile = createDataFile(Long.toString(System.currentTimeMillis()));
            } catch (IOException e) {
                log.error("Failed to create a new file!", e);
                if (callback != null) {
                    callback.onError(e);
                }
                return;
            }
            if (dataFiles.size() == maxFileCount) {
                File firstFile = dataFiles.get(0);
                if (firstFile.delete()) {
                    dataFiles.remove(0);
                }
            }
            dataFiles.add(lastFile);
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
            BufferedWriter writer = getOrInitBufferedWriter(lastFile);
            writer.write(encoded);
            writer.newLine();
            currentFileRecordsCount++;
            if (currentFileRecordsCount % maxRecordsBetweenFsync == 0) {
                writer.flush();
            }
        } catch (IOException e) {
            log.warn("Failed to update data file![{}]", lastFile.getName(), e);
            if (callback != null) {
                callback.onError(e);
            }
            return;
        }
        if (callback != null) {
            callback.onSuccess(null);
        }
    }

    private File createDataFile(String fileName) throws IOException {
        Path filePath = Paths.get(dataFolderPath + "/data_" + fileName + ".txt");
        return Files.createFile(filePath).toFile();
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

    public void destroy() throws IOException {
        if (bufferedWriter != null) {
            bufferedWriter.close();
        }
    }

}
