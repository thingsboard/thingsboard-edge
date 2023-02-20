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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.List;

/**
 * Created by ashvayka on 11.05.17.
 */
@ApiModel
@Data
@Builder
@AllArgsConstructor
public class Alarm extends BaseData<AlarmId> implements HasName, TenantEntity, HasCustomerId {

    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @ApiModelProperty(position = 4, value = "JSON object with Customer Id", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private CustomerId customerId;

    @NoXss
    @ApiModelProperty(position = 6, required = true, value = "representing type of the Alarm", example = "High Temperature Alarm")
    @Length(fieldName = "type")
    private String type;
    @ApiModelProperty(position = 7, required = true, value = "JSON object with alarm originator id")
    private EntityId originator;
    @ApiModelProperty(position = 8, required = true, value = "Alarm severity", example = "CRITICAL")
    private AlarmSeverity severity;
    @ApiModelProperty(position = 9, required = true, value = "Alarm status", example = "CLEARED_UNACK")
    private AlarmStatus status;
    @ApiModelProperty(position = 10, value = "Timestamp of the alarm start time, in milliseconds", example = "1634058704565")
    private long startTs;
    @ApiModelProperty(position = 11, value = "Timestamp of the alarm end time(last time update), in milliseconds", example = "1634111163522")
    private long endTs;
    @ApiModelProperty(position = 12, value = "Timestamp of the alarm acknowledgement, in milliseconds", example = "1634115221948")
    private long ackTs;
    @ApiModelProperty(position = 13, value = "Timestamp of the alarm clearing, in milliseconds", example = "1634114528465")
    private long clearTs;
    @ApiModelProperty(position = 14, value = "JSON object with alarm details")
    private transient JsonNode details;
    @ApiModelProperty(position = 15, value = "Propagation flag to specify if alarm should be propagated to parent entities of alarm originator", example = "true")
    private boolean propagate;
    @ApiModelProperty(position = 16, value = "Propagation flag to specify if alarm should be propagated to the owner (tenant or customer) of alarm originator", example = "true")
    private boolean propagateToOwner;
    @ApiModelProperty(position = 17, value = "Propagation flag to specify if alarm should be propagated to the owner (tenant or customer) and all parent owners in the customer hierarchy", example = "true")
    private boolean propagateToOwnerHierarchy;
    @ApiModelProperty(position = 18, value = "Propagation flag to specify if alarm should be propagated to the tenant entity", example = "true")
    private boolean propagateToTenant;
    @ApiModelProperty(position = 19, value = "JSON array of relation types that should be used for propagation. " +
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
        this.status = alarm.getStatus();
        this.startTs = alarm.getStartTs();
        this.endTs = alarm.getEndTs();
        this.ackTs = alarm.getAckTs();
        this.clearTs = alarm.getClearTs();
        this.details = alarm.getDetails();
        this.propagate = alarm.isPropagate();
        this.propagateToOwner = alarm.isPropagateToOwner();
        this.propagateToOwnerHierarchy = alarm.isPropagateToOwnerHierarchy();
        this.propagateToTenant = alarm.isPropagateToTenant();
        this.propagateRelationTypes = alarm.getPropagateRelationTypes();
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @ApiModelProperty(position = 5, required = true, value = "representing type of the Alarm", example = "High Temperature Alarm")
    public String getName() {
        return type;
    }

    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.ALARM;
    }

    @ApiModelProperty(position = 1, value = "JSON object with the alarm Id. " +
            "Specify this field to update the alarm. " +
            "Referencing non-existing alarm Id will cause error. " +
            "Omit this field to create new alarm." )
    @Override
    public AlarmId getId() {
        return super.getId();
    }


    @ApiModelProperty(position = 2, value = "Timestamp of the alarm creation, in milliseconds", example = "1634058704567", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

}
