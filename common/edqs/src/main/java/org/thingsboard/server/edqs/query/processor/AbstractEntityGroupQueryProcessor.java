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
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.edqs.data.EntityGroupData;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.query.SortableEntityData;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractEntityGroupQueryProcessor<T extends EntityFilter> extends AbstractSingleEntityTypeQueryProcessor<T> {

    public AbstractEntityGroupQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query, T filter) {
        super(repo, ctx, query, filter);
    }

    @Override
    protected List<SortableEntityData> processCustomerGenericReadWithGroups(UUID customerId, boolean readAttrPermissions, boolean readTsPermissions, List<GroupPermissions> groupPermissions) {
        var genericReadResults = processCustomerGenericRead(customerId, readAttrPermissions, readTsPermissions);
        Map<UUID, SortableEntityData> mergedResult = new HashMap<>(genericReadResults.size());
        for (SortableEntityData sd : genericReadResults) {
            mergedResult.put(sd.getId(), sd);
        }
        for (GroupPermissions permissions : groupPermissions) {
            SortableEntityData alreadyAdded = mergedResult.get(permissions.groupId);
            if (alreadyAdded != null) {
                alreadyAdded.setReadAttrs(alreadyAdded.isReadAttrs() || permissions.readAttrs);
                alreadyAdded.setReadTs(alreadyAdded.isReadTs() || permissions.readTs);
            } else {
                EntityGroupData egData = repository.getEntityGroup(permissions.groupId);
                if (matches(egData)) {
                    SortableEntityData sortData = toSortData(egData, permissions);
                    mergedResult.put(egData.getId(), sortData);
                }
            }
        }
        return new ArrayList<>(mergedResult.values());
    }

    @Override
    protected CombinedPermissions getCombinedPermissionsInternal(UUID id, boolean read, boolean readAttrs, boolean readTs, List<GroupPermissions> groupPermissions) {
        for (GroupPermissions eg : groupPermissions) {
            if (read && readAttrs && readTs) {
                break;
            }
            if (eg.groupId.equals(id)) {
                read = true;
                readAttrs = readAttrs || eg.readAttrs;
                readTs = readTs || eg.readTs;
            }
        }
        return new CombinedPermissions(read, readAttrs, readTs);
    }

}
