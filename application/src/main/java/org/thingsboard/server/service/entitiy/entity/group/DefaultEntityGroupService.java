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
package org.thingsboard.server.service.entitiy.entity.group;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.security.permission.UserPermissionsService;

import java.util.List;
import java.util.Optional;

@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultEntityGroupService extends AbstractTbEntityService implements TbEntityGroupService {

    private final GroupPermissionService groupPermissionService;
    private final UserPermissionsService userPermissionsService;
    private final EntityGroupService entityGroupService;

    @Override
    public EntityGroup save(TenantId tenantId, EntityId parentEntityId, EntityGroup entityGroup, User user) throws Exception {
        ActionType actionType = entityGroup.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        try {
            EntityGroup savedEntityGroup = checkNotNull(entityGroupService.saveEntityGroup(tenantId, parentEntityId, entityGroup));
            autoCommit(user, savedEntityGroup.getType(), savedEntityGroup.getId());

            notificationEntityService.logEntityAction(tenantId, savedEntityGroup.getId(), savedEntityGroup,
                    actionType, user);

            boolean sendMsgToEdge = actionType.equals(ActionType.UPDATED);
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, savedEntityGroup.getId(),
                    savedEntityGroup, user, actionType, sendMsgToEdge, null);
            return savedEntityGroup;
        } catch (Exception e) {
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, emptyId(EntityType.ENTITY_GROUP),
                    entityGroup, user, actionType, false, e);
            throw e;
        }
    }

    @Override
    public void delete(TenantId tenantId, List<EdgeId> relatedEdgeIds, EntityGroup entityGroup, User user) throws ThingsboardException {
        EntityGroupId entityGroupId = entityGroup.getId();
        try {
            entityGroupService.deleteEntityGroup(tenantId, entityGroupId);
            notificationEntityService.notifyDeleteEntity(tenantId, entityGroupId, entityGroup, null,
                    ActionType.DELETED, relatedEdgeIds, user, entityGroupId.getId().toString());
        } catch (Exception e) {
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, emptyId(EntityType.ENTITY_GROUP),
                    entityGroup, user, ActionType.DELETED, false, e, entityGroupId.getId().toString());
            throw e;
        }
    }

    @Override
    public EntityId makePublic(TenantId tenantId, EntityGroup entityGroup, User user) throws ThingsboardException {
        checkPublicEntityGroupType(entityGroup.getType());

        if (entityGroup.isPublic()) {
            throw new ThingsboardException("Entity group is already public!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }

        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setPublic(true);
        groupPermission.setTenantId(tenantId);

        EntityGroup publicUsers = customerService.findOrCreatePublicUserGroup(tenantId, user.getOwnerId());
        Role publicUserEntityGroupRole = customerService.findOrCreatePublicUserEntityGroupRole(tenantId, user.getOwnerId());

        groupPermission.setRoleId(publicUserEntityGroupRole.getId());
        groupPermission.setUserGroupId(publicUsers.getId());
        groupPermission.setEntityGroupId(entityGroup.getId());
        groupPermission.setEntityGroupType(entityGroup.getType());

        EntityId publicCustomerId = publicUsers.getOwnerId();

        JsonNode additionalInfo = entityGroup.getAdditionalInfo();
        if (additionalInfo == null || additionalInfo instanceof NullNode) {
            additionalInfo = JacksonUtil.newObjectNode();
        }
        ((ObjectNode) additionalInfo).put("isPublic", true);
        ((ObjectNode) additionalInfo).put("publicCustomerId", publicCustomerId.getId().toString());
        entityGroup.setAdditionalInfo(additionalInfo);

        GroupPermission savedGroupPermission = groupPermissionService.saveGroupPermission(tenantId, groupPermission);
        entityGroupService.saveEntityGroup(tenantId, entityGroup.getOwnerId(), entityGroup);
        userPermissionsService.onGroupPermissionUpdated(savedGroupPermission);

        notificationEntityService.logEntityAction(tenantId, entityGroup.getId(), null,
                ActionType.MADE_PUBLIC, user, entityGroup.getId().toString(), entityGroup.getName());
        return publicCustomerId;
    }

    @Override
    public void makePrivate(TenantId tenantId, EntityGroup entityGroup, User user) throws ThingsboardException {
        checkPublicEntityGroupType(entityGroup.getType());

        if (!entityGroup.isPublic()) {
            throw new ThingsboardException("Entity group is not public!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }

        Optional<GroupPermission> groupPermission = groupPermissionService.findPublicGroupPermissionByTenantIdAndEntityGroupId(tenantId, entityGroup.getId());
        if (groupPermission.isPresent()) {
            groupPermissionService.deleteGroupPermission(tenantId, groupPermission.get().getId());
            userPermissionsService.onGroupPermissionDeleted(groupPermission.get());
        }

        JsonNode additionalInfo = entityGroup.getAdditionalInfo();
        if (additionalInfo == null) {
            additionalInfo = JacksonUtil.newObjectNode();
        }
        ((ObjectNode) additionalInfo).put("isPublic", false);
        ((ObjectNode) additionalInfo).put("publicCustomerId", "");
        entityGroup.setAdditionalInfo(additionalInfo);

        entityGroupService.saveEntityGroup(tenantId, entityGroup.getOwnerId(), entityGroup);

        notificationEntityService.logEntityAction(tenantId, entityGroup.getId(), null,
                ActionType.MADE_PRIVATE, user, entityGroup.getId().toString(), entityGroup.getName());
    }

    private EntityType checkPublicEntityGroupType(EntityType groupType) throws ThingsboardException {
        if (groupType == null) {
            throw new ThingsboardException("EntityGroup type is required!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (groupType != EntityType.ASSET && groupType != EntityType.DEVICE
                && groupType != EntityType.ENTITY_VIEW && groupType != EntityType.EDGE && groupType != EntityType.DASHBOARD) {
            throw new ThingsboardException("Invalid entityGroup type '" + groupType + "'! Only entity groups of types 'ASSET', 'DEVICE', 'ENTITY_VIEW' or 'DASHBOARD' can be public.", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return groupType;
    }
}
