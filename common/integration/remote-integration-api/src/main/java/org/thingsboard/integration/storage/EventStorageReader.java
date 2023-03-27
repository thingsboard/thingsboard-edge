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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.gen.integration.UplinkMsg;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Data
@Slf4j
class EventStorageReader {
    private static final ObjectMapper mapper = new ObjectMapper();

    private EventStorageFiles files;
    private FileEventStorageSettings settings;

    private BufferedReader bufferedReader;

    private volatile EventStorageReaderPointer currentPos;
    private volatile EventStorageReaderPointer newPos;
    private List<UplinkMsg> currentBatch;

    EventStorageReader(EventStorageFiles files, FileEventStorageSettings settings) {
        this.files = files;
        this.settings = settings;
        this.currentPos = readStateFile();
        this.newPos = currentPos.copy();
    }

    List<UplinkMsg> read() {
        log.debug("[{}:{}] Check for new messages in storage", newPos.getFile(), newPos.getLine());
        if (!CollectionUtils.isEmpty(currentBatch) && !currentPos.equals(newPos)) {
            log.debug("The previous batch was not discarded!");
            return currentBatch;
        }
        currentBatch = new ArrayList<>();
        int recordsToRead = settings.getMaxReadRecordsCount();
        while (recordsToRead > 0) {
            try {
                int currentLineInFile = newPos.getLine();
                BufferedReader reader = getOrInitBufferedReader(newPos);
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        currentBatch.add(UplinkMsg.parseFrom(Base64.getDecoder().decode(line)));
                        recordsToRead--;
                    } catch (Exception e) {
                        log.warn("Could not parse line [{}] to uplink message!", line, e);
                    } finally {
                        currentLineInFile++;
                    }

                    newPos.setLine(currentLineInFile);
                    if (recordsToRead == 0) {
                        break;
                    }
                }

                if (currentLineInFile >= settings.getMaxRecordsPerFile()) {
                    File nextFile = getNextFile(files, newPos);
                    if (nextFile != null) {
                        if (bufferedReader != null) {
                            bufferedReader.close();
                        }
                        bufferedReader = null;
                        newPos = new EventStorageReaderPointer(nextFile, 0);
                    } else {
                        // No more records to read for now
                        break;
                    }
                } else {
                    // No more records to read for now
                    break;
                }
            } catch (IOException e) {
                log.warn("[{}] Failed to read file!", newPos.getFile().getName(), e);
                break;
            }
        }
        log.debug("Got {} mesages from storage", currentBatch.size());
        return currentBatch;
    }

    void discardBatch() {
        currentPos = newPos.copy();
        writeInfoToStateFile(currentPos);
    }

    private File getNextFile(EventStorageFiles files, EventStorageReaderPointer newPos) {
        boolean found = false;
        for (File file : files.getDataFiles()) {
            if (found) {
                return file;
            }
            if (file.getName().equals(newPos.getFile().getName())) {
                found = true;
            }
        }
        if (found) {
            return null;
        } else {
            return files.getDataFiles().get(0);
        }
    }

    private BufferedReader getOrInitBufferedReader(EventStorageReaderPointer pointer) {
        try {
            if (bufferedReader == null) {
                bufferedReader = Files.newBufferedReader(pointer.getFile().toPath());
                int linesToSkip = pointer.getLine();
                if (linesToSkip > 0) {
                    while (linesToSkip != 0 && bufferedReader.readLine() != null) {
                        linesToSkip--;
                    }
                }
            }
            return bufferedReader;
        } catch (IOException e) {
            log.error("Failed to initialize buffered reader!", e);
            throw new RuntimeException("Failed to initialize buffered reader!", e);
        }
    }

    private EventStorageReaderPointer readStateFile() {
        JsonNode stateDataNode = null;
        try (BufferedReader br = Files.newBufferedReader(files.getStateFile().toPath())) {
            stateDataNode = mapper.readTree(br);
        } catch (IOException e) {
            log.warn("Failed to fetch info from state file!", e);
        }

        File readerFile = null;
        int readerPos = 0;
        if (stateDataNode != null && stateDataNode.has("position") && stateDataNode.has("file")) {
            readerPos = stateDataNode.get("position").asInt();
            for (File file : files.getDataFiles()) {
                if (file.getName().equals(stateDataNode.get("file").asText())) {
                    readerFile = file;
                    break;
                }
            }
        }
        if (readerFile == null) {
            readerFile = files.getDataFiles().get(0);
            readerPos = 0;
        }
        log.info("Initializing from state file: [{}:{}]", readerFile.getAbsolutePath(), readerPos);
        return new EventStorageReaderPointer(readerFile, readerPos);
    }

    private void writeInfoToStateFile(EventStorageReaderPointer pointer) {
        try {
            ObjectNode stateFileNode = mapper.createObjectNode();
            stateFileNode.put("position", pointer.getLine());
            stateFileNode.put("file", pointer.getFile().getName());
            Files.write(Paths.get(files.getStateFile().toURI()), mapper.writeValueAsString(stateFileNode).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.warn("Failed to update state file!", e);
        }
    }

    void destroy() throws IOException {
        if (bufferedReader != null) {
            bufferedReader.close();
        }
    }
}
