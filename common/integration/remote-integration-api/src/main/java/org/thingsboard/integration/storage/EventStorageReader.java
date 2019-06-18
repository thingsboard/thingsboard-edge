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
    private int currentLineToRead;

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

    // TODO: 6/18/19 validate & improve method
    public List<UplinkMsg> read() {
        List<UplinkMsg> uplinkMsgs = new ArrayList<>();

        if (currentReadFile == null) {
            JsonNode stateDataNode = fetchInfoFromStateFile();
            if (stateDataNode != null) {
                currentLineToRead = stateDataNode.get("position").asInt();
                for (File file : dataFiles) {
                    if (file.getName().equals(stateDataNode.get("currentFileName").asText())) {
                        currentReadFile = file;
                    }
                }
            }
        }

        int currentFileIdx = 0;
        int recordsToRead = maxReadRecordsCount;
        while (recordsToRead > 0) {
            if (currentReadFile == null) {
                currentReadFile = dataFiles.get(currentFileIdx);
            }
            try (BufferedReader bufferedReader = getOrInitBufferedReader(currentReadFile)) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (currentLineToRead != 0) {
                        currentLineToRead--;
                        continue;
                    }
                    if (recordsToRead == 0) {
                        writeInfoToStateFile();
                        break;
                    }

                    try {
                        uplinkMsgs.add(UplinkMsg.parseFrom(Base64.getDecoder().decode(line)));
                    } catch (Exception e) {

                    }


                    currentLineToRead++;
                    recordsToRead--;
                }
                if (line == null) {
                    currentLineToRead = 0;
                    currentFileIdx++;
                    currentReadFile = dataFiles.get(currentFileIdx);
                }
            } catch (IOException e) {
                log.warn("Failed to read file![{}]", currentReadFile.getName(), e);
            }
        }
        return uplinkMsgs;
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

    private void writeInfoToStateFile() {
        try (BufferedWriter writer = getBufferedWriter()) {
            ObjectNode stateFileNode = mapper.createObjectNode();
            stateFileNode.put("position", currentLineToRead);
            stateFileNode.put("currentFileName", currentReadFile.getName());
            writer.write(mapper.writeValueAsString(stateFileNode));
            writer.flush();
        } catch (IOException e) {
            log.warn("Failed to update state file!", e);
        }
    }
}
