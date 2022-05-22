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
package org.thingsboard.server.service.entitiy;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.GroupEntity;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterableByTenantIdEntityId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.ClaimDevicesService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.service.edge.EdgeNotificationService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.mail.MessagingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractTbEntityService {

    protected static final int DEFAULT_PAGE_SIZE = 1000;

    @Value("${server.log_controller_error_stack_trace}")
    @Getter
    private boolean logControllerErrorStackTrace;
    @Value("${edges.enabled}")
    @Getter
    protected boolean edgesEnabled;

    @Autowired
    protected DbCallbackExecutorService dbExecutor;
    @Autowired
    protected TbNotificationEntityService notificationEntityService;
    @Autowired(required = false)
    protected EdgeService edgeService;
    @Autowired
    protected AlarmService alarmService;
    @Autowired
    protected DeviceService deviceService;
    @Autowired
    protected AssetService assetService;
    @Autowired
    protected DeviceCredentialsService deviceCredentialsService;
    @Autowired
    protected TenantService tenantService;
    @Autowired
    protected CustomerService customerService;
    @Autowired
    protected ClaimDevicesService claimDevicesService;
    @Autowired
    protected TbTenantProfileCache tenantProfileCache;
    @Autowired
    protected RuleChainService ruleChainService;
    @Autowired
    protected EdgeNotificationService edgeNotificationService;
    @Autowired
    protected EntityGroupService entityGroupService;
    @Autowired
    protected TbClusterService tbClusterService;
    @Autowired
    protected DashboardService dashboardService;
    @Autowired
    protected EntityViewService entityViewService;
    @Autowired
    protected TelemetrySubscriptionService tsSubService;
    @Autowired
    protected AttributesService attributesService;
    @Autowired
    protected AccessControlService accessControlService;

    protected ListenableFuture<Void> removeAlarmsByEntityId(TenantId tenantId, EntityId entityId) {
        ListenableFuture<PageData<AlarmInfo>> alarmsFuture =
                alarmService.findAlarms(tenantId, new AlarmQuery(entityId, new TimePageLink(Integer.MAX_VALUE), null, null, false));

        ListenableFuture<List<AlarmId>> alarmIdsFuture = Futures.transform(alarmsFuture, page ->
                page.getData().stream().map(AlarmInfo::getId).collect(Collectors.toList()), dbExecutor);

        return Futures.transform(alarmIdsFuture, ids -> {
            ids.stream().map(alarmId -> alarmService.deleteAlarm(tenantId, alarmId)).collect(Collectors.toList());
            return null;
        }, dbExecutor);
    }

    protected <T> T checkNotNull(T reference) throws ThingsboardException {
        return checkNotNull(reference, "Requested item wasn't found!");
    }

    protected <T> T checkNotNull(T reference, String notFoundMessage) throws ThingsboardException {
        if (reference == null) {
            throw new ThingsboardException(notFoundMessage, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        return reference;
    }

    protected <T> T checkNotNull(Optional<T> reference) throws ThingsboardException {
        return checkNotNull(reference, "Requested item wasn't found!");
    }

    protected <T> T checkNotNull(Optional<T> reference, String notFoundMessage) throws ThingsboardException {
        if (reference.isPresent()) {
            return reference.get();
        } else {
            throw new ThingsboardException(notFoundMessage, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
    }

    protected ThingsboardException handleException(Exception exception) {
        return handleException(exception, true);
    }

    protected ThingsboardException handleException(Exception exception, boolean logException) {
        if (logException && logControllerErrorStackTrace) {
            log.error("Error [{}]", exception.getMessage(), exception);
        }

        String cause = "";
        if (exception.getCause() != null) {
            cause = exception.getCause().getClass().getCanonicalName();
        }

        if (exception instanceof ThingsboardException) {
            return (ThingsboardException) exception;
        } else if (exception instanceof IllegalArgumentException || exception instanceof IncorrectParameterException
                || exception instanceof DataValidationException || cause.contains("IncorrectParameterException")) {
            return new ThingsboardException(exception.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        } else if (exception instanceof MessagingException) {
            return new ThingsboardException("Unable to send mail: " + exception.getMessage(), ThingsboardErrorCode.GENERAL);
        } else {
            return new ThingsboardException(exception.getMessage(), exception, ThingsboardErrorCode.GENERAL);
        }
    }

    @SuppressWarnings("unchecked")
    protected <I extends EntityId> I emptyId(EntityType entityType) {
        return (I) EntityIdFactory.getByTypeAndUuid(entityType, ModelConstants.NULL_UUID);
    }

    protected List<EdgeId> findRelatedEdgeIds(TenantId tenantId, EntityId entityId) {
        if (!edgesEnabled) {
            return null;
        }
        if (EntityType.EDGE.equals(entityId.getEntityType())) {
            return Collections.singletonList(new EdgeId(entityId.getId()));
        }
        PageDataIterableByTenantIdEntityId<EdgeId> relatedEdgeIdsIterator =
                new PageDataIterableByTenantIdEntityId<>(edgeService::findRelatedEdgeIdsByEntityId, tenantId, entityId, DEFAULT_PAGE_SIZE);
        List<EdgeId> result = new ArrayList<>();
        for (EdgeId edgeId : relatedEdgeIdsIterator) {
            result.add(edgeId);
        }
        return result;
    }

    protected <I extends EntityId, T extends GroupEntity<I>> void createOrUpdateGroupEntity(TenantId tenantId, T entity, EntityGroup entityGroup,
                                                                                  ActionType actionType, SecurityUser user) throws ThingsboardException {
        EntityId entityId = entity.getId();
        CustomerId customerId = entity.getCustomerId();
        if (entityGroup != null && actionType == ActionType.ADDED) {
            EntityGroupId entityGroupId = entityGroup.getId();
            entityGroupService.addEntityToEntityGroup(tenantId, entityGroupId, entityId);
            notificationEntityService.notifyAddToEntityGroup(tenantId, entityId, entity, customerId, entityGroupId, user,
                    entityId.toString(), entityGroupId.toString(), entityGroup.getName());
        }

        notificationEntityService.notifyCreateOrUpdateEntity(tenantId, entityId, entity, customerId, actionType, user);
    }
}
