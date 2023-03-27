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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_EVENT_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_EVENT_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_EVENT_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_EVENT_ORIGINATOR_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_EVENT_ORIGINATOR_TYPE_PROPERTY;
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

    @Column(name = SCHEDULER_EVENT_ORIGINATOR_ID_PROPERTY)
    private UUID originatorId;

    @Enumerated(EnumType.STRING)
    @Column(name = SCHEDULER_EVENT_ORIGINATOR_TYPE_PROPERTY)
    private EntityType originatorType;

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
        if (schedulerEvent.getOriginatorId() != null) {
            this.originatorId = schedulerEvent.getOriginatorId().getId();
            this.originatorType = schedulerEvent.getOriginatorId().getEntityType();
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
        if (originatorId != null && originatorType != null) {
            schedulerEvent.setOriginatorId(EntityIdFactory.getByTypeAndUuid(originatorType, originatorId));
        }
        schedulerEvent.setName(name);
        schedulerEvent.setType(type);
        schedulerEvent.setAdditionalInfo(additionalInfo);
        schedulerEvent.setConfiguration(configuration);
        schedulerEvent.setSchedule(schedule);
        return schedulerEvent;
    }

}
