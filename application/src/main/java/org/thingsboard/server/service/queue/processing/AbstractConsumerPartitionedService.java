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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractConsumerPartitionedService<N extends com.google.protobuf.GeneratedMessageV3> extends AbstractConsumerService<N> {

    private final Lock startupLock;
    private volatile boolean consumersInitialized;
    private PartitionChangeEvent lastPartitionChangeEvent;

    public AbstractConsumerPartitionedService(ActorSystemContext actorContext,
                                              TbTenantProfileCache tenantProfileCache,
                                              TbDeviceProfileCache deviceProfileCache,
                                              TbAssetProfileCache assetProfileCache,
                                              CalculatedFieldCache calculatedFieldCache,
                                              TbApiUsageStateService apiUsageStateService,
                                              PartitionService partitionService,
                                              ApplicationEventPublisher eventPublisher,
                                              JwtSettingsService jwtSettingsService) {
        super(actorContext, tenantProfileCache, deviceProfileCache, assetProfileCache, calculatedFieldCache, apiUsageStateService, partitionService, eventPublisher, jwtSettingsService);
        this.startupLock = new ReentrantLock();
        this.consumersInitialized = false;
    }

    @PostConstruct
    public void init() {
        super.init(getPrefix());
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void afterStartUp() {
        super.afterStartUp();
        doAfterStartUp();
        startupLock.lock();
        try {
            processPartitionChangeEvent(lastPartitionChangeEvent);
            consumersInitialized = true;
        } finally {
            startupLock.unlock();
        }
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        if (!consumersInitialized) {
            startupLock.lock();
            try {
                if (!consumersInitialized) {
                    lastPartitionChangeEvent = event;
                    return;
                }
            } finally {
                startupLock.unlock();
            }
        }
        processPartitionChangeEvent(event);
    }

    protected abstract void doAfterStartUp();

    protected abstract void processPartitionChangeEvent(PartitionChangeEvent event);

    protected abstract String getPrefix();

}
