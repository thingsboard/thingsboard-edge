/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.common.data.alarm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.List;

/**
 * Created by ashvayka on 11.05.17.
 */
@Schema
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Alarm extends BaseData<AlarmId> implements HasName, TenantEntity, HasCustomerId {

    @Schema(description = "JSON object with Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @Schema(description = "JSON object with Customer Id", accessMode = Schema.AccessMode.READ_ONLY)
    private CustomerId customerId;

    @NoXss
    @Schema(required = true, description = "representing type of the Alarm", example = "High Temperature Alarm")
    @Length(fieldName = "type")
    private String type;
    @Schema(required = true, description = "JSON object with alarm originator id")
    private EntityId originator;
    @Schema(required = true, description = "Alarm severity", example = "CRITICAL")
    private AlarmSeverity severity;
    @Schema(required = true, description = "Acknowledged", example = "true")
    private boolean acknowledged;
    @Schema(required = true, description = "Cleared", example = "false")
    private boolean cleared;
    @Schema(description = "Alarm assignee user id")
    private UserId assigneeId;
    @Schema(description = "Timestamp of the alarm start time, in milliseconds", example = "1634058704565")
    private long startTs;
    @Schema(description = "Timestamp of the alarm end time(last time update), in milliseconds", example = "1634111163522")
    private long endTs;
    @Schema(description = "Timestamp of the alarm acknowledgement, in milliseconds", example = "1634115221948")
    private long ackTs;
    @Schema(description = "Timestamp of the alarm clearing, in milliseconds", example = "1634114528465")
    private long clearTs;
    @Schema(description = "Timestamp of the alarm assignment, in milliseconds", example = "1634115928465")
    private long assignTs;
    @Schema(description = "JSON object with alarm details")
    private transient JsonNode details;
    @Schema(description = "Propagation flag to specify if alarm should be propagated to parent entities of alarm originator", example = "true")
    private boolean propagate;
    @Schema(description = "Propagation flag to specify if alarm should be propagated to the owner (tenant or customer) of alarm originator", example = "true")
    private boolean propagateToOwner;
    @Schema(description = "Propagation flag to specify if alarm should be propagated to the owner (tenant or customer) and all parent owners in the customer hierarchy", example = "true")
    private boolean propagateToOwnerHierarchy;
    @Schema(description = "Propagation flag to specify if alarm should be propagated to the tenant entity", example = "true")
    private boolean propagateToTenant;
    @Schema(description = "JSON array of relation types that should be used for propagation. " +
            "By default, 'propagateRelationTypes' array is empty which means that the alarm will be propagated based on any relation type to parent entities. " +
            "This parameter should be used only in case when 'propagate' parameter is set to true, otherwise, 'propagateRelationTypes' array will be ignored.")
    private List<String> propagateRelationTypes;

    public Alarm() {
        super();
    }

    public Alarm(AlarmId id) {
        super(id);
    }

    public Alarm(Alarm alarm) {
        super(alarm.getId());
        this.createdTime = alarm.getCreatedTime();
        this.tenantId = alarm.getTenantId();
        this.customerId = alarm.getCustomerId();
        this.type = alarm.getType();
        this.originator = alarm.getOriginator();
        this.severity = alarm.getSeverity();
        this.assigneeId = alarm.getAssigneeId();
        this.startTs = alarm.getStartTs();
        this.endTs = alarm.getEndTs();
        this.acknowledged = alarm.isAcknowledged();
        this.ackTs = alarm.getAckTs();
        this.clearTs = alarm.getClearTs();
        this.cleared = alarm.isCleared();
        this.assignTs = alarm.getAssignTs();
        this.details = alarm.getDetails();
        this.propagate = alarm.isPropagate();
        this.propagateToOwner = alarm.isPropagateToOwner();
        this.propagateToOwnerHierarchy = alarm.isPropagateToOwnerHierarchy();
        this.propagateToTenant = alarm.isPropagateToTenant();
        this.propagateRelationTypes = alarm.getPropagateRelationTypes();
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(required = true, description = "representing type of the Alarm", example = "High Temperature Alarm")
    public String getName() {
        return type;
    }

    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.ALARM;
    }

    @Schema(description = "JSON object with the alarm Id. " +
            "Specify this field to update the alarm. " +
            "Referencing non-existing alarm Id will cause error. " +
            "Omit this field to create new alarm.")
    @Override
    public AlarmId getId() {
        return super.getId();
    }


    @Schema(description = "Timestamp of the alarm creation, in milliseconds", example = "1634058704567", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(required = true, description = "status of the Alarm", example = "ACTIVE_UNACK", accessMode = Schema.AccessMode.READ_ONLY)
    public AlarmStatus getStatus() {
        return toStatus(cleared, acknowledged);
    }

    public static AlarmStatus toStatus(boolean cleared, boolean acknowledged) {

        if (cleared) {
            return acknowledged ? AlarmStatus.CLEARED_ACK : AlarmStatus.CLEARED_UNACK;
        } else {
            return acknowledged ? AlarmStatus.ACTIVE_ACK : AlarmStatus.ACTIVE_UNACK;
        }
    }

}
