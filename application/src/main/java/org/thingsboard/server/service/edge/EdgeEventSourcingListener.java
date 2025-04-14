/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.edge;

import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.EntityAlarm;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.edge.EdgeSynchronizationManager;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.RelationActionEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.tenant.TenantService;

/**
 * This event listener does not support async event processing because relay on ThreadLocal
 * Another possible approach is to implement a special annotation and a bunch of classes similar to TransactionalApplicationListener
 * This class is the simplest approach to maintain edge synchronization within the single class.
 * <p>
 * For async event publishers, you have to decide whether publish event on creating async task in the same thread where dao method called
 * @Autowired
 * EdgeEventSynchronizationManager edgeSynchronizationManager
 * ...
 *   //some async write action make future
 *   if (!edgeSynchronizationManager.isSync()) {
 *     future.addCallback(eventPublisher.publishEvent(...))
 *   }
 * */
@Slf4j
@Component
@RequiredArgsConstructor
public class EdgeEventSourcingListener {

    private final TbClusterService tbClusterService;
    private final TenantService tenantService;

    private final EdgeSynchronizationManager edgeSynchronizationManager;

    @PostConstruct
    public void init() {
        log.debug("EdgeEventSourcingListener initiated");
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(SaveEntityEvent<?> event) {
        if (Boolean.FALSE.equals(event.getBroadcastEvent())) {
            log.trace("Ignoring event {}", event);
            return;
        }

        try {
            if (!isValidSaveEntityEventForEdgeProcessing(event)) {
                return;
            }
            log.trace("[{}] SaveEntityEvent called: {}", event.getTenantId(), event);
            boolean isCreated = Boolean.TRUE.equals(event.getCreated());
            String body = getBodyMsgForEntityEvent(event.getEntity());
            EdgeEventType type = getEdgeEventTypeForEntityEvent(event.getEntity());
            EdgeEventActionType action = getActionForEntityEvent(event.getEntity(), isCreated);
            tbClusterService.sendNotificationMsgToEdge(event.getTenantId(), null, event.getEntityId(),
                    body, type, action, edgeSynchronizationManager.getEdgeId().get());
        } catch (Exception e) {
            log.error("[{}] failed to process SaveEntityEvent: {}", event.getTenantId(), event, e);
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(DeleteEntityEvent<?> event) {
        TenantId tenantId = event.getTenantId();
        if (!tenantId.isSysTenantId() && !tenantService.tenantExists(tenantId)) {
            log.debug("[{}] Ignoring DeleteEntityEvent because tenant does not exist: {}", tenantId, event);
            return;
        }
        try {
            EntityType entityType = event.getEntityId().getEntityType();
            if (EntityType.TENANT.equals(entityType) || EntityType.EDGE.equals(entityType)) {
                return;
            }
            log.trace("[{}] DeleteEntityEvent called: {}", tenantId, event);
            EdgeEventType type = getEdgeEventTypeForEntityEvent(event.getEntity());
            EdgeEventActionType actionType = getEdgeEventActionTypeForEntityEvent(event.getEntity());
            tbClusterService.sendNotificationMsgToEdge(tenantId, null, event.getEntityId(),
                    JacksonUtil.toString(event.getEntity()), type, actionType,
                    edgeSynchronizationManager.getEdgeId().get());
        } catch (Exception e) {
            log.error("[{}] failed to process DeleteEntityEvent: {}", tenantId, event, e);
        }
    }

    private EdgeEventActionType getEdgeEventActionTypeForEntityEvent(Object entity) {
        if (entity instanceof AlarmComment) {
            return EdgeEventActionType.DELETED_COMMENT;
        } else if (entity instanceof Alarm) {
            return EdgeEventActionType.ALARM_DELETE;
        }
        return EdgeEventActionType.DELETED;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(ActionEntityEvent<?> event) {
        if (event.getEntityId() != null && EntityType.DEVICE.equals(event.getEntityId().getEntityType()) && ActionType.ASSIGNED_TO_TENANT.equals(event.getActionType())) {
            return;
        }
        if (EntityType.ALARM.equals(event.getEntityId().getEntityType())) {
            return;
        }
        try {
            if (event.getEntityGroup() != null) {
                if (event.getEntityGroup().isGroupAll()) {
                    log.trace("skipping entity in case of 'All' group: {}", event);
                    return;
                }
                if (ActionType.ASSIGNED_TO_EDGE.equals(event.getActionType()) && event.getEntityGroup().isEdgeGroupAll()) {
                    log.trace("skipping entity in case of 'Edge All' group: {}", event);
                    return;
                }
            }
            if (EntityType.RULE_CHAIN.equals(event.getEntityId() != null ? event.getEntityId().getEntityType() : null) && event.getEdgeId() != null && event.getActionType().equals(ActionType.ASSIGNED_TO_EDGE)) {
                try {
                    Edge edge = JacksonUtil.fromString(event.getBody(), Edge.class);
                    if (edge != null && new RuleChainId(event.getEntityId().getId()).equals(edge.getRootRuleChainId())) {
                        log.trace("[{}] skipping ASSIGNED_TO_EDGE event of RULE_CHAIN entity in case Edge Root Rule Chain: {}", event.getTenantId(), event);
                        return;
                    }
                } catch (Exception ignored) {
                    return;
                }
            }
            EntityType entityGroupType = event.getEntityGroup() != null ? event.getEntityGroup().getType() : null;
            EntityGroupId entityGroupId = event.getEntityGroup() != null ? event.getEntityGroup().getId() : null;
            log.trace("[{}] ActionEntityEvent called: {}", event.getTenantId(), event);
            tbClusterService.sendNotificationMsgToEdge(event.getTenantId(), event.getEdgeId(), event.getEntityId(),
                    event.getBody(), event.getEdgeEventType(), EdgeUtils.getEdgeEventActionTypeByActionType(event.getActionType()),
                    entityGroupType, entityGroupId, edgeSynchronizationManager.getEdgeId().get());
        } catch (Exception e) {
            log.error("[{}] failed to process ActionEntityEvent: {}", event.getTenantId(), event, e);
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(RelationActionEvent event) {
        try {
            TenantId tenantId = event.getTenantId();
            if (!tenantId.isSysTenantId() && !tenantService.tenantExists(tenantId)) {
                log.debug("[{}] Ignoring RelationActionEvent because tenant does not exist: {}", tenantId, event);
                return;
            }
            EntityRelation relation = event.getRelation();
            if (relation == null) {
                log.trace("[{}] skipping RelationActionEvent event in case relation is null: {}", event.getTenantId(), event);
                return;
            }
            if (!RelationTypeGroup.COMMON.equals(relation.getTypeGroup())) {
                log.trace("[{}] skipping RelationActionEvent event in case NOT COMMON relation type group: {}", event.getTenantId(), event);
                return;
            }
            log.trace("[{}] RelationActionEvent called: {}", event.getTenantId(), event);
            tbClusterService.sendNotificationMsgToEdge(event.getTenantId(), null, null,
                    JacksonUtil.toString(relation), EdgeEventType.RELATION, EdgeUtils.getEdgeEventActionTypeByActionType(event.getActionType()),
                    edgeSynchronizationManager.getEdgeId().get());
        } catch (Exception e) {
            log.error("[{}] failed to process RelationActionEvent: {}", event.getTenantId(), event, e);
        }
    }

    private boolean isValidSaveEntityEventForEdgeProcessing(SaveEntityEvent<?> event) {
        Object entity = event.getEntity();
        Object oldEntity = event.getOldEntity();
        if (event.getEntityId() != null) {
            switch (event.getEntityId().getEntityType()) {
                case RULE_CHAIN:
                    if (entity instanceof RuleChain ruleChain) {
                        return RuleChainType.EDGE.equals(ruleChain.getType());
                    }
                    break;
                case USER:
                    if (entity instanceof User user) {
                        if (Authority.SYS_ADMIN.equals(user.getAuthority())) {
                            return false;
                        }
                        if (oldEntity != null) {
                            user = JacksonUtil.clone(user);
                            User oldUser = JacksonUtil.clone((User) oldEntity);
                            cleanUpUserAdditionalInfo(oldUser);
                            cleanUpUserAdditionalInfo(user);
                            return !user.equals(oldUser);
                        }
                    }
                    break;
                case OTA_PACKAGE:
                    if (entity instanceof OtaPackageInfo otaPackageInfo) {
                        return otaPackageInfo.hasUrl() || otaPackageInfo.isHasData();
                    }
                    break;
                case ALARM:
                    if (entity instanceof AlarmApiCallResult || entity instanceof Alarm || entity instanceof EntityAlarm) {
                        return false;
                    }
                    break;
                case TENANT:
                    return !event.getCreated();
                case CONVERTER:
                    Converter converter = (Converter) event.getEntity();
                    return converter.isEdgeTemplate();
                case INTEGRATION:
                    Integration integration = (Integration) event.getEntity();
                    return integration.isEdgeTemplate();
                case ENTITY_GROUP:
                    if (event.getEntity() instanceof EntityGroup entityGroup) {
                        if (entityGroup.isGroupAll()) {
                            log.trace("skipping entity in case of 'All' group: {}", entityGroup);
                            return false;
                        }
                        if (entityGroup.isEdgeGroupAll()) {
                            log.trace("skipping entity in case of Edge 'All' group: {}", entityGroup);
                            return false;
                        }
                        return !event.getCreated();
                    }
                    break;
                case API_USAGE_STATE, EDGE:
                    return false;
                case DOMAIN:
                    if (entity instanceof Domain domain) {
                        return domain.isPropagateToEdge();
                    }
            }
        }
        // Default: If the entity doesn't match any of the conditions, consider it as valid.
        return true;
    }

    private void cleanUpUserAdditionalInfo(User user) {
        if (user.getAdditionalInfo() instanceof NullNode) {
            user.setAdditionalInfo(null);
        }
        if (user.getAdditionalInfo() instanceof ObjectNode additionalInfo) {
            if (additionalInfo.isEmpty()) {
                user.setAdditionalInfo(null);
            } else {
                user.setAdditionalInfo(additionalInfo);
            }
        }
        user.setVersion(null);
    }

    private EdgeEventType getEdgeEventTypeForEntityEvent(Object entity) {
        if (entity instanceof AlarmComment) {
            return EdgeEventType.ALARM_COMMENT;
        } else if (entity instanceof DeviceGroupOtaPackage) {
            return EdgeEventType.DEVICE_GROUP_OTA;
        }
        return null;
    }

    private String getBodyMsgForEntityEvent(Object entity) {
        if (entity instanceof AlarmComment || entity instanceof DeviceGroupOtaPackage) {
            return JacksonUtil.toString(entity);
        }
        return null;
    }

    private EdgeEventActionType getActionForEntityEvent(Object entity, boolean isCreated) {
        if (entity instanceof AlarmComment) {
            return isCreated ? EdgeEventActionType.ADDED_COMMENT : EdgeEventActionType.UPDATED_COMMENT;
        }
        return isCreated ? EdgeEventActionType.ADDED : EdgeEventActionType.UPDATED;
    }

}
