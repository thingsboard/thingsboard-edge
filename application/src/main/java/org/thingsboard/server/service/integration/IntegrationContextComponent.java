package org.thingsboard.server.service.integration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.service.cluster.discovery.DiscoveryService;
import org.thingsboard.server.service.integration.downlink.DownlinkService;

@Component
@Data
public class IntegrationContextComponent {

    private volatile boolean isClosed = false;

    @EventListener
    public void handleContextClosed(ContextClosedEvent event) {
        isClosed = true;
    }

    @Lazy
    @Autowired
    private PlatformIntegrationService integrationService;

    @Lazy
    @Autowired
    private DeviceService deviceService;

    //createRelation
    @Lazy
    @Autowired
    private RelationService relationService;

    //persistDebug
    @Lazy
    @Autowired
    private EventService eventService;

    //context.getDiscoveryService().getCurrentServer().getServerAddress().toString()
    @Lazy
    @Autowired
    private DiscoveryService discoveryService;

    @Lazy
    @Autowired
    private ActorService actorService;

    @Lazy
    @Autowired
    private ConverterContext converterContext;

    @Lazy
    @Autowired
    private DownlinkService downlinkService;
}
