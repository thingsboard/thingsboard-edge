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
package org.thingsboard.server.dao.scheduler;


import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEventWithCustomerInfo;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.List;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Service
@Slf4j
public class BaseSchedulerEventService extends AbstractEntityService implements SchedulerEventService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_SCHEDULER_EVENT_ID = "Incorrect schedulerEventId ";

    @Autowired
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    @Autowired
    private SchedulerEventDao schedulerEventDao;

    @Autowired
    private SchedulerEventInfoDao schedulerEventInfoDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private CustomerDao customerDao;

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

    private DataValidator<SchedulerEvent> schedulerEventValidator =
            new DataValidator<SchedulerEvent>() {

                @Override
                protected void validateCreate(TenantId tenantId, SchedulerEvent data) {
                    DefaultTenantProfileConfiguration profileConfiguration =
                            (DefaultTenantProfileConfiguration) tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
                    long maxSchedulerEvents = profileConfiguration.getMaxSchedulerEvents();
                    validateNumberOfEntitiesPerTenant(tenantId, schedulerEventDao, maxSchedulerEvents, EntityType.SCHEDULER_EVENT);
                }

                @Override
                protected void validateDataImpl(TenantId tenantId, SchedulerEvent schedulerEvent) {
                    if (StringUtils.isEmpty(schedulerEvent.getType())) {
                        throw new DataValidationException("SchedulerEvent type should be specified!");
                    }
                    if (StringUtils.isEmpty(schedulerEvent.getName())) {
                        throw new DataValidationException("SchedulerEvent name should be specified!");
                    }
                    if (schedulerEvent.getSchedule() == null) {
                        throw new DataValidationException("SchedulerEvent schedule configuration should be specified!");
                    }
                    if (schedulerEvent.getConfiguration() == null) {
                        throw new DataValidationException("SchedulerEvent configuration should be specified!");
                    }
                    if (schedulerEvent.getTenantId() == null) {
                        throw new DataValidationException("SchedulerEvent should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(tenantId, schedulerEvent.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("SchedulerEvent is referencing to non-existent tenant!");
                        }
                    }
                    if (schedulerEvent.getCustomerId() == null) {
                        schedulerEvent.setCustomerId(new CustomerId(NULL_UUID));
                    } else if (!schedulerEvent.getCustomerId().getId().equals(NULL_UUID)) {
                        Customer customer = customerDao.findById(tenantId, schedulerEvent.getCustomerId().getId());
                        if (customer == null) {
                            throw new DataValidationException("Can't assign schedulerEvent to non-existent customer!");
                        }
                        if (!customer.getTenantId().equals(schedulerEvent.getTenantId())) {
                            throw new DataValidationException("Can't assign schedulerEvent to customer from different tenant!");
                        }
                    }
                }
            };

}
