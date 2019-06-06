package org.thingsboard.server.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.msg.cluster.ServerAddress;

@Data
public class LocalConverterContext implements ConverterContext {

    private final ConverterContextComponent ctx;
    private final Converter configuration;

    @Override
    public ServerAddress getServerAddress() {
        return ctx.getDiscoveryService().getCurrentServer().getServerAddress();
    }

    @Override
    public void saveEvent(String type, JsonNode body) {
        Event event = new Event();
        event.setTenantId(configuration.getTenantId());
        event.setEntityId(configuration.getId());
        event.setType(type);
        event.setBody(body);
        ctx.getEventService().save(event);
    }
}
