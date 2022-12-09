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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public final class EdgeUtils {

    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("(\\$\\{\\{)(.*?)(}})");
    private static final String ATTRIBUTE_PLACEHOLDER_PATTERN = "${{%s}}";
    private static final String ATTRIBUTE_REGEXP_PLACEHOLDER_PATTERN = "\\$\\{\\{%s}}";
    private static final int STACK_TRACE_LIMIT = 10;

    private EdgeUtils() {
    }

    public static EdgeEventType getEdgeEventTypeByEntityType(EntityType entityType) {
        switch (entityType) {
            case EDGE:
                return EdgeEventType.EDGE;
            case DEVICE:
                return EdgeEventType.DEVICE;
            case DEVICE_PROFILE:
                return EdgeEventType.DEVICE_PROFILE;
            case ASSET:
                return EdgeEventType.ASSET;
            case ASSET_PROFILE:
                return EdgeEventType.ASSET_PROFILE;
            case ENTITY_VIEW:
                return EdgeEventType.ENTITY_VIEW;
            case DASHBOARD:
                return EdgeEventType.DASHBOARD;
            case USER:
                return EdgeEventType.USER;
            case RULE_CHAIN:
                return EdgeEventType.RULE_CHAIN;
            case ALARM:
                return EdgeEventType.ALARM;
            case TENANT:
                return EdgeEventType.TENANT;
            case CUSTOMER:
                return EdgeEventType.CUSTOMER;
            case WIDGETS_BUNDLE:
                return EdgeEventType.WIDGETS_BUNDLE;
            case WIDGET_TYPE:
                return EdgeEventType.WIDGET_TYPE;
            case OTA_PACKAGE:
                return EdgeEventType.OTA_PACKAGE;
            case QUEUE:
                return EdgeEventType.QUEUE;
            case ENTITY_GROUP:
                return EdgeEventType.ENTITY_GROUP;
            case SCHEDULER_EVENT:
                return EdgeEventType.SCHEDULER_EVENT;
            case ROLE:
                return EdgeEventType.ROLE;
            case GROUP_PERMISSION:
                return EdgeEventType.GROUP_PERMISSION;
            case INTEGRATION:
                return EdgeEventType.INTEGRATION;
            case CONVERTER:
                return EdgeEventType.CONVERTER;
            default:
                log.warn("Unsupported entity type [{}]", entityType);
                return null;
        }
    }

    public static int nextPositiveInt() {
        return ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
    }

    public static EdgeEvent constructEdgeEvent(TenantId tenantId,
                                               EdgeId edgeId,
                                               EdgeEventType type,
                                               EdgeEventActionType action,
                                               EntityId entityId,
                                               JsonNode body) {
        return constructEdgeEvent(tenantId, edgeId, type, action, entityId, body, null);
    }

    public static EdgeEvent constructEdgeEvent(TenantId tenantId,
                                               EdgeId edgeId,
                                               EdgeEventType type,
                                               EdgeEventActionType action,
                                               EntityId entityId,
                                               JsonNode body,
                                               EntityId entityGroupId) {
        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setEdgeId(edgeId);
        edgeEvent.setType(type);
        edgeEvent.setAction(action);
        if (entityId != null) {
            edgeEvent.setEntityId(entityId.getId());
        }
        if (entityGroupId != null) {
            edgeEvent.setEntityGroupId(entityGroupId.getId());
        }
        edgeEvent.setBody(body);
        return edgeEvent;
    }

    public static Set<String> getAttributeKeysFromConfiguration(String integrationConfiguration) {
        Set<String> result = new HashSet<>();
        Matcher m = ATTRIBUTE_PATTERN.matcher(integrationConfiguration);
        while (m.find()) {
            result.add(m.group(2));
        }
        return result;
    }

    public static String formatAttributeKeyToPlaceholderFormat(String attributeKey) {
        return String.format(ATTRIBUTE_PLACEHOLDER_PATTERN, attributeKey);
    }

    public static String formatAttributeKeyToRegexpPlaceholderFormat(String attributeKey) {
        return String.format(ATTRIBUTE_REGEXP_PLACEHOLDER_PATTERN, attributeKey);
    }

    public static String createErrorMsgFromRootCauseAndStackTrace(Throwable t) {
        Throwable rootCause = Throwables.getRootCause(t);
        StringBuilder errorMsg = new StringBuilder(rootCause.getMessage() != null ? rootCause.getMessage() : "");
        if (rootCause.getStackTrace().length > 0) {
            int idx = 0;
            for (StackTraceElement stackTraceElement : rootCause.getStackTrace()) {
                errorMsg.append("\n").append(stackTraceElement.toString());
                idx++;
                if (idx > STACK_TRACE_LIMIT) {
                    break;
                }
            }
        }
        return errorMsg.toString();
    }

    public static boolean isEdgeGroupAll(String groupName) {
        return groupName.startsWith(EntityGroup.GROUP_EDGE_ALL_STARTS_WITH) && groupName.endsWith(EntityGroup.GROUP_EDGE_ALL_ENDS_WITH);
    }
}
