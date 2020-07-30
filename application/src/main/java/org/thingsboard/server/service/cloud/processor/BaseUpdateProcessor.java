/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.cloud.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.CloudUtils;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.queue.TbClusterService;

@Slf4j
public abstract class BaseUpdateProcessor {

    protected static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    protected AttributesService attributesService;

    @Autowired
    protected DeviceCredentialsService deviceCredentialsService;

    @Autowired
    protected AlarmService alarmService;

    @Autowired
    protected DeviceService deviceService;

    @Autowired
    protected AssetService assetService;

    @Autowired
    protected EntityViewService entityViewService;

    @Autowired
    protected DashboardService dashboardService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    protected EntityGroupService entityGroupService;

    @Autowired
    private CloudEventService cloudEventService;

    @Autowired
    protected RelationService relationService;

    @Autowired
    protected WidgetsBundleService widgetsBundleService;

    @Autowired
    private EventService eventService;

    @Autowired
    protected TbClusterService tbClusterService;

    @Autowired
    protected DbCallbackExecutorService dbCallbackExecutor;

    protected void updateAuditLogs(TenantId tenantId, Device origin, Device destination) {
        TimePageData<AuditLog> auditLogs = auditLogService.findAuditLogsByTenantIdAndEntityId(tenantId, origin.getId(), null, new TimePageLink(Integer.MAX_VALUE));
        if (auditLogs != null && auditLogs.getData() != null && !auditLogs.getData().isEmpty()) {
            for (AuditLog auditLogEntry : auditLogs.getData()) {
                auditLogEntry.setEntityId(destination.getId());
                auditLogService.saveOrUpdateAuditLog(auditLogEntry);
            }
        }
        log.debug("Related audit logs updated, origin [{}], destination [{}]", origin.getId(), destination.getId());
    }

    protected void requestForAdditionalData(TenantId tenantId, UpdateMsgType updateMsgType, EntityId entityId) {
        if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(updateMsgType) ||
                UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE.equals(updateMsgType) ||
                UpdateMsgType.DEVICE_CONFLICT_RPC_MESSAGE.equals(updateMsgType)) {
            saveCloudEvent(tenantId, CloudUtils.getCloudEventTypeByEntityType(entityId.getEntityType()),
                    ActionType.ATTRIBUTES_REQUEST, entityId, null);
            saveCloudEvent(tenantId, CloudUtils.getCloudEventTypeByEntityType(entityId.getEntityType()),
                    ActionType.RELATION_REQUEST, entityId, null);
        }
    }

    protected void updateEvents(TenantId tenantId, Device origin, Device destination) {
        TimePageData<Event> events = eventService.findEvents(tenantId, origin.getId(), new TimePageLink(Integer.MAX_VALUE));
        if (events != null && events.getData() != null && !events.getData().isEmpty()) {
            for (Event event : events.getData()) {
                event.setEntityId(destination.getId());
                eventService.saveAsync(event);
            }
        }
        log.debug("Related events updated, origin [{}], destination [{}]", origin.getId(), destination.getId());
    }

    protected void addEntityToGroup(TenantId tenantId, String groupName, EntityId entityId, EntityType entityType) {
        if (!StringUtils.isEmpty(groupName)) {
            EntityGroup orCreateEntityGroup = entityGroupService.findOrCreateEntityGroup(tenantId, tenantId, entityType, groupName, null, null);
            if (orCreateEntityGroup != null) {
                addEntityToGroup(tenantId, orCreateEntityGroup.getId(), entityId);
            }
        }
    }

    protected void addEntityToGroup(TenantId tenantId, EntityGroupId entityGroupId, EntityId entityId) {
        if (entityGroupId != null && !ModelConstants.NULL_UUID.equals(entityGroupId.getId())) {
            ListenableFuture<EntityGroup> entityGroupFuture = entityGroupService.findEntityGroupByIdAsync(tenantId, entityGroupId);
            Futures.addCallback(entityGroupFuture, new FutureCallback<EntityGroup>() {
                @Override
                public void onSuccess(@org.checkerframework.checker.nullness.qual.Nullable EntityGroup EntityGroup) {
                    if (EntityGroup != null) {
                        entityGroupService.addEntityToEntityGroup(tenantId, entityGroupId, entityId);
                    }
                }

                @Override
                public void onFailure(Throwable t) {

                }
            }, dbCallbackExecutor);
        }
    }

    protected void saveCloudEvent(TenantId tenantId,
                                  CloudEventType cloudEventType,
                                  ActionType cloudEventAction,
                                  EntityId entityId,
                                  JsonNode entityBody) {
        log.debug("Pushing cloud event to cloud queue. tenantId [{}], cloudEventType [{}], cloudEventAction[{}], entityId [{}], entityBody [{}]",
                tenantId, cloudEventType, cloudEventAction, entityId, entityBody);

        CloudEvent cloudEvent = new CloudEvent();
        cloudEvent.setTenantId(tenantId);
        cloudEvent.setCloudEventType(cloudEventType);
        cloudEvent.setCloudEventAction(cloudEventAction.name());
        if (entityId != null) {
            cloudEvent.setEntityId(entityId.getId());
        }
        cloudEvent.setEntityBody(entityBody);
        cloudEventService.saveAsync(cloudEvent);
    }
}
