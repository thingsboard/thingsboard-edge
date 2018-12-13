/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum Resource {
    ALL(),
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
    WHITE_LABELING();

    private static final Map<EntityType, Resource> groupResourceByGroupType = new HashMap<>();
    private static final Map<EntityType, Resource> resourceByEntityType = new HashMap<>();

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
    }

    public static Resource groupResourceFromGroupType(EntityType groupType) {
        return groupResourceByGroupType.get(groupType);
    }

    public static Resource resourceFromEntityType(EntityType entityType) {
        return resourceByEntityType.get(entityType);
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
