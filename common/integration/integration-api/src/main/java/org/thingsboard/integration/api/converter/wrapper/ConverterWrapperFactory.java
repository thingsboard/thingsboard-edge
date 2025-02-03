package org.thingsboard.integration.api.converter.wrapper;

import org.thingsboard.server.common.data.integration.IntegrationType;

import java.util.concurrent.ConcurrentHashMap;

public final class ConverterWrapperFactory {

    private static final ConcurrentHashMap<IntegrationType, ConverterWrapper> wrappers = new ConcurrentHashMap<>();

    private ConverterWrapperFactory() {}

    public static ConverterWrapper getWrapper(IntegrationType integrationType) {
        return wrappers.computeIfAbsent(integrationType, key -> switch (integrationType) {
            case LORIOT -> new LoriotConverterWrapper();
            default -> throw new IllegalArgumentException("Unsupported integrationType: " + integrationType);
        });
    }
}
