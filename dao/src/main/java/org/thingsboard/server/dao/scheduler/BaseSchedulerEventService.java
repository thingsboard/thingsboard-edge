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
package org.thingsboard.server.dao.scheduler;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEventWithCustomerInfo;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Service("SchedulerEventDaoService")
@Slf4j
public class BaseSchedulerEventService extends AbstractEntityService implements SchedulerEventService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_SCHEDULER_EVENT_ID = "Incorrect schedulerEventId ";

    @Autowired
    private SchedulerEventDao schedulerEventDao;

    @Autowired
    private SchedulerEventInfoDao schedulerEventInfoDao;

    @Autowired
    private EdgeService edgeService;

    @Autowired
    private DataValidator<SchedulerEvent> schedulerEventValidator;

    @Override
    public SchedulerEvent findSchedulerEventById(TenantId tenantId, SchedulerEventId schedulerEventId) {
        log.trace("Executing findSchedulerEventById [{}]", schedulerEventId);
        validateId(schedulerEventId, INCORRECT_SCHEDULER_EVENT_ID + schedulerEventId);
        return schedulerEventDao.findById(tenantId, schedulerEventId.getId());
    }

    @Override
    public SchedulerEventInfo findSchedulerEventInfoById(TenantId tenantId, SchedulerEventId schedulerEventId) {
        log.trace("Executing findSchedulerEventInfoById [{}]", schedulerEventId);
        validateId(schedulerEventId, INCORRECT_SCHEDULER_EVENT_ID + schedulerEventId);
        return schedulerEventInfoDao.findById(tenantId, schedulerEventId.getId());
    }

    @Override
    public SchedulerEventWithCustomerInfo findSchedulerEventWithCustomerInfoById(TenantId tenantId, SchedulerEventId schedulerEventId) {
        log.trace("Executing findSchedulerEventWithCustomerInfoById [{}]", schedulerEventId);
        validateId(schedulerEventId, INCORRECT_SCHEDULER_EVENT_ID + schedulerEventId);
        return schedulerEventInfoDao.findSchedulerEventWithCustomerInfoById(tenantId.getId(), schedulerEventId.getId());
    }

    @Override
    public ListenableFuture<SchedulerEventInfo> findSchedulerEventInfoByIdAsync(TenantId tenantId, SchedulerEventId schedulerEventId) {
        log.trace("Executing findSchedulerEventInfoByIdAsync [{}]", schedulerEventId);
        validateId(schedulerEventId, INCORRECT_SCHEDULER_EVENT_ID + schedulerEventId);
        return schedulerEventInfoDao.findByIdAsync(tenantId, schedulerEventId.getId());
    }

    @Override
    public ListenableFuture<List<SchedulerEventInfo>> findSchedulerEventInfoByIdsAsync(TenantId tenantId, List<SchedulerEventId> schedulerEventIds) {
        log.trace("Executing findSchedulerEventInfoByIdsAsync, tenantId [{}], schedulerEventIds [{}]", tenantId, schedulerEventIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateIds(schedulerEventIds, "Incorrect schedulerEventIds " + schedulerEventIds);
        return schedulerEventInfoDao.findSchedulerEventsByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(schedulerEventIds));
    }

    @Override
    public List<SchedulerEventInfo> findSchedulerEventsByTenantId(TenantId tenantId) {
        log.trace("Executing findSchedulerEventsByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return schedulerEventInfoDao.findSchedulerEventsByTenantId(tenantId.getId());
    }

    @Override
    public List<SchedulerEventWithCustomerInfo> findSchedulerEventsWithCustomerInfoByTenantId(TenantId tenantId) {
        log.trace("Executing findSchedulerEventsWithCustomerInfoByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return schedulerEventInfoDao.findSchedulerEventsWithCustomerInfoByTenantId(tenantId.getId());
    }

    @Override
    public List<SchedulerEventWithCustomerInfo> findSchedulerEventsByTenantIdAndType(TenantId tenantId, String type) {
        log.trace("Executing findSchedulerEventsByTenantIdAndType, tenantId [{}], type [{}]", tenantId, type);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateString(type, "Incorrect type " + type);
        return schedulerEventInfoDao.findSchedulerEventsByTenantIdAndType(tenantId.getId(), type);
    }

    @Override
    public List<SchedulerEventWithCustomerInfo> findSchedulerEventsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing findSchedulerEventsByTenantIdAndCustomerId, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        return schedulerEventInfoDao.findSchedulerEventsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId());
    }

    @Override
    public List<SchedulerEventWithCustomerInfo> findSchedulerEventsByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type) {
        log.trace("Executing findSchedulerEventsByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}]", tenantId, customerId, type);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateString(type, "Incorrect type " + type);
        return schedulerEventInfoDao.findSchedulerEventsByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type);
    }

    @Override
    public SchedulerEvent saveSchedulerEvent(SchedulerEvent schedulerEvent) {
        log.trace("Executing saveSchedulerEvent [{}]", schedulerEvent);
        schedulerEventValidator.validate(schedulerEvent, SchedulerEventInfo::getTenantId);
        return schedulerEventDao.save(schedulerEvent.getTenantId(), schedulerEvent);
    }

    @Override
    public void deleteSchedulerEvent(TenantId tenantId, SchedulerEventId schedulerEventId) {
        log.trace("Executing deleteSchedulerEvent [{}]", schedulerEventId);
        validateId(schedulerEventId, INCORRECT_SCHEDULER_EVENT_ID + schedulerEventId);
        deleteEntityRelations(tenantId, schedulerEventId);
        schedulerEventDao.removeById(tenantId, schedulerEventId.getId());
    }

    @Override
    public void deleteSchedulerEventsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteSchedulerEventsByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        List<SchedulerEventInfo> schedulerEvents = schedulerEventInfoDao.findSchedulerEventsByTenantId(tenantId.getId());
        for (SchedulerEventInfo schedulerEvent : schedulerEvents) {
            deleteSchedulerEvent(tenantId, schedulerEvent.getId());
        }
    }

    @Override
    public void deleteSchedulerEventsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteSchedulerEventsByTenantIdAndCustomerId, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        List<SchedulerEventWithCustomerInfo> schedulerEvents = schedulerEventInfoDao.findSchedulerEventsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId());
        for (SchedulerEventInfo schedulerEvent : schedulerEvents) {
            deleteSchedulerEvent(tenantId, schedulerEvent.getId());
        }
    }

    @Override
    public SchedulerEventInfo assignSchedulerEventToEdge(TenantId tenantId, SchedulerEventId schedulerEventId, EdgeId edgeId) {
        SchedulerEventInfo schedulerEventInfo = findSchedulerEventInfoById(tenantId, schedulerEventId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't assign scheduler event to non-existent edge!");
        }
        try {
            createRelation(tenantId, new EntityRelation(edgeId, schedulerEventId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to create scheduler event relation. Edge Id: [{}]", schedulerEventId, edgeId);
            throw new RuntimeException(e);
        }
        return schedulerEventInfo;
    }

    @Override
    public SchedulerEventInfo unassignSchedulerEventFromEdge(TenantId tenantId, SchedulerEventId schedulerEventId, EdgeId edgeId) {
        SchedulerEventInfo schedulerEventInfo = findSchedulerEventInfoById(tenantId, schedulerEventId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't unassign scheduler event from non-existent edge group!");
        }
        try {
            deleteRelation(tenantId, new EntityRelation(edgeId, schedulerEventId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to delete scheduler event relation. Edge group id: [{}]", schedulerEventId, edgeId);
            throw new RuntimeException(e);
        }
        return schedulerEventInfo;
    }

    @Override
    public PageData<SchedulerEventInfo> findSchedulerEventInfosByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink) {
        log.trace("Executing findSchedulerEventInfosByTenantIdAndEdgeId, tenantId [{}], edgeId [{}]", tenantId, edgeId);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validateId(edgeId, "Incorrect edgeId " + edgeId);
        return schedulerEventInfoDao.findSchedulerEventInfosByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), pageLink);
    }

    @Override
    public PageData<SchedulerEvent> findSchedulerEventsByTenantIdAndEdgeId(TenantId tenantId,
                                                                            EdgeId edgeId, PageLink pageLink) {
        log.trace("Executing findSchedulerEventsByTenantIdAndEdgeId, tenantId [{}], edgeId [{}]", tenantId, edgeId);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validateId(edgeId, "Incorrect edgeId " + edgeId);
        return schedulerEventDao.findSchedulerEventsByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), pageLink);
    }

    public static EntityId getOriginatorId(SchedulerEvent event) {
        return event.getOriginatorId() != null ? event.getOriginatorId() : event.getId();
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findSchedulerEventById(tenantId, new SchedulerEventId(entityId.getId())));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SCHEDULER_EVENT;
    }

}
