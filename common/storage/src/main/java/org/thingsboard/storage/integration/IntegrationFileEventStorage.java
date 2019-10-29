package org.thingsboard.storage.integration;

import org.springframework.stereotype.Component;
import org.thingsboard.server.gen.integration.UplinkMsg;
import org.thingsboard.storage.EventStorageFiles;
import org.thingsboard.storage.EventStorageReader;
import org.thingsboard.storage.FileEventStorage;
import org.thingsboard.storage.FileEventStorageSettings;

@Component("integrationFileEventStorage")
public class IntegrationFileEventStorage extends FileEventStorage<UplinkMsg> {

    @Override
    protected EventStorageReader<UplinkMsg> getEventStorageReader(EventStorageFiles files, FileEventStorageSettings settings) {
        return new IntegrationEventStorageReader(files, settings);
    }
}
