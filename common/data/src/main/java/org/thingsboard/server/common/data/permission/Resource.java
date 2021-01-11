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
package org.thingsboard.server.common.data.permission;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.security.Authority;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public enum Resource {
    ALL(),
    PROFILE(),
    ADMIN_SETTINGS(),
    ALARM(EntityType.ALARM),
    DEVICE(EntityType.DEVICE),
    ASSET(EntityType.ASSET),
    CUSTOMER(EntityType.CUSTOMER),
    DASHBOARD(EntityType.DASHBOARD),
    ENTITY_VIEW(EntityType.ENTITY_VIEW),
    TENANT(EntityType.TENANT),
    RULE_CHAIN(EntityType.RULE_CHAIN),
    USER(EntityType.USER),
    WIDGETS_BUNDLE(EntityType.WIDGETS_BUNDLE),
    WIDGET_TYPE(EntityType.WIDGET_TYPE),
    OAUTH2_CONFIGURATION_INFO(),
    OAUTH2_CONFIGURATION_TEMPLATE(),
    TENANT_PROFILE(EntityType.TENANT_PROFILE),
    DEVICE_PROFILE(EntityType.DEVICE_PROFILE),
    CONVERTER(EntityType.CONVERTER),
    INTEGRATION(EntityType.INTEGRATION),
    SCHEDULER_EVENT(EntityType.SCHEDULER_EVENT),
    BLOB_ENTITY(EntityType.BLOB_ENTITY),
    CUSTOMER_GROUP(EntityType.ENTITY_GROUP),
    DEVICE_GROUP(EntityType.ENTITY_GROUP),
    ASSET_GROUP(EntityType.ENTITY_GROUP),
    USER_GROUP(EntityType.ENTITY_GROUP),
    ENTITY_VIEW_GROUP(EntityType.ENTITY_GROUP),
    DASHBOARD_GROUP(EntityType.ENTITY_GROUP),
    ROLE(EntityType.ROLE),
    GROUP_PERMISSION(EntityType.GROUP_PERMISSION),
    WHITE_LABELING(),
    AUDIT_LOG(),
    API_USAGE_STATE(EntityType.API_USAGE_STATE);

    private static final Map<EntityType, Resource> groupResourceByGroupType = new HashMap<>();
    private static final Map<EntityType, Resource> resourceByEntityType = new HashMap<>();
    public static final Map<Resource, Set<Operation>> operationsByResource = new HashMap<>();
    public static final Map<Authority, Set<Resource>> resourcesByAuthority = new HashMap<>();

    static {
        groupResourceByGroupType.put(EntityType.CUSTOMER, CUSTOMER_GROUP);
        groupResourceByGroupType.put(EntityType.DEVICE, DEVICE_GROUP);
        groupResourceByGroupType.put(EntityType.ASSET, ASSET_GROUP);
        groupResourceByGroupType.put(EntityType.USER, USER_GROUP);
        groupResourceByGroupType.put(EntityType.ENTITY_VIEW, ENTITY_VIEW_GROUP);
        groupResourceByGroupType.put(EntityType.DASHBOARD, DASHBOARD_GROUP);

        for (EntityType entityType : EntityType.values()) {
            if (entityType.equals(EntityType.ENTITY_GROUP)) {
                continue;
            }
            for (Resource resource : Resource.values()) {
                if (resource.getEntityType().isPresent() && resource.getEntityType().get().equals(entityType)) {
                    resourceByEntityType.put(entityType, resource);
                }
            }
        }
        operationsByResource.put(Resource.ALL, new HashSet<>(Arrays.asList(Operation.values())));
        operationsByResource.put(Resource.PROFILE, new HashSet<>(Arrays.asList(Operation.ALL, Operation.WRITE)));
        operationsByResource.put(Resource.ADMIN_SETTINGS, new HashSet<>(Arrays.asList(Operation.ALL, Operation.READ, Operation.WRITE)));
        operationsByResource.put(Resource.OAUTH2_CONFIGURATION_INFO, Operation.crudOperations);
        operationsByResource.put(Resource.OAUTH2_CONFIGURATION_TEMPLATE, Operation.crudOperations);
        operationsByResource.put(Resource.ALARM, new HashSet<>(Arrays.asList(Operation.ALL, Operation.READ, Operation.WRITE, Operation.CREATE)));
        operationsByResource.put(Resource.DEVICE, new HashSet<>(Arrays.asList(Operation.ALL, Operation.READ, Operation.WRITE,
                Operation.CREATE, Operation.DELETE, Operation.RPC_CALL, Operation.READ_CREDENTIALS, Operation.WRITE_CREDENTIALS,
                Operation.READ_ATTRIBUTES, Operation.WRITE_ATTRIBUTES, Operation.READ_TELEMETRY, Operation.WRITE_TELEMETRY,
                Operation.CLAIM_DEVICES, Operation.CHANGE_OWNER, Operation.ASSIGN_TO_TENANT)));
        operationsByResource.put(Resource.DEVICE_PROFILE, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.ASSET, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.CUSTOMER, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.DASHBOARD, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.ENTITY_VIEW, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.TENANT, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.TENANT_PROFILE, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.API_USAGE_STATE, new HashSet<>(Arrays.asList(Operation.ALL, Operation.READ, Operation.READ_TELEMETRY)));
        operationsByResource.put(Resource.RULE_CHAIN, Operation.defaultEntityOperations);
        Set<Operation> userOperations = new HashSet<>(Operation.defaultEntityOperations);
        userOperations.add(Operation.IMPERSONATE);
        operationsByResource.put(Resource.USER, userOperations);
        operationsByResource.put(Resource.WIDGETS_BUNDLE, Operation.crudOperations);
        operationsByResource.put(Resource.WIDGET_TYPE, Operation.crudOperations);
        operationsByResource.put(Resource.CONVERTER, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.INTEGRATION, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.SCHEDULER_EVENT, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.BLOB_ENTITY, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.CUSTOMER_GROUP, Operation.defaultEntityGroupOperations);
        operationsByResource.put(Resource.DEVICE_GROUP, Operation.defaultEntityGroupOperations);
        operationsByResource.put(Resource.ASSET_GROUP, Operation.defaultEntityGroupOperations);
        operationsByResource.put(Resource.USER_GROUP, Operation.defaultEntityGroupOperations);
        operationsByResource.put(Resource.ENTITY_VIEW_GROUP, Operation.defaultEntityGroupOperations);
        operationsByResource.put(Resource.DASHBOARD_GROUP, Operation.defaultEntityGroupOperations);
        operationsByResource.put(Resource.ROLE, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.GROUP_PERMISSION, Operation.crudOperations);
        operationsByResource.put(Resource.WHITE_LABELING, new HashSet<>(Arrays.asList(Operation.ALL, Operation.READ, Operation.WRITE)));
        operationsByResource.put(Resource.AUDIT_LOG, new HashSet<>(Arrays.asList(Operation.ALL, Operation.READ)));

        resourcesByAuthority.put(Authority.SYS_ADMIN, new HashSet<>(Arrays.asList(
                Resource.ALL,
                Resource.PROFILE,
                Resource.ADMIN_SETTINGS,
                Resource.DASHBOARD,
                Resource.ALARM,
                Resource.TENANT,
                Resource.TENANT_PROFILE,
                Resource.USER,
                Resource.WIDGETS_BUNDLE,
                Resource.WIDGET_TYPE,
                Resource.ROLE,
                Resource.WHITE_LABELING,
                Resource.OAUTH2_CONFIGURATION_INFO,
                Resource.OAUTH2_CONFIGURATION_TEMPLATE)));

        resourcesByAuthority.put(Authority.TENANT_ADMIN, new HashSet<>(Arrays.asList(
                Resource.ALL,
                Resource.PROFILE,
                Resource.ALARM,
                Resource.DEVICE,
                Resource.DEVICE_PROFILE,
                Resource.API_USAGE_STATE,
                Resource.ASSET,
                Resource.ENTITY_VIEW,
                Resource.CUSTOMER,
                Resource.DASHBOARD,
                Resource.TENANT,
                Resource.USER,
                Resource.WIDGETS_BUNDLE,
                Resource.WIDGET_TYPE,
                Resource.RULE_CHAIN,
                Resource.ROLE,
                Resource.CONVERTER,
                Resource.INTEGRATION,
                Resource.SCHEDULER_EVENT,
                Resource.BLOB_ENTITY,
                Resource.CUSTOMER_GROUP,
                Resource.USER_GROUP,
                Resource.DEVICE_GROUP,
                Resource.ASSET_GROUP,
                Resource.DASHBOARD_GROUP,
                Resource.ENTITY_VIEW_GROUP,
                Resource.GROUP_PERMISSION,
                Resource.WHITE_LABELING,
                Resource.AUDIT_LOG)));

        resourcesByAuthority.put(Authority.CUSTOMER_USER, new HashSet<>(Arrays.asList(
                Resource.ALL,
                Resource.PROFILE,
                Resource.ALARM,
                Resource.DEVICE,
                Resource.ASSET,
                Resource.ENTITY_VIEW,
                Resource.CUSTOMER,
                Resource.DASHBOARD,
                Resource.USER,
                Resource.WIDGETS_BUNDLE,
                Resource.WIDGET_TYPE,
                Resource.ROLE,
                Resource.SCHEDULER_EVENT,
                Resource.BLOB_ENTITY,
                Resource.CUSTOMER_GROUP,
                Resource.USER_GROUP,
                Resource.DEVICE_GROUP,
                Resource.ASSET_GROUP,
                Resource.DASHBOARD_GROUP,
                Resource.ENTITY_VIEW_GROUP,
                Resource.GROUP_PERMISSION,
                Resource.WHITE_LABELING,
                Resource.AUDIT_LOG)));

    }

    public static Resource groupResourceFromGroupType(EntityType groupType) {
        return groupResourceByGroupType.get(groupType);
    }

    public static Resource resourceFromEntityType(EntityType entityType) {
        return resourceByEntityType.get(entityType);
    }

    public static Set<Operation> operationsForResource(Resource resource) {
        return operationsByResource.get(resource);
    }

    private final EntityType entityType;

    Resource() {
        this.entityType = null;
    }

    Resource(EntityType entityType) {
        this.entityType = entityType;
    }

    public Optional<EntityType> getEntityType() {
        return Optional.ofNullable(entityType);
    }
}
