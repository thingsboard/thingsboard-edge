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
package org.thingsboard.server.common.data.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SchedulerEventInfo extends BaseDataWithAdditionalInfo<SchedulerEventId> implements HasName, TenantEntity, HasCustomerId, HasOwnerId {

    private static final long serialVersionUID = 2807343040519549363L;

    @Schema(description = "JSON object with Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @Schema(description = "JSON object with Customer Id", accessMode = Schema.AccessMode.READ_ONLY)
    private CustomerId customerId;
    @Schema(description = "JSON object with Originator Id", accessMode = Schema.AccessMode.READ_ONLY)
    private EntityId originatorId;
    @NoXss
    @Length(fieldName = "name")
    @Schema(description = "scheduler event name", example = "Weekly Dashboard Report")
    private String name;
    @Schema(description = "scheduler event type", example = "generateReport")
    @NoXss
    @Length(fieldName = "type")
    private String type;
    @Schema(description = "a JSON value with schedule time configuration", implementation = com.fasterxml.jackson.databind.JsonNode.class)
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

    @Schema(description = "JSON object with the scheduler event Id. " +
            "Specify this field to update the scheduler event. " +
            "Referencing non-existing scheduler event Id will cause error. " +
            "Omit this field to create new scheduler event" )
    @Override
    public SchedulerEventId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the scheduler event creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "Additional parameters of the scheduler event", implementation = com.fasterxml.jackson.databind.JsonNode.class)
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

    @Override
    public String getName() {
        return name;
    }

    @Schema(description = "JSON object with Customer or Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
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
        return BaseDataWithAdditionalInfo.getJson(() -> schedule, () -> scheduleBytes);
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
