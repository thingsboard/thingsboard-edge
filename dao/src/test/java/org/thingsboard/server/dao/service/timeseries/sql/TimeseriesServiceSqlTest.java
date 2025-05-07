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
package org.thingsboard.server.dao.service.timeseries.sql;

import org.junit.Test;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.service.timeseries.BaseTimeseriesServiceTest;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DaoSqlTest
public class TimeseriesServiceSqlTest extends BaseTimeseriesServiceTest {

    @Test
    public void testRemoveLatestAndNoValuePresentInDB() throws ExecutionException, InterruptedException, TimeoutException {
        TsKvEntry tsKvEntry = toTsEntry(TS, stringKvEntry);
        tsService.save(tenantId, deviceId, tsKvEntry).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        Optional<TsKvEntry> tsKvEntryOpt = tsService.findLatest(tenantId, deviceId, STRING_KEY).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        assertThat(tsKvEntryOpt).isPresent();
        equalsIgnoreVersion(tsKvEntry, tsKvEntryOpt.get());
        assertThat(tsKvEntryOpt.get().getVersion()).isNotNull();

        tsService.removeLatest(tenantId, deviceId, List.of(STRING_KEY));

        await().alias("Wait until ts last is removed from the cache").atMost(MAX_TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Optional<TsKvEntry> tsKvEntryAfterRemoval = tsService.findLatest(tenantId, deviceId, STRING_KEY).get(MAX_TIMEOUT, TimeUnit.SECONDS);
                    assertThat(tsKvEntryAfterRemoval).isNotPresent();
                });
    }

}
