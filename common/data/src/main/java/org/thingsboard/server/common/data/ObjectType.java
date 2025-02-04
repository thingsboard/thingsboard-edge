/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.common.data;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public enum ObjectType {
    TENANT,
    TENANT_PROFILE,
    CUSTOMER,
    ADMIN_SETTINGS,
    QUEUE,
    RPC,
    RULE_CHAIN,
    OTA_PACKAGE,
    RESOURCE,
    ROLE,
    ENTITY_GROUP,
    DEVICE_GROUP_OTA_PACKAGE,
    GROUP_PERMISSION,
    BLOB_ENTITY,
    SCHEDULER_EVENT,
    EVENT,
    RULE_NODE,
    CONVERTER,
    INTEGRATION,
    USER,
    USER_CREDENTIALS,
    USER_AUTH_SETTINGS,
    EDGE,
    WIDGETS_BUNDLE,
    WIDGET_TYPE,
    DASHBOARD,
    DEVICE_PROFILE,
    DEVICE,
    DEVICE_CREDENTIALS,
    ASSET_PROFILE,
    ASSET,
    ENTITY_VIEW,
    ALARM,
    ENTITY_ALARM,
    OAUTH2_CLIENT,
    OAUTH2_DOMAIN,
    OAUTH2_MOBILE,
    USER_SETTINGS,
    NOTIFICATION_TARGET,
    NOTIFICATION_TEMPLATE,
    NOTIFICATION_RULE,
    WHITE_LABELING,
    CUSTOM_TRANSLATION,
    ALARM_COMMENT,
    ALARM_TYPE,
    API_USAGE_STATE,
    QUEUE_STATS,

    AUDIT_LOG,
    RELATION,
    ATTRIBUTE_KV,
    LATEST_TS_KV;

    public static final Set<ObjectType> edqsTenantTypes = EnumSet.of(
            TENANT, TENANT_PROFILE, CUSTOMER, DEVICE_PROFILE, DEVICE, ASSET_PROFILE, ASSET, EDGE, ENTITY_VIEW, USER, DASHBOARD,
            RULE_CHAIN, WIDGET_TYPE, WIDGETS_BUNDLE, CONVERTER, INTEGRATION, SCHEDULER_EVENT, ROLE,
            BLOB_ENTITY, API_USAGE_STATE, QUEUE_STATS
    );
    public static final Set<ObjectType> edqsTypes = new HashSet<>(edqsTenantTypes);
    public static final Set<ObjectType> edqsSystemTypes = EnumSet.of(TENANT, TENANT_PROFILE, USER, DASHBOARD,
            API_USAGE_STATE, ATTRIBUTE_KV, LATEST_TS_KV);

    static {
        edqsTypes.addAll(Arrays.asList(ENTITY_GROUP, RELATION, ATTRIBUTE_KV, LATEST_TS_KV));
    }

    public EntityType toEntityType() {
        return EntityType.valueOf(name());
    }

    public static ObjectType fromEntityType(EntityType entityType) {
        try {
            return ObjectType.valueOf(entityType.name());
        } catch (Exception e) {
            return null;
        }
    }

}
