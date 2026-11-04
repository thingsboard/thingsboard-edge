// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.cloud.rpc.processor;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.service.validator.RoleDataValidator;
import org.thingsboard.server.gen.edge.v1.RoleProto;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;
import org.thingsboard.server.service.security.permission.UserPermissionsService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RoleCloudProcessor extends BaseEdgeProcessor {

    @Autowired
    private RoleDataValidator roleValidator;

    @Autowired
    private UserPermissionsService userPermissionsService;

    private final Set<Operation> partialUpdateOrReadOperations = new HashSet<>(Arrays.asList(Operation.RPC_CALL,
            Operation.READ, Operation.READ_ATTRIBUTES, Operation.READ_CREDENTIALS, Operation.READ_TELEMETRY,
            Operation.WRITE_ATTRIBUTES, Operation.WRITE_TELEMETRY, Operation.WRITE_CREDENTIALS));

    private final Set<Operation> fullEntityUpdateOperations = new HashSet<>(Arrays.asList(
            Operation.CHANGE_OWNER, Operation.CREATE,
            Operation.READ, Operation.READ_ATTRIBUTES, Operation.READ_TELEMETRY,
            Operation.WRITE, Operation.WRITE_ATTRIBUTES, Operation.WRITE_TELEMETRY,
            Operation.READ_CALCULATED_FIELD, Operation.WRITE_CALCULATED_FIELD));

    private final Set<Operation> edgeOperations = new HashSet<>(Arrays.asList(
            Operation.READ, Operation.READ_ATTRIBUTES, Operation.READ_TELEMETRY,
            Operation.WRITE));

    private final Set<Operation> entityGroupOperations = new HashSet<>(Arrays.asList(
            Operation.ADD_TO_GROUP, Operation.REMOVE_FROM_GROUP,
            Operation.READ, Operation.READ_ATTRIBUTES, Operation.READ_TELEMETRY,
            Operation.WRITE_ATTRIBUTES, Operation.WRITE_CREDENTIALS));

    public ListenableFuture<Void> processRoleMsgFromCloud(TenantId tenantId, RoleProto roleProto) {
        try {
            RoleId roleId = new RoleId(new UUID(roleProto.getIdMSB(), roleProto.getIdLSB()));
            switch (roleProto.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    Role role = JacksonUtil.fromString(roleProto.getEntity(), Role.class, true);
                    if (role == null) {
                        throw new RuntimeException("[{" + tenantId + "}] roleProto {" + roleProto + "} cannot be converted to rolo");
                    }
                    Role roleById = edgeCtx.getRoleService().findRoleById(tenantId, roleId);
                    boolean created = false;
                    if (roleById == null) {
                        created = true;
                        role.setId(null);
                    }
                    role = replaceWriteOperationsToReadIfRequired(role);
                    roleValidator.validate(role, Role::getTenantId);

                    if (created) {
                        role.setId(roleId);
                    }

                    Role savedRole = edgeCtx.getRoleService().saveRole(tenantId, role, false);

                    userPermissionsService.onRoleUpdated(savedRole);

                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    Role roleToDelete = edgeCtx.getRoleService().findRoleById(tenantId, roleId);
                    if (roleToDelete != null) {
                        edgeCtx.getRoleService().deleteRole(tenantId, roleId);
                    }
                    break;
                case UNRECOGNIZED:
                    return handleUnsupportedMsgType(roleProto.getMsgType());
            }
        } catch (Exception e) {
            String errMsg = String.format("Can't process roleProto [%s]", roleProto);
            log.error(errMsg, e);
            return Futures.immediateFailedFuture(new RuntimeException(errMsg, e));
        }
        return Futures.immediateFuture(null);
    }

    Role replaceWriteOperationsToReadIfRequired(Role role) {
        if (RoleType.GROUP.equals(role.getType())) {
            return role;
        } else {
            CollectionType operationType = TypeFactory.defaultInstance().constructCollectionType(List.class, Operation.class);
            JavaType resourceType = JacksonUtil.OBJECT_MAPPER.getTypeFactory().constructType(Resource.class);
            MapType mapType = TypeFactory.defaultInstance().constructMapType(HashMap.class, resourceType, operationType);
            Map<Resource, List<Operation>> originPermissions = JacksonUtil.fromString(JacksonUtil.toString(role.getPermissions()), mapType);
            if (originPermissions == null) {
                return role;
            }
            Map<Resource, List<Operation>> newPermissions = new HashMap<>();
            for (Map.Entry<Resource, List<Operation>> entry : originPermissions.entrySet()) {
                List<Operation> originOperations = entry.getValue();
                List<Operation> newOperations;
                switch (entry.getKey()) {
                    case PROFILE:
                    case DEVICE:
                    case ASSET:
                    case ENTITY_VIEW:
                    case DASHBOARD:
                    case ALARM:
                    case SCHEDULER_EVENT:
                    case REPORT_TEMPLATE:
                    case REPORT:
                    case NOTIFICATION:
                    case USER:
                    case AI_MODEL:
                    case API_KEY:
                    case AI:
                        newOperations = entry.getValue();
                        break;
                    case DEVICE_GROUP:
                    case ASSET_GROUP:
                    case ENTITY_VIEW_GROUP:
                    case DASHBOARD_GROUP:
                    case USER_GROUP:
                        newOperations = filterOperations(originOperations, entityGroupOperations);
                        break;
                    case VERSION_CONTROL:
                        newOperations = new ArrayList<>();
                        break;
                    case DEVICE_PROFILE:
                    case ASSET_PROFILE:
                    case TB_RESOURCE:
                    case RULE_CHAIN:
                        newOperations = filterOperations(originOperations, fullEntityUpdateOperations);
                        break;
                    case EDGE:
                        newOperations = filterOperations(originOperations, edgeOperations);
                        break;
                    case ALL:
                        if (originOperations.contains(Operation.ALL)) {
                            newPermissions.put(Resource.PROFILE, Collections.singletonList(Operation.ALL));
                            newPermissions.put(Resource.VERSION_CONTROL, new ArrayList<>());
                            newPermissions.put(Resource.DEVICE, Collections.singletonList(Operation.ALL));
                            newPermissions.put(Resource.ASSET, Collections.singletonList(Operation.ALL));
                            newPermissions.put(Resource.ENTITY_VIEW, Collections.singletonList(Operation.ALL));
                            newPermissions.put(Resource.DASHBOARD, Collections.singletonList(Operation.ALL));
                            newPermissions.put(Resource.ALARM, Collections.singletonList(Operation.ALL));
                            newPermissions.put(Resource.SCHEDULER_EVENT, Collections.singletonList(Operation.ALL));
                            newPermissions.put(Resource.REPORT_TEMPLATE, Collections.singletonList(Operation.ALL));
                            newPermissions.put(Resource.REPORT, Collections.singletonList(Operation.ALL));
                            newPermissions.put(Resource.NOTIFICATION, Collections.singletonList(Operation.ALL));
                            newPermissions.put(Resource.USER, Collections.singletonList(Operation.ALL));
                            newPermissions.put(Resource.AI_MODEL, Collections.singletonList(Operation.ALL));
                            newPermissions.put(Resource.API_KEY, Collections.singletonList(Operation.ALL));
                            newPermissions.put(Resource.AI, Collections.singletonList(Operation.ALL));
                            newPermissions.put(Resource.DEVICE_GROUP, new ArrayList<>(entityGroupOperations));
                            newPermissions.put(Resource.ASSET_GROUP, new ArrayList<>(entityGroupOperations));
                            newPermissions.put(Resource.ENTITY_VIEW_GROUP, new ArrayList<>(entityGroupOperations));
                            newPermissions.put(Resource.DASHBOARD_GROUP, new ArrayList<>(entityGroupOperations));
                            newPermissions.put(Resource.USER_GROUP, new ArrayList<>(entityGroupOperations));
                            newPermissions.put(Resource.DEVICE_PROFILE, new ArrayList<>(fullEntityUpdateOperations));
                            newPermissions.put(Resource.ASSET_PROFILE, new ArrayList<>(fullEntityUpdateOperations));
                            newPermissions.put(Resource.TB_RESOURCE, new ArrayList<>(fullEntityUpdateOperations));
                            newPermissions.put(Resource.RULE_CHAIN, new ArrayList<>(fullEntityUpdateOperations));
                            newPermissions.put(Resource.EDGE, new ArrayList<>(edgeOperations));
                            newOperations = new ArrayList<>(partialUpdateOrReadOperations);
                        } else {
                            newOperations = originOperations.stream()
                                    .filter(partialUpdateOrReadOperations::contains)
                                    .collect(Collectors.toList());
                        }
                        break;
                    default:
                        newOperations = filterOperations(originOperations, partialUpdateOrReadOperations);
                        break;
                }
                newPermissions.put(entry.getKey(), newOperations);
            }
            role.setPermissions(JacksonUtil.valueToTree(newPermissions));
        }
        return role;
    }

    private List<Operation> filterOperations(List<Operation> originOperations, Set<Operation> edgeSupportedOperations) {
        if (originOperations.contains(Operation.ALL)) {
            return new ArrayList<>(edgeSupportedOperations);
        } else {
            return originOperations.stream()
                    .filter(edgeSupportedOperations::contains)
                    .collect(Collectors.toList());
        }
    }

}