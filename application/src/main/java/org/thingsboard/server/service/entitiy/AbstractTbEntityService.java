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
import org.thingsboard.server.common.data.User;
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
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;
import org.thingsboard.server.service.telemetry.AlarmSubscriptionService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractTbEntityService {

    @Value("${server.log_controller_error_stack_trace}")
    @Getter
    private boolean logControllerErrorStackTrace;

    @Autowired
    protected DbCallbackExecutorService dbExecutor;
    @Autowired(required = false)
    protected TbNotificationEntityService notificationEntityService;
    @Autowired(required = false)
    protected EdgeService edgeService;
    @Autowired
    protected AlarmService alarmService;
    @Autowired
    protected AlarmSubscriptionService alarmSubscriptionService;
    @Autowired
    protected CustomerService customerService;
    @Autowired
    protected TbClusterService tbClusterService;
    @Autowired
    protected EntityGroupService entityGroupService;
    @Autowired(required = false)
    private EntitiesVersionControlService vcService;

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

    protected <I extends EntityId> I emptyId(EntityType entityType) {
        return (I) EntityIdFactory.getByTypeAndUuid(entityType, ModelConstants.NULL_UUID);
    }

    protected <I extends EntityId, T extends GroupEntity<I>> void createOrUpdateGroupEntity(TenantId tenantId, T entity, EntityGroup entityGroup,
                                                                                            ActionType actionType, User user) throws ThingsboardException {
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

    protected ListenableFuture<UUID> autoCommit(User user, EntityId entityId) throws Exception {
        if (vcService != null) {
            return vcService.autoCommit(user, entityId);
        } else {
            // We do not support auto-commit for rule engine
            return Futures.immediateFailedFuture(new RuntimeException("Operation not supported!"));
        }
    }

    protected ListenableFuture<UUID> autoCommit(User user, EntityType entityType, List<UUID> entityIds) throws Exception {
        if (vcService != null) {
            return vcService.autoCommit(user, entityType, entityIds);
        } else {
            // We do not support auto-commit for rule engine
            return Futures.immediateFailedFuture(new RuntimeException("Operation not supported!"));
        }
    }

    protected ListenableFuture<UUID> autoCommit(User user, EntityType entityType, EntityGroupId groupId) throws Exception {
        if (vcService != null) {
            return vcService.autoCommit(user, entityType, groupId);
        } else {
            // We do not support auto-commit for rule engine
            return Futures.immediateFailedFuture(new RuntimeException("Operation not supported!"));
        }
    }
}
