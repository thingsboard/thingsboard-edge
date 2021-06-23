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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.gen.edge.v1.RoleProto;
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
public class RoleCloudProcessor extends BaseCloudProcessor {

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserPermissionsService userPermissionsService;

    private Set<Operation> allowedEntityGroupOperations = new HashSet<>(Arrays.asList(Operation.READ,
            Operation.READ_ATTRIBUTES, Operation.READ_TELEMETRY));

    private Set<Operation> allowedGenericOperations = new HashSet<>(Arrays.asList(Operation.READ,
            Operation.READ_ATTRIBUTES, Operation.READ_TELEMETRY, Operation.RPC_CALL,
            Operation.READ_CREDENTIALS, Operation.ADD_TO_GROUP, Operation.REMOVE_FROM_GROUP));

    public ListenableFuture<Void> processRoleMsgFromCloud(TenantId tenantId, RoleProto roleProto) {
        try {
            RoleId roleId = new RoleId(new UUID(roleProto.getIdMSB(), roleProto.getIdLSB()));
            switch (roleProto.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    Role role = roleService.findRoleById(tenantId, roleId);
                    if (role == null) {
                        role = new Role();
                        role.setId(roleId);
                        role.setCreatedTime(Uuids.unixTimestamp(roleId.getId()));
                        TenantId roleTenantId = new TenantId(new UUID(roleProto.getTenantIdMSB(), roleProto.getTenantIdLSB()));
                        role.setTenantId(roleTenantId);
                    }
                    role.setName(roleProto.getName());
                    role.setType(RoleType.valueOf(roleProto.getType()));
                    role.setAdditionalInfo(JacksonUtil.toJsonNode(roleProto.getAdditionalInfo()));
                    role.setPermissions(JacksonUtil.toJsonNode(roleProto.getPermissions()));

                    replaceWriteOperationsToReadIfRequired(role);

                    UUID customerUUID = safeGetUUID(roleProto.getCustomerIdMSB(), roleProto.getCustomerIdLSB());
                    if (customerUUID != null) {
                        role.setCustomerId(new CustomerId(customerUUID));
                    }

                    Role savedRole = roleService.saveRole(tenantId, role);

                    userPermissionsService.onRoleUpdated(savedRole);

                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    Role roleById = roleService.findRoleById(tenantId, roleId);
                    if (roleById != null) {
                        roleService.deleteRole(tenantId, roleId);
                    }
                    break;
                case UNRECOGNIZED:
                    log.error("Unsupported msg type");
                    return Futures.immediateFailedFuture(new RuntimeException("Unsupported msg type " + roleProto.getMsgType()));
            }
        } catch (Exception e) {
            log.error("Can't process roleProto [{}]", roleProto, e);
            return Futures.immediateFailedFuture(new RuntimeException("Can't process roleProto " + roleProto, e));
        }
        return Futures.immediateFuture(null);
    }

    private void replaceWriteOperationsToReadIfRequired(Role role) throws JsonProcessingException {
        if (RoleType.GROUP.equals(role.getType())) {
            CollectionType collectionType = TypeFactory.defaultInstance().constructCollectionType(List.class, Operation.class);
            List<Operation> originOperations = mapper.readValue(role.getPermissions().toString(), collectionType);
            List<Operation> operations;
            if (originOperations.contains(Operation.ALL)) {
                operations = new ArrayList<>(allowedEntityGroupOperations);
            } else {
                operations = originOperations.stream()
                        .filter(allowedEntityGroupOperations::contains)
                        .collect(Collectors.toList());
            }
            role.setPermissions(mapper.valueToTree(operations));
        } else {
            CollectionType operationType = TypeFactory.defaultInstance().constructCollectionType(List.class, Operation.class);
            JavaType resourceType = mapper.getTypeFactory().constructType(Resource.class);
            MapType mapType = TypeFactory.defaultInstance().constructMapType(HashMap.class, resourceType, operationType);
            Map<Resource, List<Operation>> originPermissions = mapper.readValue(role.getPermissions().toString(), mapType);
            for (Map.Entry<Resource, List<Operation>> entry : originPermissions.entrySet()) {
                List<Operation> originOperations = entry.getValue();
                if (Resource.DEVICE.equals(entry.getKey())) {
                    continue;
                }
                if (Resource.ALL.equals(entry.getKey()) && originOperations.contains(Operation.ALL)) {
                    originPermissions.put(Resource.DEVICE, Collections.singletonList(Operation.ALL));
                }
                List<Operation> operations;
                if (originOperations.contains(Operation.ALL)) {
                    operations = new ArrayList<>(allowedGenericOperations);
                } else {
                    operations = originOperations.stream()
                            .filter(allowedGenericOperations::contains)
                            .collect(Collectors.toList());
                }
                originPermissions.put(entry.getKey(), operations);
            }
            role.setPermissions(mapper.valueToTree(originPermissions));
        }
    }


}
