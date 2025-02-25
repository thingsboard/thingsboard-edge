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
import org.thingsboard.server.common.data.query.EntityGroupFilter;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.data.EntityGroupData;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.query.SortableEntityData;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.thingsboard.server.edqs.util.RepositoryUtils.getSortValue;

public class EntitiesByGroupQueryProcessor extends AbstractSingleEntityTypeQueryProcessor<EntityGroupFilter> {

    private final String groupType;
    private final UUID groupId;

    public EntitiesByGroupQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (EntityGroupFilter) query.getEntityFilter());
        groupId = UUID.fromString(filter.getEntityGroup());
        groupType = filter.getGroupType().name();
    }

    @Override
    protected void processCustomerGenericRead(UUID customerId, Consumer<EntityData<?>> processor) {
        var customers = repository.getAllCustomers(customerId);
        processAll(ed -> {
            if (checkCustomerHierarchy(customers, ed)) {
                processor.accept(ed);
            }
        });
    }

    @Override
    protected List<SortableEntityData> processCustomerGenericReadWithGroups(UUID customerId, boolean readAttrPermissions, boolean readTsPermissions, List<GroupPermissions> groupPermissions) {
        List<SortableEntityData> result = new ArrayList<>(getProbableResultSize());
        var customers = repository.getAllCustomers(customerId);
        processAll(ed -> {
            CombinedPermissions permissions = getCombinedPermissions(ed.getId(), checkCustomerHierarchy(customers, ed), readAttrPermissions, readTsPermissions, groupPermissions);
            if (permissions.isRead()) {
                SortableEntityData sortData = new SortableEntityData(ed);
                sortData.setSortValue(getSortValue(ed, sortKey));
                sortData.setReadAttrs(permissions.isReadAttrs());
                sortData.setReadTs(permissions.isReadTs());
                result.add(sortData);
            }
        });
        return result;
    }

    @Override
    protected void processGroupsOnly(List<GroupPermissions> groupPermissions, Consumer<EntityData<?>> processor) {
        processAll(processor);
    }

    @Override
    protected void processAll(Consumer<EntityData<?>> processor) {
        EntityGroupData entityGroup = repository.getEntityGroup(groupId);
        if (matches(entityGroup)) {
            for (EntityData<?> ed : entityGroup.getEntities()) {
                processor.accept(ed);
            }
        }
    }

    @Override
    protected boolean matches(EntityData ed) {
        return super.matches(ed) && groupType.equals(ed.getFields().getType());
    }

    @Override
    protected int getProbableResultSize() {
        return 1024;
    }

}
