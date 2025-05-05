/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityGroupId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Schema
@Data
public class MergedGroupTypePermissionInfo {

    public static final MergedGroupTypePermissionInfo MERGED_GROUP_TYPE_PERMISSION_INFO_EMPTY_GROUPS_HAS_GENERIC_READ_TRUE =
            new MergedGroupTypePermissionInfo(Collections.emptyList(), true);
    public static final MergedGroupTypePermissionInfo MERGED_GROUP_TYPE_PERMISSION_INFO_EMPTY_GROUPS_HAS_GENERIC_READ_FALSE =
            new MergedGroupTypePermissionInfo(Collections.emptyList(), false);

    @Schema(description = "List of Entity Groups in case of group roles are assigned to the user (user group)")
    private final List<EntityGroupId> entityGroupIds; // immutable
    @Schema(description = "Indicates if generic permission assigned to the user group.")
    private final boolean hasGenericRead;

    public MergedGroupTypePermissionInfo(List<EntityGroupId> entityGroupIds, boolean hasGenericRead) {
        this.entityGroupIds = entityGroupIds == null ? null : List.copyOf(entityGroupIds);
        this.hasGenericRead = hasGenericRead;
    }

    public static MergedGroupTypePermissionInfo ofEmptyGroups(boolean hasGenericRead) {
        return hasGenericRead ? MERGED_GROUP_TYPE_PERMISSION_INFO_EMPTY_GROUPS_HAS_GENERIC_READ_TRUE : MERGED_GROUP_TYPE_PERMISSION_INFO_EMPTY_GROUPS_HAS_GENERIC_READ_FALSE;
    }

    public MergedGroupTypePermissionInfo addId(EntityGroupId id) {
        if (this.entityGroupIds == null || this.entityGroupIds.isEmpty()) {
            return new MergedGroupTypePermissionInfo(List.of(id), this.hasGenericRead);
        }
        List<EntityGroupId> mergedList = new ArrayList<>(this.entityGroupIds.size() + 1);
        mergedList.addAll(this.entityGroupIds);
        mergedList.add(id);
        return new MergedGroupTypePermissionInfo(mergedList, this.hasGenericRead);
    }

    public MergedGroupTypePermissionInfo addIds(List<EntityGroupId> ids) {
        if (this.entityGroupIds == null || this.entityGroupIds.isEmpty()) {
            return new MergedGroupTypePermissionInfo(new ArrayList<>(ids), this.hasGenericRead);
        }
        Set<EntityGroupId> result = new LinkedHashSet<>();
        result.addAll(this.entityGroupIds);
        result.addAll(ids);
        return new MergedGroupTypePermissionInfo(new ArrayList<>(result), this.hasGenericRead);
    }

}
