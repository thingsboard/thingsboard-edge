package org.thingsboard.storage.integration;

import com.google.protobuf.InvalidProtocolBufferException;
import org.thingsboard.server.gen.integration.UplinkMsg;
import org.thingsboard.storage.EventStorageFiles;
import org.thingsboard.storage.EventStorageReader;
import org.thingsboard.storage.FileEventStorageSettings;

import java.util.Base64;

public class IntegrationEventStorageReader extends EventStorageReader<UplinkMsg> {

    IntegrationEventStorageReader(EventStorageFiles files, FileEventStorageSettings settings) {
        super(files, settings);
    }

    @Override
    protected UplinkMsg parseFromLine(String line) throws InvalidProtocolBufferException {
        return UplinkMsg.parser().parseFrom(Base64.getDecoder().decode(line));
    }
}
