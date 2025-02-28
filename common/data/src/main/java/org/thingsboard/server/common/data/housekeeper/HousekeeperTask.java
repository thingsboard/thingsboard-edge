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
package org.thingsboard.server.common.data.housekeeper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "taskType", visible = true, include = JsonTypeInfo.As.EXISTING_PROPERTY, defaultImpl = HousekeeperTask.class)
@JsonSubTypes({
        @Type(name = "DELETE_TS_HISTORY", value = TsHistoryDeletionHousekeeperTask.class),
        @Type(name = "DELETE_LATEST_TS", value = LatestTsDeletionHousekeeperTask.class),
        @Type(name = "DELETE_TENANT_ENTITIES", value = TenantEntitiesDeletionHousekeeperTask.class),
        @Type(name = "DELETE_ENTITIES", value = EntitiesDeletionHousekeeperTask.class),
        @Type(name = "DELETE_ALARMS", value = AlarmsDeletionHousekeeperTask.class),
        @Type(name = "UNASSIGN_ALARMS", value = AlarmsUnassignHousekeeperTask.class),
        @Type(name = "CLEANUP_ENTITIES", value = EntitiesCleanupHousekeeperTask.class),
})
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HousekeeperTask implements Serializable {

    private TenantId tenantId;
    private EntityId entityId;
    private HousekeeperTaskType taskType;
    private long ts;

    protected HousekeeperTask(@NonNull TenantId tenantId, @NonNull EntityId entityId, @NonNull HousekeeperTaskType taskType) {
        this.tenantId = tenantId;
        this.entityId = entityId;
        this.taskType = taskType;
        this.ts = System.currentTimeMillis();
    }

    public static HousekeeperTask deleteAttributes(TenantId tenantId, EntityId entityId) {
        return new HousekeeperTask(tenantId, entityId, HousekeeperTaskType.DELETE_ATTRIBUTES);
    }

    public static HousekeeperTask deleteTelemetry(TenantId tenantId, EntityId entityId) {
        return new HousekeeperTask(tenantId, entityId, HousekeeperTaskType.DELETE_TELEMETRY);
    }

    public static HousekeeperTask deleteEvents(TenantId tenantId, EntityId entityId) {
        return new HousekeeperTask(tenantId, entityId, HousekeeperTaskType.DELETE_EVENTS);
    }

    public static HousekeeperTask unassignAlarms(User user) {
        return new AlarmsUnassignHousekeeperTask(user);
    }

    public static HousekeeperTask deleteAlarms(TenantId tenantId, EntityId entityId) {
        return new AlarmsDeletionHousekeeperTask(tenantId, entityId);
    }

    public static HousekeeperTask deleteTenantEntities(TenantId tenantId, EntityType entityType) {
        return new TenantEntitiesDeletionHousekeeperTask(tenantId, entityType);
    }

    @JsonIgnore
    public String getDescription() {
        return taskType.getDescription() + " for " + entityId.getEntityType().getNormalName().toLowerCase() + " " + entityId.getId();
    }

}
