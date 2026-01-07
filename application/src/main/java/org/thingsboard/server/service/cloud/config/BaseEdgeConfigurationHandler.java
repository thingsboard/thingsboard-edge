/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.cloud.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeSettings;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.cloud.EdgeSettingsService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.eventsourcing.ResetQueueOffsetEvent;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.cloud.CloudContextComponent;
import org.thingsboard.server.service.cloud.event.migrator.CloudEventMigrationService;
import org.thingsboard.server.service.cloud.info.EdgeInfoHolder;
import org.thingsboard.server.service.cloud.rpc.ConnectionStatusManager;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseEdgeConfigurationHandler implements EdgeConfigurationHandler {

    private final EdgeSettingsService edgeSettingsService;
    private final CloudContextComponent cloudCtx;
    private final EdgeService edgeService;
    private final EdgeInfoHolder edgeInfo;
    private final PartitionService partitionService;
    private final ConnectionStatusManager connectionStatusManager;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired(required = false)
    private CloudEventMigrationService cloudEventMigrationService;

    @Override
    public EdgeSettings initAndUpdateEdgeSettings(EdgeConfiguration edgeConfiguration) throws Exception {
        if (!isSystemTenantPartitionMine()) {
            return null;
        }
        TenantId tenantId = TenantId.fromUUID(new UUID(edgeConfiguration.getTenantIdMSB(), edgeConfiguration.getTenantIdLSB()));
        edgeInfo.setTenantId(tenantId);

        EdgeSettings currentEdgeSettings = updateEdgeSettingsIfRequired(edgeConfiguration);

        cloudCtx.getTenantProcessor().createTenantIfNotExists(tenantId);
        boolean edgeCustomerIdUpdated = setOrUpdateCustomerId(edgeConfiguration);
        if (edgeCustomerIdUpdated) {
            cloudCtx.getCustomerProcessor().createCustomerIfNotExists(tenantId, edgeConfiguration);
        }

        edgeSettingsService.saveEdgeSettings(tenantId, currentEdgeSettings);
        saveOrUpdateEdge(tenantId, edgeConfiguration);

        connectionStatusManager.updateConnectivityStatus(true);

        if (cloudEventMigrationService != null && cloudEventMigrationService.isMigrationRequired()) {
            cloudEventMigrationService.migrateUnprocessedEventToKafka();
        }

        return currentEdgeSettings;
    }

    @Override
    public boolean setOrUpdateCustomerId(EdgeConfiguration edgeConfiguration) {
        EdgeId edgeId = getEdgeId(edgeConfiguration);
        Edge edge = edgeService.findEdgeById(edgeInfo.getTenantId(), edgeId);
        CustomerId previousCustomerId = null;
        if (edge != null) {
            previousCustomerId = edge.getCustomerId();
        }
        if (edgeConfiguration.getCustomerIdMSB() != 0 && edgeConfiguration.getCustomerIdLSB() != 0) {
            UUID customerUUID = new UUID(edgeConfiguration.getCustomerIdMSB(), edgeConfiguration.getCustomerIdLSB());
            edgeInfo.setCustomerId(new CustomerId(customerUUID));
            return !edgeInfo.getCustomerId().equals(previousCustomerId);
        }
        return false;
    }

    private void saveOrUpdateEdge(TenantId tenantId, EdgeConfiguration edgeConfiguration) throws Exception {
        EdgeId edgeId = getEdgeId(edgeConfiguration);
        cloudCtx.getEdgeProcessor().processEdgeConfigurationMsgFromCloud(tenantId, edgeConfiguration);
        cloudCtx.getCloudEventService().saveCloudEvent(tenantId, CloudEventType.EDGE, EdgeEventActionType.ATTRIBUTES_REQUEST, edgeId, null);
        cloudCtx.getCloudEventService().saveCloudEvent(tenantId, CloudEventType.EDGE, EdgeEventActionType.RELATION_REQUEST, edgeId, null);
    }

    private EdgeId getEdgeId(EdgeConfiguration edgeConfiguration) {
        UUID edgeUUID = new UUID(edgeConfiguration.getEdgeIdMSB(), edgeConfiguration.getEdgeIdLSB());
        return new EdgeId(edgeUUID);
    }

    private EdgeSettings updateEdgeSettingsIfRequired(EdgeConfiguration edgeConfiguration) {
        EdgeSettings currentEdgeSettings = edgeSettingsService.findEdgeSettings();
        EdgeSettings newEdgeSettings = constructEdgeSettings(edgeConfiguration);

        if (currentEdgeSettings != null && !newEdgeSettings.getEdgeId().equals(currentEdgeSettings.getEdgeId())) {
            cloudCtx.getTenantProcessor().cleanUp();
            eventPublisher.publishEvent(ResetQueueOffsetEvent.INSTANCE);
            currentEdgeSettings = null;
        }

        if (currentEdgeSettings == null) {
            currentEdgeSettings = newEdgeSettings;
        } else {
            log.trace("Using edge settings from DB {}", currentEdgeSettings);
            currentEdgeSettings.setName(newEdgeSettings.getName());
            currentEdgeSettings.setType(newEdgeSettings.getType());
            currentEdgeSettings.setRoutingKey(newEdgeSettings.getRoutingKey());
        }

        edgeInfo.setSettings(currentEdgeSettings);
        return currentEdgeSettings;
    }

    private EdgeSettings constructEdgeSettings(EdgeConfiguration edgeConfiguration) {
        EdgeSettings edgeSettings = new EdgeSettings();
        UUID edgeUUID = new UUID(edgeConfiguration.getEdgeIdMSB(), edgeConfiguration.getEdgeIdLSB());
        edgeSettings.setEdgeId(edgeUUID.toString());
        UUID tenantUUID = new UUID(edgeConfiguration.getTenantIdMSB(), edgeConfiguration.getTenantIdLSB());
        edgeSettings.setTenantId(tenantUUID.toString());
        edgeSettings.setName(edgeConfiguration.getName());
        edgeSettings.setType(edgeConfiguration.getType());
        edgeSettings.setRoutingKey(edgeConfiguration.getRoutingKey());
        edgeSettings.setFullSyncRequired(true);
        return edgeSettings;
    }

    private boolean isSystemTenantPartitionMine() {
        return partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isMyPartition();
    }
}
