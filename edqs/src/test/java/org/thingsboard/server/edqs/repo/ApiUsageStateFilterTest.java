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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.query.ApiUsageStateFilter;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.StringFilterPredicate;
import org.thingsboard.server.edqs.util.RepositoryUtils;

import java.util.Arrays;
import java.util.UUID;

public class ApiUsageStateFilterTest extends AbstractEDQTest {

    @Before
    public void setUp() {
        Tenant entity = new Tenant();
        entity.setId(tenantId);
        entity.setTitle("test tenant");
        addOrUpdate(EntityType.TENANT, entity);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testFindCustomerApiUsageState() {
        UUID customerId = UUID.randomUUID();
        createCustomer(customerId, null, "Customer A");

        ApiUsageState apiUsageState = buildApiUsageState(customerId);
        addOrUpdate(EntityType.API_USAGE_STATE, apiUsageState);

        var result = repository.findEntityDataByQuery(tenantId, null, RepositoryUtils.ALL_READ_PERMISSIONS, getEntityDataQuery(new CustomerId(customerId)), false);

        Assert.assertEquals(1, result.getTotalElements());
        var customer = result.getData().get(0);
        Assert.assertEquals("Customer A", customer.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue());
    }

    private ApiUsageState buildApiUsageState(UUID customerId) {
        ApiUsageState apiUsageState = new ApiUsageState();
        apiUsageState.setId(new ApiUsageStateId(UUID.randomUUID()));
        apiUsageState.setTenantId(tenantId);
        apiUsageState.setEntityId(new CustomerId(customerId));
        apiUsageState.setTransportState(ApiUsageStateValue.ENABLED);
        apiUsageState.setReExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setJsExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setTbelExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setDbStorageState(ApiUsageStateValue.ENABLED);
        apiUsageState.setSmsExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setEmailExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setAlarmExecState(ApiUsageStateValue.ENABLED);
        return apiUsageState;
    }

    private static EntityDataQuery getEntityDataQuery(CustomerId customerId) {
        ApiUsageStateFilter filter = new ApiUsageStateFilter();
        filter.setCustomerId(customerId);
        var pageLink = new EntityDataPageLink(20, 0, null, new EntityDataSortOrder(new EntityKey(EntityKeyType.TIME_SERIES, "name"), EntityDataSortOrder.Direction.DESC), false);

        var entityFields = Arrays.asList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"), new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"));
        KeyFilter nameFilter = new KeyFilter();
        nameFilter.setKey(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        var predicate = new StringFilterPredicate();
        predicate.setIgnoreCase(false);
        predicate.setOperation(StringFilterPredicate.StringOperation.CONTAINS);
        predicate.setValue(new FilterPredicateValue<>("Customer A"));
        nameFilter.setPredicate(predicate);
        nameFilter.setValueType(EntityKeyValueType.STRING);

        return new EntityDataQuery(filter, pageLink, entityFields, null, Arrays.asList(nameFilter));
    }

}
