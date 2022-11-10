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
package org.thingsboard.server.common.data.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@ApiModel
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SchedulerEventInfo extends SearchTextBasedWithAdditionalInfo<SchedulerEventId> implements HasName, TenantEntity, HasCustomerId, HasOwnerId {

    private static final long serialVersionUID = 2807343040519549363L;

    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @ApiModelProperty(position = 4, value = "JSON object with Customer Id", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private CustomerId customerId;
    @ApiModelProperty(position = 5, value = "JSON object with Originator Id", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private EntityId originatorId;
    @NoXss
    @Length(fieldName = "name")
    @ApiModelProperty(position = 6, value = "scheduler event name", example = "Weekly Dashboard Report")
    private String name;
    @ApiModelProperty(position = 7, value = "scheduler event type", example = "generateReport")
    @NoXss
    @Length(fieldName = "type")
    private String type;
    @ApiModelProperty(position = 8, value = "a JSON value with schedule time configuration", dataType = "com.fasterxml.jackson.databind.JsonNode")
    private transient JsonNode schedule;
    @JsonIgnore
    private byte[] scheduleBytes;

    public SchedulerEventInfo() {
        super();
    }

    public SchedulerEventInfo(SchedulerEventId id) {
        super(id);
    }

    public SchedulerEventInfo(SchedulerEventInfo schedulerEventInfo) {
        super(schedulerEventInfo);
        this.tenantId = schedulerEventInfo.getTenantId();
        this.customerId = schedulerEventInfo.getCustomerId();
        this.originatorId = schedulerEventInfo.getOriginatorId();
        this.name = schedulerEventInfo.getName();
        this.type = schedulerEventInfo.getType();
        this.setSchedule(schedulerEventInfo.getSchedule());
    }

    @Override
    public String getSearchText() {
        return getName();
    }

    @ApiModelProperty(position = 1, value = "JSON object with the scheduler event Id. " +
            "Specify this field to update the scheduler event. " +
            "Referencing non-existing scheduler event Id will cause error. " +
            "Omit this field to create new scheduler event" )
    @Override
    public SchedulerEventId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the scheduler event creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @ApiModelProperty(position = 10, value = "Additional parameters of the scheduler event", dataType = "com.fasterxml.jackson.databind.JsonNode")
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

    @Override
    public String getName() {
        return name;
    }

    @ApiModelProperty(position = 5, value = "JSON object with Customer or Tenant Id", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public EntityId getOwnerId() {
        return customerId != null && !customerId.isNullUid() ? customerId : tenantId;
    }

    @Override
    public void setOwnerId(EntityId entityId) {
        if (EntityType.CUSTOMER.equals(entityId.getEntityType())) {
            this.customerId = new CustomerId(entityId.getId());
        } else {
            this.customerId = new CustomerId(CustomerId.NULL_UUID);
        }
    }

    public JsonNode getSchedule() {
        return SearchTextBasedWithAdditionalInfo.getJson(() -> schedule, () -> scheduleBytes);
    }

    public void setSchedule(JsonNode data) {
        setJson(data, json -> this.schedule = json, bytes -> this.scheduleBytes = bytes);
    }

    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.SCHEDULER_EVENT;
    }

}
