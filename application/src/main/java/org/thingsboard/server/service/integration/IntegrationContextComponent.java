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
package org.thingsboard.server.service.integration;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.integration.downlink.DownlinkService;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@TbCoreComponent
@Component
@Data
public class IntegrationContextComponent {

    private volatile boolean isClosed = false;

    @EventListener
    public void handleContextClosed(ContextClosedEvent event) {
        isClosed = true;
    }

    @Autowired
    private TbServiceInfoProvider serviceInfoProvider;

    @Autowired
    private DeviceStateService deviceStateService;

    @Lazy
    @Autowired
    private PlatformIntegrationService platformIntegrationService;

    @Lazy
    @Autowired
    private IntegrationService integrationService;

    @Lazy
    @Autowired
    private ConverterService converterService;

    @Lazy
    @Autowired
    private DeviceService deviceService;

    @Lazy
    @Autowired
    private EntityViewService entityViewService;

    @Lazy
    @Autowired
    private RelationService relationService;

    @Lazy
    @Autowired
    private AttributesService attributesService;

    @Lazy
    @Autowired
    private TelemetrySubscriptionService telemetrySubscriptionService;

    @Lazy
    @Autowired
    private EventService eventService;

    @Lazy
    @Autowired
    private DownlinkService downlinkService;

    @Lazy
    @Autowired
    private ConverterContextComponent converterContextComponent;

    @Lazy
    @Autowired
    private CustomerService customerService;

    @Lazy
    @Autowired
    private AssetService assetService;

    @Lazy
    @Autowired
    private EntityGroupService entityGroupService;

    private EventLoopGroup eventLoopGroup;
    private ScheduledExecutorService scheduledExecutorService;

    @PostConstruct
    public void init() {
        eventLoopGroup = new NioEventLoopGroup();
        scheduledExecutorService = Executors.newScheduledThreadPool(3);
    }

    @PreDestroy
    public void destroy() {
        eventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
        }
    }

    ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }
}
