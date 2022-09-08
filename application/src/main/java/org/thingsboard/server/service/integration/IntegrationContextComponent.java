/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
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
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.integration.api.IntegrationStatisticsService;
import org.thingsboard.integration.api.util.LogSettingsComponent;
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
import org.thingsboard.server.service.integration.downlink.DownlinkCacheService;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
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
    private DownlinkCacheService downlinkCacheService;

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

    @Lazy
    @Autowired
    private LogSettingsComponent logSettingsComponent;

    @Lazy
    @Autowired
    private IntegrationStatisticsService integrationStatisticsService;

    private EventLoopGroup eventLoopGroup;
    private ScheduledExecutorService scheduledExecutorService;
    private ExecutorService callBackExecutorService;
    private ExecutorService generalExecutorService;

    @PostConstruct
    public void init() {
        eventLoopGroup = new NioEventLoopGroup();
        scheduledExecutorService = Executors.newScheduledThreadPool(3, ThingsBoardThreadFactory.forName("integration-scheduled"));
        callBackExecutorService = ThingsBoardExecutors.newWorkStealingPool(Math.max(2, Runtime.getRuntime().availableProcessors()), "integration-callback");
        generalExecutorService = ThingsBoardExecutors.newWorkStealingPool(20, "integration-general");
    }

    @PreDestroy
    public void destroy() {
        eventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
        }
        if (callBackExecutorService != null) {
            callBackExecutorService.shutdownNow();
        }
        if (generalExecutorService != null) {
            generalExecutorService.shutdownNow();
        }
    }

    ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    ExecutorService getCallBackExecutorService() {
        return callBackExecutorService;
    }

    ExecutorService getGeneralExecutorService() {
        return generalExecutorService;
    }

    public boolean isExceptionStackTraceEnabled() {
        return logSettingsComponent.isExceptionStackTraceEnabled();
    }
}
