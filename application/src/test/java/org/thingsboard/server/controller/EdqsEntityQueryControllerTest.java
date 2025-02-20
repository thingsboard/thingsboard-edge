/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.msg.edqs.EdqsService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.edqs.util.EdqsRocksDb;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@DaoSqlTest
@TestPropertySource(properties = {
        "queue.type=kafka", // uncomment to use Kafka
        "queue.kafka.bootstrap.servers=192.168.0.105:9092",
        "queue.edqs.sync.enabled=true",
        "queue.edqs.api_enabled=true",
        "queue.edqs.mode=local"
})
public class EdqsEntityQueryControllerTest extends EntityQueryControllerTest {

    @Autowired
    private EdqsService edqsService;

    @MockBean // so that we don't do backup for tests
    private EdqsRocksDb edqsRocksDb;

    @Before
    public void before() {
        await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> edqsService.isApiEnabled());
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

}
