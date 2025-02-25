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

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.query.SortableEntityData;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class SingleEntityQueryProcessor extends AbstractSingleEntityTypeQueryProcessor<SingleEntityFilter> {

    private final EntityType entityType;
    private final UUID entityId;

    public SingleEntityQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (SingleEntityFilter) query.getEntityFilter());
        this.entityType = filter.getSingleEntity().getEntityType();
        this.entityId = filter.getSingleEntity().getId();
    }

    @Override
    protected void processCustomerGenericRead(UUID customerId, Consumer<EntityData<?>> processor) {
        EntityData ed = repository.getEntityMap(entityType).get(entityId);
        if (ed != null && ed.getCustomerId() != null && matches(ed)) {
            if (customerId.equals(ed.getCustomerId()) || repository.getAllCustomers(customerId).contains(ed.getCustomerId())) {
                processor.accept(ed);
            }
        }
    }

    @Override
    protected List<SortableEntityData> processCustomerGenericReadWithGroups(UUID customerId, boolean readAttrPermissions, boolean readTsPermissions, List<GroupPermissions> groupPermissions) {
        EntityData ed = repository.getEntityMap(entityType).get(entityId);
        if (!matches(ed)) {
            return Collections.emptyList();
        } else {
            boolean genericRead = customerId.equals(ed.getCustomerId()) || repository.getAllCustomers(customerId).contains(ed.getCustomerId());
            CombinedPermissions permissions = getCombinedPermissions(ed.getId(), genericRead, readAttrPermissions, readTsPermissions, groupPermissions);
            if (permissions.isRead()) {
                SortableEntityData sortData = toSortData(ed, permissions);
                return Collections.singletonList(sortData);
            } else {
                return Collections.emptyList();
            }
        }
    }

    @Override
    protected void processGroupsOnly(List<GroupPermissions> groupPermissions, Consumer<EntityData<?>> processor) {
        processAll(processor);
    }

    @Override
    protected void processAll(Consumer<EntityData<?>> processor) {
        EntityData ed = repository.getEntityMap(entityType).get(entityId);
        if (matches(ed)) {
            processor.accept(ed);
        }
    }

    @Override
    protected int getProbableResultSize() {
        return 1;
    }

}
