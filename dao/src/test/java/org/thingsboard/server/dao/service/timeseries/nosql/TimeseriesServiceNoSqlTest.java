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
package org.thingsboard.server.dao.service.timeseries.nosql;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Test;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.service.DaoNoSqlTest;
import org.thingsboard.server.dao.service.timeseries.BaseTimeseriesServiceTest;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@DaoNoSqlTest
public class TimeseriesServiceNoSqlTest extends BaseTimeseriesServiceTest {

    @Test
    public void shouldSaveEntryOfEachTypeWithTtl() throws ExecutionException, InterruptedException, TimeoutException {
        long ttlInSec = TimeUnit.SECONDS.toSeconds(3);
        List<TsKvEntry> timeseries = List.of(
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(1), new BooleanDataEntry("test", true)),
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(2), new StringDataEntry("test", "text")),
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(3), new LongDataEntry("test", 15L)),
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(4), new DoubleDataEntry("test", 10.5)),
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(5), new JsonDataEntry("test", "{\"test\":\"testValue\"}")));

        DeviceId deviceId = new DeviceId(Uuids.timeBased());
        tsService.save(tenantId, deviceId, timeseries, ttlInSec);

        List<TsKvEntry> fullList = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("test", 0L,
                TimeUnit.MINUTES.toMillis(6), 1000, 10, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(5, fullList.size());

        // check entries after ttl
        Thread.sleep(TimeUnit.SECONDS.toMillis(ttlInSec + 1));
        List<TsKvEntry> listAfterTtl = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("test", 0L,
                TimeUnit.MINUTES.toMillis(6), 1000, 10, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(0, listAfterTtl.size());
    }

    @Test
    public void testNullValuesOfNoneTargetColumn() throws ExecutionException, InterruptedException, TimeoutException {
        long ts = TimeUnit.MINUTES.toMillis(1);
        long longValue = 10L;
        TsKvEntry longEntry = new BasicTsKvEntry(ts, new LongDataEntry("temp", longValue));
        double doubleValue = 20.6;
        TsKvEntry doubleEntry = new BasicTsKvEntry(ts, new DoubleDataEntry("temp", doubleValue));
        DeviceId deviceId = new DeviceId(Uuids.timeBased());
        tsService.save(tenantId, deviceId, longEntry).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        tsService.save(tenantId, deviceId, doubleEntry).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        List<TsKvEntry> listWithoutAgg = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("temp", 0L,
                ts + 1 , 1000, 3, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(1, listWithoutAgg.size());
        assertTrue(listWithoutAgg.get(0).getLongValue().isPresent());
        assertFalse(listWithoutAgg.get(0).getDoubleValue().isPresent());
        assertThat(listWithoutAgg.get(0).getLongValue().get()).isEqualTo(longValue);

        // long value should not be reset to null, so avg = (doubleValue + longValue)/ 2
        List<TsKvEntry> listWithAgg = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("temp", 0L,
                ts + 1, 200000, 3, Aggregation.AVG))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(1, listWithAgg.size());
        assertTrue(listWithAgg.get(0).getDoubleValue().isPresent());
        double expectedValue = (doubleValue + longValue)/ 2;
        assertThat(listWithAgg.get(0).getDoubleValue().get()).isEqualTo(expectedValue);
    }
}
