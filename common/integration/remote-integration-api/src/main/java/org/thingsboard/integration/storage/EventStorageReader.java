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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.gen.integration.UplinkMsg;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Data
@Slf4j
public class EventStorageReader {

    public static final ObjectMapper mapper = new ObjectMapper();

    private List<File> dataFiles;
    private File stateFile;
    private int maxReadRecordsCount;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;

    private File currentReadFile;
    private File previousReadFile;

    private int startReadingFromLine;
    private int lastReadLine;

    public EventStorageReader(List<File> dataFiles, File stateFile, int maxReadRecordsCount) {
        this.dataFiles = dataFiles;
        this.stateFile = stateFile;
        this.maxReadRecordsCount = maxReadRecordsCount;
        try {
            this.bufferedWriter = new BufferedWriter(new FileWriter(stateFile));
        } catch (IOException e) {
            log.error("Failed to initialize buffered writer for state file!", e);
        }
    }

    public BufferedReader getOrInitBufferedReader(File file) {
        try {
            if (bufferedReader == null) {
                return Files.newBufferedReader(file.toPath());
            } else {
                return bufferedReader;
            }
        } catch (IOException e) {
            log.error("Failed to initialize buffered reader!", e);
            throw new RuntimeException("Failed to initialize buffered reader!", e);
        }
    }

    public List<UplinkMsg> read() {
        if (startReadingFromLine != lastReadLine) {
            log.error("The previous batch must be discarded first!");
            currentReadFile = previousReadFile;
            lastReadLine = startReadingFromLine;
        }
        List<UplinkMsg> uplinkMsgs = new ArrayList<>();
        if (currentReadFile == null) {
            readStateFile();
        }
        int linesToSkip = startReadingFromLine;

        int currentFileIdx = 0;
        int recordsToRead = maxReadRecordsCount;
        while (recordsToRead > 0) {
            if (currentReadFile == null) {
                currentReadFile = dataFiles.get(currentFileIdx);
            }
            try (BufferedReader bufferedReader = getOrInitBufferedReader(currentReadFile)) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (linesToSkip != 0) {
                        linesToSkip--;
                        continue;
                    }
                    if (recordsToRead == 0) {
                        break;
                    }
                    try {
                        uplinkMsgs.add(UplinkMsg.parseFrom(Base64.getDecoder().decode(line)));
                        lastReadLine++;
                        recordsToRead--;
                    } catch (Exception e) {
                        log.warn("Could not parse line [{}] to uplink message!", line, e);
                        lastReadLine++;
                    }
                }
                if (line == null) {
                    lastReadLine = 0;
                    currentFileIdx++;
                    previousReadFile = currentReadFile;
                    currentReadFile = dataFiles.get(currentFileIdx);
                }
            } catch (IOException e) {
                log.warn("Failed to read file![{}]", currentReadFile.getName(), e);
            }
        }
        return uplinkMsgs;
    }

    private void readStateFile() {
        JsonNode stateDataNode = fetchInfoFromStateFile();
        if (stateDataNode != null) {
            startReadingFromLine = stateDataNode.get("position").asInt();
            for (File file : dataFiles) {
                if (file.getName().equals(stateDataNode.get("currentFileName").asText())) {
                    currentReadFile = file;
                    break;
                } else {
                    if (file.delete()) {
                        dataFiles.remove(file);
                    }
                }
            }
        }
    }

    private JsonNode fetchInfoFromStateFile() {
        try (BufferedReader br = Files.newBufferedReader(stateFile.toPath())) {
            String line = br.readLine();
            if (line != null) {
                return mapper.readTree(line);
            }
        } catch (IOException e) {
            log.warn("Failed to fetch info from state file!", e);
        }
        return null;
    }

    public void discardBatch() {
        startReadingFromLine = lastReadLine;
        if (previousReadFile != currentReadFile && previousReadFile.delete()) {
            dataFiles.remove(previousReadFile);
        }
        writeInfoToStateFile();
    }

    private void writeInfoToStateFile() {
        try (BufferedWriter writer = getBufferedWriter()) {
            ObjectNode stateFileNode = mapper.createObjectNode();
            stateFileNode.put("position", startReadingFromLine);
            stateFileNode.put("currentFileName", currentReadFile.getName());
            writer.write(mapper.writeValueAsString(stateFileNode));
            writer.flush();
        } catch (IOException e) {
            log.warn("Failed to update state file!", e);
        }
    }
}
