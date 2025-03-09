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
package org.thingsboard.server.dao.service.attributes.sql;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.cache.TbCacheValueWrapper;
import org.thingsboard.server.cache.VersionedTbCache;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.dao.attributes.AttributeCacheKey;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@DaoSqlTest
public class AttributeCacheServiceSqlTest extends AbstractServiceTest {

    private static final String TEST_KEY = "key";
    private static final String TEST_VALUE = "value";
    private static final DeviceId DEVICE_ID = new DeviceId(UUID.randomUUID());

    @Autowired
    VersionedTbCache<AttributeCacheKey, AttributeKvEntry> cache;

    @Test
    public void testPutAndGet() {
        AttributeCacheKey testKey = new AttributeCacheKey(AttributeScope.CLIENT_SCOPE, DEVICE_ID, TEST_KEY);
        AttributeKvEntry testValue = new BaseAttributeKvEntry(new StringDataEntry(TEST_KEY, TEST_VALUE), 1, 1L);
        cache.put(testKey, testValue);

        TbCacheValueWrapper<AttributeKvEntry> wrapper = cache.get(testKey);
        assertNotNull(wrapper);

        assertEquals(testValue, wrapper.get());

        AttributeKvEntry testValue2 = new BaseAttributeKvEntry(new StringDataEntry(TEST_KEY, TEST_VALUE), 1, 2L);
        cache.put(testKey, testValue2);

        wrapper = cache.get(testKey);
        assertNotNull(wrapper);

        assertEquals(testValue2, wrapper.get());

        AttributeKvEntry testValue3 = new BaseAttributeKvEntry(new StringDataEntry(TEST_KEY, TEST_VALUE), 1, 0L);
        cache.put(testKey, testValue3);

        wrapper = cache.get(testKey);
        assertNotNull(wrapper);

        assertEquals(testValue2, wrapper.get());

        cache.evict(testKey);
    }

    @Test
    public void testEvictWithVersion() {
        AttributeCacheKey testKey = new AttributeCacheKey(AttributeScope.CLIENT_SCOPE, DEVICE_ID, TEST_KEY);
        AttributeKvEntry testValue = new BaseAttributeKvEntry(new StringDataEntry(TEST_KEY, TEST_VALUE), 1, 1L);
        cache.put(testKey, testValue);

        TbCacheValueWrapper<AttributeKvEntry> wrapper = cache.get(testKey);
        assertNotNull(wrapper);

        assertEquals(testValue, wrapper.get());

        cache.evict(testKey, 2L);

        wrapper = cache.get(testKey);
        assertNotNull(wrapper);

        assertNull(wrapper.get());

        cache.evict(testKey);
    }

    @Test
    public void testEvict() {
        AttributeCacheKey testKey = new AttributeCacheKey(AttributeScope.CLIENT_SCOPE, DEVICE_ID, TEST_KEY);
        AttributeKvEntry testValue = new BaseAttributeKvEntry(new StringDataEntry(TEST_KEY, TEST_VALUE), 1, 1L);
        cache.put(testKey, testValue);

        TbCacheValueWrapper<AttributeKvEntry> wrapper = cache.get(testKey);
        assertNotNull(wrapper);

        assertEquals(testValue, wrapper.get());

        cache.evict(testKey);

        wrapper = cache.get(testKey);
        assertNull(wrapper);
    }
}
