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
package org.thingsboard.server.common.data.query;

public enum EntityFilterType {
    SINGLE_ENTITY("singleEntity"),
    ENTITY_GROUP("entityGroup"),
    ENTITY_LIST("entityList"),
    ENTITY_NAME("entityName"),
    ENTITY_TYPE("entityType"),
    ENTITY_GROUP_LIST("entityGroupList"),
    ENTITY_GROUP_NAME("entityGroupName"),
    ENTITIES_BY_GROUP_NAME("entitiesByGroupName"),
    STATE_ENTITY_OWNER("stateEntityOwner"),
    ASSET_TYPE("assetType"),
    DEVICE_TYPE("deviceType"),
    ENTITY_VIEW_TYPE("entityViewType"),
    EDGE_TYPE("edgeType"),
    RELATIONS_QUERY("relationsQuery"),
    ASSET_SEARCH_QUERY("assetSearchQuery"),
    DEVICE_SEARCH_QUERY("deviceSearchQuery"),
    ENTITY_VIEW_SEARCH_QUERY("entityViewSearchQuery"),
    EDGE_SEARCH_QUERY("edgeSearchQuery"),
    API_USAGE_STATE("apiUsageState"),
    SCHEDULER_EVENT("schedulerEvent");

    private final String label;

    EntityFilterType(String label) {
        this.label = label;
    }
}
