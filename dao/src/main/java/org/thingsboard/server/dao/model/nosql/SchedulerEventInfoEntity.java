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
package org.thingsboard.server.dao.model.nosql;

import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.model.type.JsonCodec;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.*;

@Table(name = SCHEDULER_EVENT_COLUMN_FAMILY_NAME)
@EqualsAndHashCode
@ToString
public final class SchedulerEventInfoEntity implements SearchTextEntity<SchedulerEventInfo> {

    @PartitionKey(value = 0)
    @Column(name = ID_PROPERTY)
    private UUID id;

    @PartitionKey(value = 1)
    @Column(name = SCHEDULER_EVENT_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @PartitionKey(value = 2)
    @Column(name = SCHEDULER_EVENT_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @PartitionKey(value = 3)
    @Column(name = SCHEDULER_EVENT_TYPE_PROPERTY)
    private String type;

    @Column(name = SCHEDULER_EVENT_NAME_PROPERTY)
    private String name;

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = SCHEDULER_EVENT_ADDITIONAL_INFO_PROPERTY, codec = JsonCodec.class)
    private JsonNode additionalInfo;

    @Column(name = SCHEDULER_EVENT_SCHEDULE_PROPERTY, codec = JsonCodec.class)
    private JsonNode schedule;

    public SchedulerEventInfoEntity() {
        super();
    }

    public SchedulerEventInfoEntity(SchedulerEventInfo schedulerEventInfo) {
        if (schedulerEventInfo.getId() != null) {
            this.id = schedulerEventInfo.getId().getId();
        }
        if (schedulerEventInfo.getTenantId() != null) {
            this.tenantId = schedulerEventInfo.getTenantId().getId();
        }
        if (schedulerEventInfo.getCustomerId() != null) {
            this.customerId = schedulerEventInfo.getCustomerId().getId();
        }
        this.name = schedulerEventInfo.getName();
        this.type = schedulerEventInfo.getType();
        this.additionalInfo = schedulerEventInfo.getAdditionalInfo();
        this.schedule = schedulerEventInfo.getSchedule();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public JsonNode getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(JsonNode additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public JsonNode getSchedule() {
        return schedule;
    }

    public void setSchedule(JsonNode schedule) {
        this.schedule = schedule;
    }

    @Override
    public String getSearchTextSource() {
        return getName();
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public String getSearchText() {
        return searchText;
    }

    @Override
    public SchedulerEventInfo toData() {
        SchedulerEventInfo schedulerEventInfo = new SchedulerEventInfo(new SchedulerEventId(id));
        schedulerEventInfo.setCreatedTime(UUIDs.unixTimestamp(id));
        if (tenantId != null) {
            schedulerEventInfo.setTenantId(new TenantId(tenantId));
        }
        if (customerId != null) {
            schedulerEventInfo.setCustomerId(new CustomerId(customerId));
        }
        schedulerEventInfo.setName(name);
        schedulerEventInfo.setType(type);
        schedulerEventInfo.setAdditionalInfo(additionalInfo);
        schedulerEventInfo.setSchedule(schedule);
        return schedulerEventInfo;
    }

}