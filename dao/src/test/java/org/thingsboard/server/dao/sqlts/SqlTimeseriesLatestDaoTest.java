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
package org.thingsboard.server.dao.sqlts;

import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.timeseries.TimeseriesLatestDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DaoSqlTest
public class SqlTimeseriesLatestDaoTest extends AbstractServiceTest {

    @Autowired
    private TimeseriesLatestDao timeseriesLatestDao;

    @Test
    public void saveLatestTest() throws Exception {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        var entry = createEntry("key", 1000);
        Long version = timeseriesLatestDao.saveLatest(tenantId, deviceId, entry).get();
        assertNotNull(version);
        assertTrue(version > 0);

        TsKvEntry foundEntry = timeseriesLatestDao.findLatest(tenantId, deviceId, "key").get();
        assertNotNull(foundEntry);
        equalsIgnoreVersion(entry, foundEntry);
        assertEquals(version, foundEntry.getVersion());

        var updatedEntry = createEntry("key", 2000);
        Long updatedVersion = timeseriesLatestDao.saveLatest(tenantId, deviceId, updatedEntry).get();
        assertNotNull(updatedVersion);
        assertTrue(updatedVersion > version);

        foundEntry = timeseriesLatestDao.findLatest(tenantId, deviceId, "key").get();
        assertNotNull(foundEntry);
        equalsIgnoreVersion(updatedEntry, foundEntry);
        assertEquals(updatedVersion, foundEntry.getVersion());

        var oldEntry = createEntry("key", 1);
        Long oldVersion = timeseriesLatestDao.saveLatest(tenantId, deviceId, oldEntry).get();
        assertNull(oldVersion);

        foundEntry = timeseriesLatestDao.findLatest(tenantId, deviceId, "key").get();
        assertNotNull(foundEntry);
        equalsIgnoreVersion(updatedEntry, foundEntry);
        assertEquals(updatedVersion, foundEntry.getVersion());
    }

    @Test
    public void updateWithOldTsTest() throws Exception {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        int n = 50;
        for (int i = 0; i < n; i++) {
            timeseriesLatestDao.saveLatest(tenantId, deviceId, createEntry("key_" + i, System.currentTimeMillis()));
        }

        List<ListenableFuture<Long>> futures = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            long ts = i % 2 == 0 ? System.currentTimeMillis() : 1000;
            futures.add(timeseriesLatestDao.saveLatest(tenantId, deviceId, createEntry("key_" + i, ts)));
        }

        for (int i = 0; i < futures.size(); i++) {
            Long version = futures.get(i).get();
            if (i % 2 == 0) {
                assertNotNull(version);
                assertTrue(version > 0);
            } else {
                assertNull(version);
            }
        }
    }

    private TsKvEntry createEntry(String key, long ts) {
        return new BasicTsKvEntry(ts, new StringDataEntry(key, RandomStringUtils.random(10)));
    }

    private void equalsIgnoreVersion(TsKvEntry expected, TsKvEntry actual) {
        Assert.assertEquals(expected.getKey(), actual.getKey());
        Assert.assertEquals(expected.getValue(), actual.getValue());
        Assert.assertEquals(expected.getTs(), actual.getTs());
    }

}
