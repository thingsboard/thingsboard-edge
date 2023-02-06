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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SingleEntityFilter.class, name = "singleEntity"),
        @JsonSubTypes.Type(value = EntityGroupFilter.class, name = "entityGroup"),
        @JsonSubTypes.Type(value = EntityListFilter.class, name = "entityList"),
        @JsonSubTypes.Type(value = EntityNameFilter.class, name = "entityName"),
        @JsonSubTypes.Type(value = EntityTypeFilter.class, name = "entityType"),
        @JsonSubTypes.Type(value = EntityGroupListFilter.class, name = "entityGroupList"),
        @JsonSubTypes.Type(value = EntityGroupNameFilter.class, name = "entityGroupName"),
        @JsonSubTypes.Type(value = EntitiesByGroupNameFilter.class, name = "entitiesByGroupName"),
        @JsonSubTypes.Type(value = StateEntityOwnerFilter.class, name = "stateEntityOwner"),
        @JsonSubTypes.Type(value = AssetTypeFilter.class, name = "assetType"),
        @JsonSubTypes.Type(value = DeviceTypeFilter.class, name = "deviceType"),
        @JsonSubTypes.Type(value = EdgeTypeFilter.class, name = "edgeType"),
        @JsonSubTypes.Type(value = EntityViewTypeFilter.class, name = "entityViewType"),
        @JsonSubTypes.Type(value = ApiUsageStateFilter.class, name = "apiUsageState"),
        @JsonSubTypes.Type(value = RelationsQueryFilter.class, name = "relationsQuery"),
        @JsonSubTypes.Type(value = AssetSearchQueryFilter.class, name = "assetSearchQuery"),
        @JsonSubTypes.Type(value = DeviceSearchQueryFilter.class, name = "deviceSearchQuery"),
        @JsonSubTypes.Type(value = EntityViewSearchQueryFilter.class, name = "entityViewSearchQuery"),
        @JsonSubTypes.Type(value = EdgeSearchQueryFilter.class, name = "edgeSearchQuery"),
        @JsonSubTypes.Type(value = SchedulerEventFilter.class, name = "schedulerEvent")})
public interface EntityFilter {

    @JsonIgnore
    EntityFilterType getType();
}
