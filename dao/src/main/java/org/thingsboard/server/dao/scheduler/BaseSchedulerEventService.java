/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.firmware.FirmwareInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.firmware.FirmwareType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.FirmwareId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEventWithCustomerInfo;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.firmware.FirmwareService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.Validator;
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

    @Autowired
    private EdgeService edgeService;

    @Autowired
    private FirmwareService firmwareService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceProfileService deviceProfileService;

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
    public ListenableFuture<List<SchedulerEventInfo>> findSchedulerEventInfosByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId) {
        log.trace("Executing findSchedulerEventInfosByTenantIdAndEdgeId, tenantId [{}], edgeId [{}]", tenantId, edgeId);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validateId(edgeId, "Incorrect edgeId " + edgeId);
        return schedulerEventInfoDao.findSchedulerEventInfosByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), new TimePageLink(Integer.MAX_VALUE));
    }

    @Override
    public ListenableFuture<List<SchedulerEvent>> findSchedulerEventsByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId) {
        log.trace("Executing findSchedulerEventsByTenantIdAndEdgeId, tenantId [{}], edgeId [{}]", tenantId, edgeId);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validateId(edgeId, "Incorrect edgeId " + edgeId);
        return schedulerEventDao.findSchedulerEventsByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), new TimePageLink(Integer.MAX_VALUE));
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

                    boolean isFirmwareUpdate = "firmwareUpdate".equals(schedulerEvent.getType());
                    boolean isSoftwareUpdate = "softwareUpdate".equals(schedulerEvent.getType());

                    if (isFirmwareUpdate || isSoftwareUpdate) {
                        FirmwareId firmwareId =
                                JacksonUtil.convertValue(schedulerEvent.getConfiguration().get("msgBody"), FirmwareId.class);
                        if (firmwareId == null) {
                            throw new DataValidationException("SchedulerEvent firmwareId should be specified!");
                        }
                        FirmwareInfo firmwareInfo = firmwareService.findFirmwareById(tenantId, firmwareId);
                        if (firmwareInfo == null) {
                            throw new DataValidationException("Can't assign non-existent firmware!");
                        }

                        if ((isFirmwareUpdate && !FirmwareType.FIRMWARE.equals(firmwareInfo.getType()))
                                || (isSoftwareUpdate && !FirmwareType.SOFTWARE.equals(firmwareInfo.getType()))) {
                            throw new DataValidationException("SchedulerEvent Can't assign firmware with different type!");
                        }

                        EntityId originatorId = getOriginatorId(schedulerEvent.getConfiguration());

                        if (originatorId == null) {
                            throw new DataValidationException("SchedulerEvent originatorId should be specified!");
                        }

                        switch (originatorId.getEntityType()) {
                            case DEVICE:
                                Device device = deviceService.findDeviceById(tenantId, (DeviceId) originatorId);
                                if (!device.getDeviceProfileId().equals(firmwareInfo.getDeviceProfileId())) {
                                    throw new DataValidationException("SchedulerEvent can't assign firmware with different deviceProfile!");
                                }
                                break;
                            case DEVICE_PROFILE:
                                DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(tenantId, (DeviceProfileId) originatorId);
                                if (!deviceProfile.getId().equals(firmwareInfo.getDeviceProfileId())) {
                                    throw new DataValidationException("SchedulerEvent can't assign firmware with different deviceProfile!");
                                }
                                break;
                        }
                    }
                }
            };

    private EntityId getOriginatorId(JsonNode configuration) {
        EntityId originatorId = null;
        if (configuration.has("originatorId") && !configuration.get("originatorId").isNull()) {
            JsonNode entityId = configuration.get("originatorId");
            if (entityId != null) {
                if (entityId.has("entityType") && !entityId.get("entityType").isNull()
                        && entityId.has("id") && !entityId.get("id").isNull())
                    originatorId = EntityIdFactory.getByTypeAndId(entityId.get("entityType").asText(), entityId.get("id").asText());
            }
        }
        return originatorId;
    }

}
