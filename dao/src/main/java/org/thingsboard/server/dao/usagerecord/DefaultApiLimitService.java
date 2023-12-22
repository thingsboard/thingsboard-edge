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
package org.thingsboard.server.dao.usagerecord;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.query.BooleanFilterPredicate;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class DefaultApiLimitService implements ApiLimitService {

    private final EntityService entityService;
    private final TbTenantProfileCache tenantProfileCache;

    private static final KeyFilter edgeTemplateExcludeFilter;

    static {
        edgeTemplateExcludeFilter = new KeyFilter();
        edgeTemplateExcludeFilter.setKey(new EntityKey(EntityKeyType.ENTITY_FIELD, "edgeTemplate"));
        edgeTemplateExcludeFilter.setValueType(EntityKeyValueType.BOOLEAN);
        BooleanFilterPredicate predicate = new BooleanFilterPredicate();
        predicate.setOperation(BooleanFilterPredicate.BooleanOperation.EQUAL);
        predicate.setValue(new FilterPredicateValue<>(false));
        edgeTemplateExcludeFilter.setPredicate(predicate);
    }

    @Override
    public boolean checkEntitiesLimit(TenantId tenantId, EntityType entityType) {
        long limit = getLimit(tenantId, profileConfiguration -> profileConfiguration.getEntitiesLimit(entityType));
        if (limit <= 0) {
            return true;
        }

        EntityTypeFilter filter = new EntityTypeFilter();
        filter.setEntityType(entityType);
        EntityCountQuery query;
        if (EntityType.INTEGRATION.equals(entityType) || EntityType.CONVERTER.equals(entityType)) {
            query = new EntityCountQuery(filter, List.of(edgeTemplateExcludeFilter));
        } else {
            query = new EntityCountQuery(filter);
        }
        long currentCount = entityService.countEntitiesByQuery(tenantId, new CustomerId(EntityId.NULL_UUID),
                new MergedUserPermissions(Map.of(Resource.ALL, Set.of(Operation.ALL)), Collections.emptyMap()),
                query);
        return currentCount < limit;
    }

    @Override
    public long getLimit(TenantId tenantId, Function<DefaultTenantProfileConfiguration, Number> extractor) {
        if (tenantId == null || tenantId.isSysTenantId()) {
            return 0L;
        }
        TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
        if (tenantProfile == null) {
            throw new IllegalArgumentException("Tenant profile not found for tenant " + tenantId);
        }
        Number value = extractor.apply(tenantProfile.getDefaultProfileConfiguration());
        if (value == null) {
            return 0L;
        }
        return Math.max(0, value.longValue());
    }

}
