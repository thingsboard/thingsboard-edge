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
package org.thingsboard.server.dao.model.sql;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import static org.thingsboard.server.dao.model.ModelConstants.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = SCHEDULER_EVENT_COLUMN_FAMILY_NAME)
public final class SchedulerEventInfoEntity extends BaseSqlEntity<SchedulerEventInfo> implements SearchTextEntity<SchedulerEventInfo> {

    @Column(name = SCHEDULER_EVENT_TENANT_ID_PROPERTY)
    private String tenantId;

    @Column(name = SCHEDULER_EVENT_CUSTOMER_ID_PROPERTY)
    private String customerId;

    @Column(name = SCHEDULER_EVENT_NAME_PROPERTY)
    private String name;

    @Column(name = SCHEDULER_EVENT_TYPE_PROPERTY)
    private String type;

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Type(type = "json")
    @Column(name = ModelConstants.SCHEDULER_EVENT_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    @Type(type = "json")
    @Column(name = ModelConstants.SCHEDULER_EVENT_SCHEDULE_PROPERTY)
    private JsonNode schedule;

    public SchedulerEventInfoEntity() {
        super();
    }

    public SchedulerEventInfoEntity(SchedulerEventInfo schedulerEventInfo) {
        if (schedulerEventInfo.getId() != null) {
            this.setId(schedulerEventInfo.getId().getId());
        }
        if (schedulerEventInfo.getTenantId() != null) {
            this.tenantId = UUIDConverter.fromTimeUUID(schedulerEventInfo.getTenantId().getId());
        }
        if (schedulerEventInfo.getCustomerId() != null) {
            this.customerId = UUIDConverter.fromTimeUUID(schedulerEventInfo.getCustomerId().getId());
        }
        this.name = schedulerEventInfo.getName();
        this.type = schedulerEventInfo.getType();
        this.additionalInfo = schedulerEventInfo.getAdditionalInfo();
        this.schedule = schedulerEventInfo.getSchedule();
    }

    @Override
    public String getSearchTextSource() {
        return name;
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
        SchedulerEventInfo schedulerEventInfo = new SchedulerEventInfo(new SchedulerEventId(UUIDConverter.fromString(id)));
        schedulerEventInfo.setCreatedTime(UUIDs.unixTimestamp(UUIDConverter.fromString(id)));
        if (tenantId != null) {
            schedulerEventInfo.setTenantId(new TenantId(UUIDConverter.fromString(tenantId)));
        }
        if (customerId != null) {
            schedulerEventInfo.setCustomerId(new CustomerId(UUIDConverter.fromString(customerId)));
        }
        schedulerEventInfo.setName(name);
        schedulerEventInfo.setType(type);
        schedulerEventInfo.setAdditionalInfo(additionalInfo);
        schedulerEventInfo.setSchedule(schedule);
        return schedulerEventInfo;
    }

}