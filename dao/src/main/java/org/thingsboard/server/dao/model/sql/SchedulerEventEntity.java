/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_EVENT_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_EVENT_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_EVENT_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_EVENT_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_EVENT_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SEARCH_TEXT_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = SCHEDULER_EVENT_COLUMN_FAMILY_NAME)
public final class SchedulerEventEntity extends BaseSqlEntity<SchedulerEvent> implements SearchTextEntity<SchedulerEvent> {

    @Column(name = SCHEDULER_EVENT_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = SCHEDULER_EVENT_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

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
    @Column(name = ModelConstants.SCHEDULER_EVENT_CONFIGURATION_PROPERTY)
    private JsonNode configuration;

    @Type(type = "json")
    @Column(name = ModelConstants.SCHEDULER_EVENT_SCHEDULE_PROPERTY)
    private JsonNode schedule;

    public SchedulerEventEntity() {
        super();
    }

    public SchedulerEventEntity(SchedulerEvent schedulerEvent) {
        this.createdTime = schedulerEvent.getCreatedTime();
        if (schedulerEvent.getId() != null) {
            this.setUuid(schedulerEvent.getId().getId());
        }
        if (schedulerEvent.getTenantId() != null) {
            this.tenantId = schedulerEvent.getTenantId().getId();
        }
        if (schedulerEvent.getCustomerId() != null) {
            this.customerId = schedulerEvent.getCustomerId().getId();
        }
        this.name = schedulerEvent.getName();
        this.type = schedulerEvent.getType();
        this.additionalInfo = schedulerEvent.getAdditionalInfo();
        this.configuration = schedulerEvent.getConfiguration();
        this.schedule = schedulerEvent.getSchedule();
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
    public SchedulerEvent toData() {
        SchedulerEvent schedulerEvent = new SchedulerEvent(new SchedulerEventId(id));
        schedulerEvent.setCreatedTime(createdTime);
        if (tenantId != null) {
            schedulerEvent.setTenantId(new TenantId(tenantId));
        }
        if (customerId != null) {
            schedulerEvent.setCustomerId(new CustomerId(customerId));
        }
        schedulerEvent.setName(name);
        schedulerEvent.setType(type);
        schedulerEvent.setAdditionalInfo(additionalInfo);
        schedulerEvent.setConfiguration(configuration);
        schedulerEvent.setSchedule(schedule);
        return schedulerEvent;
    }

}
