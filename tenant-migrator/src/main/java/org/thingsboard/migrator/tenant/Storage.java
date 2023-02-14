/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.migrator.tenant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Component
public class Storage {

    @Value("${working_directory}")
    private String workingDir;

    private final ObjectMapper jsonMapper = new ObjectMapper();

    @PostConstruct
    private void init() throws IOException {
        Files.createDirectories(Path.of(workingDir));
    }

    public void newFile(String name) throws IOException {
        Path file = getPath(name);
        Files.deleteIfExists(file);
        Files.createFile(file);
    }

    public Writer newWriter(String file) throws IOException {
        return Files.newBufferedWriter(getPath(file), StandardOpenOption.APPEND);
    }

    public Writer newGzipWriter(String file) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(getPath(file).toFile());
        return new OutputStreamWriter(new GZIPOutputStream(fileOutputStream), StandardCharsets.UTF_8);
    }

    public void addToFile(Writer writer, Map<String, Object> row) throws IOException {
        row.replaceAll((column, data) -> {
            if (data instanceof PGobject) {
                data = ((PGobject) data).getValue();
            }
            return data;
        });
        String serialized = toJson(row);
        writer.append(serialized).append(System.lineSeparator());
    }

    public void readAndProcessGzipped(String file, Consumer<Map<String, Object>> processor) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(getPath(file).toFile());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(fileInputStream)))) {
            readAndProcess(reader, processor);
        }
    }

    public void readAndProcess(String file, Consumer<Map<String, Object>> processor) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(getPath(file))) {
            readAndProcess(reader, processor);
        }
    }

    private void readAndProcess(BufferedReader reader, Consumer<Map<String, Object>> processor) {
        reader.lines().forEach(line -> {
            if (StringUtils.isNotBlank(line)) {
                Map<String, Object> data;
                try {
                    data = jsonMapper.readValue(line, new TypeReference<Map<String, Object>>() {});
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                data.replaceAll((key, value) -> {
                    if (value == null) return null;
                    if (key.contains("id")) {
                        try {
                            value = UUID.fromString(value.toString());
                        } catch (IllegalArgumentException ignored) {}
                    } else if (value instanceof Map) {
                        value = jsonMapper.valueToTree(value);
                    }
                    return value;
                });
                processor.accept(data);
            }
        });
    }

    @SneakyThrows
    private String toJson(Object o) {
        return jsonMapper.writeValueAsString(o);
    }

    private Path getPath(String file) {
        return Path.of(workingDir, file);
    }

}
