/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import com.datastax.oss.driver.api.core.uuid.Uuids;
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
import javax.persistence.MappedSuperclass;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_EVENT_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_EVENT_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_EVENT_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_EVENT_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SEARCH_TEXT_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeDef(name = "json", typeClass = JsonStringType.class)
@MappedSuperclass
public abstract class AbstractSchedulerEventInfoEntity<T extends SchedulerEventInfo> extends BaseSqlEntity<T> implements SearchTextEntity<T> {

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
    @Column(name = ModelConstants.SCHEDULER_EVENT_SCHEDULE_PROPERTY)
    private JsonNode schedule;

    public AbstractSchedulerEventInfoEntity() {
        super();
    }

    public AbstractSchedulerEventInfoEntity(SchedulerEventInfo schedulerEventInfo) {
        if (schedulerEventInfo.getId() != null) {
            this.setUuid(schedulerEventInfo.getId().getId());
        }
        this.setCreatedTime(schedulerEventInfo.getCreatedTime());
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

    public AbstractSchedulerEventInfoEntity(SchedulerEventInfoEntity schedulerEventInfoEntity) {
        this.setId(schedulerEventInfoEntity.getId());
        this.setCreatedTime(schedulerEventInfoEntity.getCreatedTime());
        this.tenantId = schedulerEventInfoEntity.getTenantId();
        this.customerId = schedulerEventInfoEntity.getCustomerId();
        this.type = schedulerEventInfoEntity.getType();
        this.name = schedulerEventInfoEntity.getName();
        this.searchText = schedulerEventInfoEntity.getSearchText();
        this.schedule = schedulerEventInfoEntity.getSchedule();
        this.additionalInfo = schedulerEventInfoEntity.getAdditionalInfo();
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

    protected SchedulerEventInfo toSchedulerEventInfo() {
        SchedulerEventInfo schedulerEventInfo = new SchedulerEventInfo(new SchedulerEventId(id));
        schedulerEventInfo.setCreatedTime(createdTime);
        if (tenantId != null) {
            schedulerEventInfo.setTenantId(new TenantId(tenantId));
        }
        if (customerId != null) {
            schedulerEventInfo.setCustomerId(new CustomerId(customerId));
        }
        schedulerEventInfo.setName(name);
        schedulerEventInfo.setType(type);
        schedulerEventInfo.setSchedule(schedule);
        schedulerEventInfo.setAdditionalInfo(additionalInfo);
        return schedulerEventInfo;
    }

}
