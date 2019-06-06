package org.thingsboard.server.service.integration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.service.cluster.discovery.DiscoveryService;

@Component
@Data
public class ConverterContextComponent {
    @Lazy
    @Autowired
    private EventService eventService;

    @Lazy
    @Autowired
    private DiscoveryService discoveryService;
}
