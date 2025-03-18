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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.edqs.EdqsSyncRequest;
import org.thingsboard.server.common.data.edqs.ToCoreEdqsRequest;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.StringFilterPredicate;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@DaoSqlTest
public class EdqsControllerTest extends EdqsEntityQueryControllerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void beforeEdqsControllerTest() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testEdqsSync() throws Exception {
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Device device = new Device();
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            ObjectNode additionalInfo = JacksonUtil.newObjectNode();
            additionalInfo.put("gateway", true);
            device.setAdditionalInfo(additionalInfo);
            devices.add(doPost("/api/device", device, Device.class));
            Thread.sleep(1);
        }

        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");

        List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));

        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, null);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, null, Collections.singletonList(getGatewayFilter()));
        findByQueryAndCheck(query, 3);

        // update db
        Device device1 = devices.get(0);
        device1.setAdditionalInfo(JacksonUtil.newObjectNode());
        jdbcTemplate.execute("update device set additional_info = '{}' where id = '" + device1.getId().getId().toString() + "'");

        // do edqs sync
        loginSysAdmin();
        ToCoreEdqsRequest syncRequest = new ToCoreEdqsRequest(new EdqsSyncRequest(), null);
        doPost("/api/edqs/system/request", syncRequest);

        //check sync is finished
        await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> {
            Optional<AttributeKvEntry> attribute = attributesService.find(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, AttributeScope.SERVER_SCOPE, "edqsSyncState").get();
            return attribute.isPresent() && attribute.get().getJsonValue().isPresent() &&
                    attribute.get().getJsonValue().get().contains("\"status\":\"FINISHED\"");
        });

        // check if the count is updated
        loginTenantAdmin();
        findByQueryAndCheck(query, 2);
    }

    private KeyFilter getGatewayFilter() {
        KeyFilter additionalInfoFilter = new KeyFilter();
        additionalInfoFilter.setKey(new EntityKey(EntityKeyType.ENTITY_FIELD, "additionalInfo"));
        additionalInfoFilter.setValueType(EntityKeyValueType.STRING);
        StringFilterPredicate predicate = new StringFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromString("\"gateway\":true"));
        predicate.setOperation(StringFilterPredicate.StringOperation.CONTAINS);
        additionalInfoFilter.setPredicate(predicate);
        return additionalInfoFilter;
    }
}
