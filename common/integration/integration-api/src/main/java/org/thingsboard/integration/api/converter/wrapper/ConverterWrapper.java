package org.thingsboard.integration.api.converter.wrapper;

import org.thingsboard.integration.api.converter.DedicatedConverterConfig;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.common.data.util.TbPair;

import java.util.Set;

public interface ConverterWrapper {

    TbPair<byte[], UplinkMetaData> wrap(DedicatedConverterConfig config, byte[] payload, UplinkMetaData metadata);

    Set<String> getKeys();

}
