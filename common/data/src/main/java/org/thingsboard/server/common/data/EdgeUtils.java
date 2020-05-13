/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import org.thingsboard.server.common.data.id.EntityGroupId;

import java.util.Set;

public final class EdgeUtils {

    private EdgeUtils() {
    }

    public static boolean isAssignedToEdgeGroup(Set<ShortEntityGroupInfo> assignedEdgeGroups, EntityGroupId edgeEntityGroupId) {
        return assignedEdgeGroups != null && assignedEdgeGroups.contains(new ShortEntityGroupInfo(edgeEntityGroupId, null));
    }

    public static ShortEntityGroupInfo getAssignedEdgeGroupInfo(Set<ShortEntityGroupInfo> assignedEdgeGroups, EntityGroupId edgeEntityGroupId) {
        if (assignedEdgeGroups != null) {
            for (ShortEntityGroupInfo edgeInfo : assignedEdgeGroups) {
                if (edgeInfo.getEntityGroupId().equals(edgeEntityGroupId)) {
                    return edgeInfo;
                }
            }
        }
        return null;
    }

    public static boolean addAssignedEdgeGroup(Set<ShortEntityGroupInfo> assignedEdgeGroups, ShortEntityGroupInfo entityGroup) {
        if (assignedEdgeGroups != null && assignedEdgeGroups.contains(entityGroup)) {
            return false;
        } else {
            if (assignedEdgeGroups != null) {
                assignedEdgeGroups.add(entityGroup);
                return true;
            } else {
                return false;
            }
        }
    }

    public static boolean updateAssignedEdgeGroup(Set<ShortEntityGroupInfo> assignedEdgeGroups, ShortEntityGroupInfo entityGroup) {
        if (assignedEdgeGroups != null && assignedEdgeGroups.contains(entityGroup)) {
            assignedEdgeGroups.remove(entityGroup);
            assignedEdgeGroups.add(entityGroup);
            return true;
        } else {
            return false;
        }
    }

    public static boolean removeAssignedEdgeGroup(Set<ShortEntityGroupInfo> assignedEdgeGroups, ShortEntityGroupInfo entityGroup) {
        if (assignedEdgeGroups != null && assignedEdgeGroups.contains(entityGroup)) {
            assignedEdgeGroups.remove(entityGroup);
            return true;
        } else {
            return false;
        }
    }
}
