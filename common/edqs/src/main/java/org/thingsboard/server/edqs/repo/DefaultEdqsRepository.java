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
package org.thingsboard.server.edqs.repo;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.EdqsEvent;
import org.thingsboard.server.common.data.edqs.EdqsEventType;
import org.thingsboard.server.common.data.edqs.query.QueryResult;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.edqs.stats.EdqsStatsService;
import org.thingsboard.server.queue.edqs.EdqsComponent;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

@EdqsComponent
@AllArgsConstructor
@Service
@Slf4j
public class DefaultEdqsRepository implements EdqsRepository {

    private final static ConcurrentMap<TenantId, TenantRepo> repos = new ConcurrentHashMap<>();
    private final Optional<EdqsStatsService> statsService;

    public TenantRepo get(TenantId tenantId) {
        return repos.computeIfAbsent(tenantId, id -> new TenantRepo(id, statsService));
    }

    @Override
    public void processEvent(EdqsEvent event) {
        if (event.getEventType() == EdqsEventType.DELETED && event.getObjectType() == ObjectType.TENANT) {
            log.info("Tenant {} deleted", event.getTenantId());
            repos.remove(event.getTenantId());
        } else {
            get(event.getTenantId()).processEvent(event);
        }
    }

    @Override
    public long countEntitiesByQuery(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityCountQuery query, boolean ignorePermissionCheck) {
        long startNs = System.nanoTime();
        long result = get(tenantId).countEntitiesByQuery(customerId, userPermissions, query, ignorePermissionCheck);
        double timingMs = (double) (System.nanoTime() - startNs) / 1000_000;
        log.info("countEntitiesByQuery done in {} ms", timingMs);
        return result;
    }

    @Override
    public PageData<QueryResult> findEntityDataByQuery(TenantId tenantId, CustomerId customerId,
                                                       MergedUserPermissions userPermissions, EntityDataQuery query, boolean ignorePermissionCheck) {
        long startNs = System.nanoTime();
        var result = get(tenantId).findEntityDataByQuery(customerId, userPermissions, query, ignorePermissionCheck);
        double timingMs = (double) (System.nanoTime() - startNs) / 1000_000;
        log.info("findEntityDataByQuery done in {} ms", timingMs);
        return result;
    }

    @Override
    public void clearIf(Predicate<TenantId> predicate) {
        repos.keySet().removeIf(predicate);
    }

    @Override
    public void clear() {
        repos.clear();
    }

}
