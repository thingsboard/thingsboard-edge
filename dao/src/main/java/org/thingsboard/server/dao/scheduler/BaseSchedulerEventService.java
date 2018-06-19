/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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


import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetSearchQuery;
import org.thingsboard.server.common.data.group.EntityField;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.service.Validator.*;

@Service
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
    private TenantDao tenantDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private EntityService entityService;

    @Override
    public SchedulerEvent findSchedulerEventById(SchedulerEventId schedulerEventId) {
        log.trace("Executing findSchedulerEventById [{}]", schedulerEventId);
        validateId(schedulerEventId, INCORRECT_SCHEDULER_EVENT_ID + schedulerEventId);
        return schedulerEventDao.findById(schedulerEventId.getId());
    }

    @Override
    public List<SchedulerEventInfo> findSchedulerEventsByTenantId(TenantId tenantId) {
        return null;
    }

    @Override
    public List<SchedulerEventInfo> findSchedulerEventsByTenantIdAndType(TenantId tenantId, String type) {
        return null;
    }

    @Override
    public List<SchedulerEventInfo> findSchedulerEventsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        return null;
    }

    @Override
    public List<SchedulerEventInfo> findSchedulerEventsByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type) {
        return null;
    }

    @Override
    public SchedulerEvent saveSchedulerEvent(SchedulerEvent schedulerEvent) {
        log.trace("Executing saveSchedulerEvent [{}]", schedulerEvent);
        schedulerEventValidator.validate(schedulerEvent);
        SchedulerEvent savedSchedulerEvent = schedulerEventDao.save(schedulerEvent);
        return savedSchedulerEvent;
    }

    @Override
    public void deleteSchedulerEvent(SchedulerEventId schedulerEventId) {
        log.trace("Executing deleteSchedulerEvent [{}]", schedulerEventId);
        validateId(schedulerEventId, INCORRECT_SCHEDULER_EVENT_ID + schedulerEventId);
        deleteEntityRelations(schedulerEventId);
        schedulerEventDao.removeById(schedulerEventId.getId());
    }

    @Override
    public void deleteSchedulerEventsByTenantId(TenantId tenantId) {

    }


    private DataValidator<SchedulerEvent> schedulerEventValidator =
            new DataValidator<SchedulerEvent>() {

                @Override
                protected void validateDataImpl(SchedulerEvent schedulerEvent) {
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
                        Tenant tenant = tenantDao.findById(schedulerEvent.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("SchedulerEvent is referencing to non-existent tenant!");
                        }
                    }
                    if (schedulerEvent.getCustomerId() == null) {
                        schedulerEvent.setCustomerId(new CustomerId(NULL_UUID));
                    } else if (!schedulerEvent.getCustomerId().getId().equals(NULL_UUID)) {
                        Customer customer = customerDao.findById(schedulerEvent.getCustomerId().getId());
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
