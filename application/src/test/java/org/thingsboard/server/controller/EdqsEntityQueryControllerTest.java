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

import org.assertj.core.api.ThrowingConsumer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.edqs.EdqsState;
import org.thingsboard.server.common.data.edqs.EdqsState.EdqsApiMode;
import org.thingsboard.server.common.data.edqs.ToCoreEdqsRequest;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.msg.edqs.EdqsService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.edqs.util.EdqsRocksDb;
import org.thingsboard.server.queue.discovery.DiscoveryService;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DaoSqlTest
@TestPropertySource(properties = {
//        "queue.type=kafka", // uncomment to use Kafka
//        "queue.kafka.bootstrap.servers=10.7.2.107:9092",
        "queue.edqs.sync.enabled=true",
        "queue.edqs.api.supported=true",
        "queue.edqs.api.auto_enable=true",
        "queue.edqs.mode=local",
        "queue.edqs.readiness_check_interval=500"
})
public class EdqsEntityQueryControllerTest extends EntityQueryControllerTest {

    @Autowired
    private EdqsService edqsService;

    @Autowired
    private DiscoveryService discoveryService;

    @MockBean // so that we don't do backup for tests
    private EdqsRocksDb edqsRocksDb;

    @Before
    public void before() {
        await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> edqsService.getState().isApiEnabled());
    }

    @Override
    protected PageData<EntityData> findByQueryAndCheck(EntityDataQuery query, int expectedResultSize) {
        return await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> findByQuery(query),
                result -> result.getTotalElements() == expectedResultSize);
    }

    @Override
    protected Long countByQueryAndCheck(EntityCountQuery query, long expectedResult) {
        return await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> countByQuery(query),
                result -> result == expectedResult);
    }

    @Test
    public void testEdqsState() throws Exception {
        loginSysAdmin();
        assertThat(getEdqsState().getApiMode()).isEqualTo(EdqsApiMode.AUTO_ENABLED);

        // notifying EDQS is not ready: API should be auto-disabled
        discoveryService.setReady(false);
        verifyState(state -> {
            assertThat(state.getApiMode()).isEqualTo(EdqsApiMode.AUTO_DISABLED);
            assertThat(state.getEdqsReady()).isFalse();
            assertThat(state.isApiEnabled()).isFalse();
        });

        // manually disabling API
        edqsService.processSystemRequest(ToCoreEdqsRequest.builder()
                .apiEnabled(false)
                .build());
        verifyState(state -> {
            assertThat(state.getApiMode()).isEqualTo(EdqsApiMode.DISABLED);
            assertThat(state.getEdqsReady()).isFalse();
            assertThat(state.isApiEnabled()).isFalse();
        });

        // notifying EDQS is ready: API should not be enabled automatically because manually disabled previously
        discoveryService.setReady(true);
        verifyState(state -> {
            assertThat(state.getEdqsReady()).isTrue();
            assertThat(state.getApiMode()).isEqualTo(EdqsApiMode.DISABLED);
            assertThat(state.isApiEnabled()).isFalse();
        });

        // manually enabling API
        edqsService.processSystemRequest(ToCoreEdqsRequest.builder()
                .apiEnabled(true)
                .build());
        verifyState(state -> {
            assertThat(state.getApiMode()).isEqualTo(EdqsApiMode.ENABLED);
            assertThat(state.getEdqsReady()).isTrue();
            assertThat(state.isApiEnabled()).isTrue();
        });

        // notifying EDQS is not ready: API should be auto-disabled
        discoveryService.setReady(false);
        verifyState(state -> {
            assertThat(state.getApiMode()).isEqualTo(EdqsApiMode.AUTO_DISABLED);
            assertThat(state.getEdqsReady()).isFalse();
            assertThat(state.isApiEnabled()).isFalse();
        });

        discoveryService.setReady(true);
    }

    private void verifyState(ThrowingConsumer<EdqsState> assertion) {
        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(getEdqsState()).satisfies(assertion);
        });
    }

    private EdqsState getEdqsState() throws Exception {
        return doGet("/api/edqs/state", EdqsState.class);
    }

}
