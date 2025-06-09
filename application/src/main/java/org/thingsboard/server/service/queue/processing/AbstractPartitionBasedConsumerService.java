/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.queue.processing;

import jakarta.annotation.PostConstruct;
import org.springframework.context.ApplicationEventPublisher;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.cf.CalculatedFieldCache;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;
import org.thingsboard.server.service.security.permission.OwnersCacheService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractPartitionBasedConsumerService<N extends com.google.protobuf.GeneratedMessageV3> extends AbstractConsumerService<N> {

    private final Lock startupLock = new ReentrantLock();
    private volatile boolean started = false;
    private List<PartitionChangeEvent> pendingEvents = new ArrayList<>();

    public AbstractPartitionBasedConsumerService(ActorSystemContext actorContext,
                                                 TbTenantProfileCache tenantProfileCache,
                                                 TbDeviceProfileCache deviceProfileCache,
                                                 TbAssetProfileCache assetProfileCache,
                                                 CalculatedFieldCache calculatedFieldCache,
                                                 TbApiUsageStateService apiUsageStateService,
                                                 PartitionService partitionService,
                                                 ApplicationEventPublisher eventPublisher,
                                                 JwtSettingsService jwtSettingsService,
                                                 OwnersCacheService ownersCacheService) {
        super(actorContext, tenantProfileCache, deviceProfileCache, assetProfileCache, calculatedFieldCache, apiUsageStateService, partitionService, eventPublisher, jwtSettingsService, ownersCacheService);
    }

    @PostConstruct
    public void init() {
        super.init(getPrefix());
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    @Override
    public void afterStartUp() {
        super.afterStartUp();
        onStartUp();
        startupLock.lock();
        try {
            for (PartitionChangeEvent partitionChangeEvent : pendingEvents) {
                log.info("Handling partition change event: {}", partitionChangeEvent);
                try {
                    onPartitionChangeEvent(partitionChangeEvent);
                } catch (Throwable t) {
                    log.error("Failed to handle partition change event: {}", partitionChangeEvent, t);
                }
            }
            started = true;
            pendingEvents = null;
        } finally {
            startupLock.unlock();
        }
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        log.debug("Received partition change event: {}", event);
        if (!started) {
            startupLock.lock();
            try {
                if (!started) {
                    log.debug("App not started yet, storing event for later: {}", event);
                    pendingEvents.add(event);
                    return;
                }
            } finally {
                startupLock.unlock();
            }
        }
        log.info("Handling partition change event: {}", event);
        onPartitionChangeEvent(event);
    }

    protected abstract void onStartUp();

    protected abstract void onPartitionChangeEvent(PartitionChangeEvent event);

    protected abstract String getPrefix();

}
