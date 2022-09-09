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
package org.thingsboard.integration.service.context;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.integration.api.IntegrationStatisticsService;
import org.thingsboard.server.cache.TbCacheValueWrapper;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.cache.device.DeviceCacheKey;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.integration.downlink.DownlinkCacheService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Data
@RequiredArgsConstructor
public class DefaultTbIntegrationExecutorContextComponent implements TbIntegrationExecutorContextComponent {

    private final DownlinkCacheService downlinkCacheService;
    private final TbTransactionalCache<DeviceCacheKey, Device> deviceCache;
    private final IntegrationStatisticsService integrationStatisticsService;
    private EventLoopGroup eventLoopGroup;
    private ScheduledExecutorService scheduledExecutorService;
    private ExecutorService generalExecutorService;
    private ExecutorService callBackExecutorService;

    @PostConstruct
    public void init() {
        eventLoopGroup = new NioEventLoopGroup();
        scheduledExecutorService = Executors.newScheduledThreadPool(3, ThingsBoardThreadFactory.forName("integration-scheduled"));
        generalExecutorService = ThingsBoardExecutors.newWorkStealingPool(20, "integration-general");
        callBackExecutorService = ThingsBoardExecutors.newWorkStealingPool(Math.max(2, Runtime.getRuntime().availableProcessors()), "integration-callback");
    }

    @PreDestroy
    public void destroy() {
        eventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
        }
        if (generalExecutorService != null) {
            generalExecutorService.shutdownNow();
        }
        if (callBackExecutorService != null) {
            callBackExecutorService.shutdownNow();
        }
    }

    @Override
    public Device findCachedDeviceByTenantIdAndName(TenantId tenantId, String deviceName) {
        TbCacheValueWrapper<Device> cacheValue = deviceCache.get(new DeviceCacheKey(tenantId, deviceName));
        return cacheValue == null ? null : cacheValue.get();
    }

}
