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
    EDGE(EntityType.EDGE),
    TENANT(EntityType.TENANT),
    RULE_CHAIN(EntityType.RULE_CHAIN),
    USER(EntityType.USER),
    WIDGETS_BUNDLE(EntityType.WIDGETS_BUNDLE),
    WIDGET_TYPE(EntityType.WIDGET_TYPE),
    OAUTH2_CONFIGURATION_INFO(),
    OAUTH2_CONFIGURATION_TEMPLATE(),
    TENANT_PROFILE(EntityType.TENANT_PROFILE),
    DEVICE_PROFILE(EntityType.DEVICE_PROFILE),
    ASSET_PROFILE(EntityType.ASSET_PROFILE),
    CONVERTER(EntityType.CONVERTER),
    INTEGRATION(EntityType.INTEGRATION),
    SCHEDULER_EVENT(EntityType.SCHEDULER_EVENT),
    BLOB_ENTITY(EntityType.BLOB_ENTITY),
    CUSTOMER_GROUP(EntityType.ENTITY_GROUP),
    DEVICE_GROUP(EntityType.ENTITY_GROUP),
    ASSET_GROUP(EntityType.ENTITY_GROUP),
    USER_GROUP(EntityType.ENTITY_GROUP),
    ENTITY_VIEW_GROUP(EntityType.ENTITY_GROUP),
    EDGE_GROUP(EntityType.ENTITY_GROUP),
    DASHBOARD_GROUP(EntityType.ENTITY_GROUP),
    ROLE(EntityType.ROLE),
    GROUP_PERMISSION(EntityType.GROUP_PERMISSION),
    WHITE_LABELING(),
    AUDIT_LOG(),
    API_USAGE_STATE(EntityType.API_USAGE_STATE),
    TB_RESOURCE(EntityType.TB_RESOURCE),
    OTA_PACKAGE(EntityType.OTA_PACKAGE),
    QUEUE(EntityType.QUEUE),
    VERSION_CONTROL,
    NOTIFICATION_TARGET(EntityType.NOTIFICATION_TARGET),
    NOTIFICATION_TEMPLATE(EntityType.NOTIFICATION_TEMPLATE),
    NOTIFICATION_REQUEST(EntityType.NOTIFICATION_REQUEST),
    NOTIFICATION_RULE(EntityType.NOTIFICATION_RULE);

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
        groupResourceByGroupType.put(EntityType.EDGE, EDGE_GROUP);
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
        operationsByResource.put(Resource.ASSET_PROFILE, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.OTA_PACKAGE, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.ASSET, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.CUSTOMER, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.DASHBOARD, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.ENTITY_VIEW, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.EDGE, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.TENANT, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.TENANT_PROFILE, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.API_USAGE_STATE, new HashSet<>(Arrays.asList(Operation.ALL, Operation.READ, Operation.READ_TELEMETRY)));
        operationsByResource.put(Resource.RULE_CHAIN, Operation.defaultEntityOperations);
        Set<Operation> userOperations = new HashSet<>(Operation.defaultEntityOperations);
        userOperations.add(Operation.IMPERSONATE);
        operationsByResource.put(Resource.USER, userOperations);
        operationsByResource.put(Resource.WIDGETS_BUNDLE, Operation.crudOperations);
        operationsByResource.put(Resource.WIDGET_TYPE, Operation.crudOperations);
        operationsByResource.put(Resource.TB_RESOURCE, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.CONVERTER, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.INTEGRATION, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.SCHEDULER_EVENT, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.BLOB_ENTITY, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.CUSTOMER_GROUP, Operation.defaultEntityGroupOperations);
        operationsByResource.put(Resource.DEVICE_GROUP, Operation.defaultEntityGroupOperations);
        operationsByResource.put(Resource.ASSET_GROUP, Operation.defaultEntityGroupOperations);
        operationsByResource.put(Resource.USER_GROUP, Operation.defaultEntityGroupOperations);
        operationsByResource.put(Resource.ENTITY_VIEW_GROUP, Operation.defaultEntityGroupOperations);
        operationsByResource.put(Resource.EDGE_GROUP, Operation.defaultEntityGroupOperations);
        operationsByResource.put(Resource.DASHBOARD_GROUP, Operation.defaultEntityGroupOperations);
        operationsByResource.put(Resource.ROLE, Operation.defaultEntityOperations);
        operationsByResource.put(Resource.GROUP_PERMISSION, Operation.crudOperations);
        operationsByResource.put(Resource.WHITE_LABELING, new HashSet<>(Arrays.asList(Operation.ALL, Operation.READ, Operation.WRITE)));
        operationsByResource.put(Resource.AUDIT_LOG, new HashSet<>(Arrays.asList(Operation.ALL, Operation.READ)));
        operationsByResource.put(Resource.QUEUE, new HashSet<>(Arrays.asList(Operation.ALL, Operation.READ)));
        operationsByResource.put(Resource.VERSION_CONTROL, new HashSet<>(Arrays.asList(Operation.ALL, Operation.READ, Operation.WRITE, Operation.DELETE)));

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
                Resource.OAUTH2_CONFIGURATION_TEMPLATE,
                Resource.TB_RESOURCE,
                Resource.QUEUE)));

        resourcesByAuthority.put(Authority.TENANT_ADMIN, new HashSet<>(Arrays.asList(
                Resource.ALL,
                Resource.PROFILE,
                Resource.ALARM,
                Resource.DEVICE,
                Resource.DEVICE_PROFILE,
                Resource.ASSET_PROFILE,
                Resource.API_USAGE_STATE,
                Resource.ASSET,
                Resource.ENTITY_VIEW,
                Resource.EDGE,
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
                Resource.EDGE_GROUP,
                Resource.GROUP_PERMISSION,
                Resource.WHITE_LABELING,
                Resource.AUDIT_LOG,
                Resource.TB_RESOURCE,
                Resource.OTA_PACKAGE,
                Resource.QUEUE,
                Resource.VERSION_CONTROL)));

        resourcesByAuthority.put(Authority.CUSTOMER_USER, new HashSet<>(Arrays.asList(
                Resource.ALL,
                Resource.PROFILE,
                Resource.ALARM,
                Resource.DEVICE,
                Resource.ASSET,
                Resource.ENTITY_VIEW,
                Resource.EDGE,
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
                Resource.EDGE_GROUP,
                Resource.GROUP_PERMISSION,
                Resource.WHITE_LABELING,
                Resource.AUDIT_LOG,
                Resource.DEVICE_PROFILE,
                Resource.ASSET_PROFILE)));

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
