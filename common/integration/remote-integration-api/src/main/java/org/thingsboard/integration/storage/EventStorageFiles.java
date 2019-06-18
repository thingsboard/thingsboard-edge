package org.thingsboard.integration.storage;

import lombok.Data;

import java.io.File;
import java.util.List;

@Data
class EventStorageFiles {

    private final File stateFile;
    private final List<File> dataFiles;

}
