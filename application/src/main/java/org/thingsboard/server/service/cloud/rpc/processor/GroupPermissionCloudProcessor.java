// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.EntityGroupRequestMsg;
import org.thingsboard.server.gen.edge.v1.GroupPermissionProto;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;
import org.thingsboard.server.service.security.permission.UserPermissionsService;

import java.util.UUID;

@Component
@Slf4j
public class GroupPermissionCloudProcessor extends BaseEdgeProcessor {

    @Autowired
    private GroupPermissionService groupPermissionService;

    @Autowired
    private UserPermissionsService userPermissionsService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private EntityGroupService entityGroupService;

    public ListenableFuture<Void> processGroupPermissionMsgFromCloud(TenantId tenantId, GroupPermissionProto groupPermissionProto) {
        try {
            GroupPermissionId groupPermissionId = new GroupPermissionId(new UUID(groupPermissionProto.getIdMSB(), groupPermissionProto.getIdLSB()));
            switch (groupPermissionProto.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    GroupPermission groupPermission = JacksonUtil.fromString(groupPermissionProto.getEntity(), GroupPermission.class, true);
                    if (groupPermission == null) {
                        throw new RuntimeException("[{" + tenantId + "}] groupPermissionProto {" + groupPermissionProto + "} cannot be converted to group permission");
                    }
                    if (!isGroupPermissionValid(tenantId, groupPermission)) {
                        return Futures.immediateFuture(null);
                    }
                    GroupPermission saveGroupPermission = groupPermissionService.saveGroupPermission(tenantId, groupPermission);
                    userPermissionsService.onGroupPermissionUpdated(saveGroupPermission);
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    GroupPermission groupPermissionById = groupPermissionService.findGroupPermissionById(tenantId, groupPermissionId);
                    if (groupPermissionById != null) {
                        groupPermissionService.deleteGroupPermission(tenantId, groupPermissionId);
                    }
                    break;
                case UNRECOGNIZED:
                    return handleUnsupportedMsgType(groupPermissionProto.getMsgType());
            }
        } catch (Exception e) {
            if (e instanceof DataValidationException
                    && e.getMessage().contains("Group Permission is referencing to non-existent")) {
                String warnMsg = String.format("Group Permission is referencing to non-existent entity group, role or user group! " +
                        "This permission will be saved with an appropriate entity group on next messages [%s]", groupPermissionProto);
                log.warn(warnMsg, e);
            } else {
                String errMsg = String.format("Can't process groupPermissionProto [%s]", groupPermissionProto);
                log.error(errMsg, e);
                return Futures.immediateFailedFuture(new RuntimeException(errMsg, e));
            }
        }
        return Futures.immediateFuture(null);
    }

    private boolean isGroupPermissionValid(TenantId tenantId, GroupPermission groupPermission) {
        if (groupPermission.getRoleId() != null) {
            Role roleById = roleService.findRoleById(tenantId, groupPermission.getRoleId());
            if (roleById == null) {
                return false;
            }
        }
        if (groupPermission.getUserGroupId() != null) {
            EntityGroup userGroupById = entityGroupService.findEntityGroupById(tenantId, groupPermission.getUserGroupId());
            if (userGroupById == null) {
                return false;
            }
        }
        if (groupPermission.getEntityGroupId() != null) {
            EntityGroup entityGroupById = entityGroupService.findEntityGroupById(tenantId, groupPermission.getEntityGroupId());
            if (entityGroupById == null) {
                return false;
            }
        }
        return true;
    }

    public UplinkMsg processEntityGroupPermissionsRequestMsgToCloud(CloudEvent cloudEvent) {
        EntityId entityGroupId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getType(), cloudEvent.getEntityId());
        String type = cloudEvent.getEntityBody().get("type").asText();
        EntityGroupRequestMsg entityGroupPermissionsRequestMsg = EntityGroupRequestMsg.newBuilder()
                .setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits())
                .setType(type)
                .build();
        UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                .addEntityGroupPermissionsRequestMsg(entityGroupPermissionsRequestMsg);
        return builder.build();
    }

}
