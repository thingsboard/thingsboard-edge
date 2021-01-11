/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Data
public class FileEventStorageSettings {
    @Value("${storage.data_folder_path}")
    private String dataFolderPath;
    @Value("${storage.max_file_count}")
    private int maxFileCount;
    @Value("${storage.max_records_per_file}")
    private int maxRecordsPerFile;
    @Value("${storage.max_records_between_fsync}")
    private int maxRecordsBetweenFsync;
    @Value("${storage.max_read_records_count}")
    private int maxReadRecordsCount;
    @Value("${storage.no_read_records_sleep}")
    private long noRecordsSleepInterval;
}
