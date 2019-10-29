package org.thingsboard.storage.edge;

import org.springframework.stereotype.Component;
import org.thingsboard.server.gen.edge.UplinkMsg;
import org.thingsboard.storage.EventStorageFiles;
import org.thingsboard.storage.EventStorageReader;
import org.thingsboard.storage.FileEventStorage;
import org.thingsboard.storage.FileEventStorageSettings;

@Component("edgeFileEventStorage")
public class EdgeFileEventStorage extends FileEventStorage<UplinkMsg> {

    @Override
    protected EventStorageReader<UplinkMsg> getEventStorageReader(EventStorageFiles files, FileEventStorageSettings settings) {
        return new EdgeEventStorageReader(files, settings);
    }
}
