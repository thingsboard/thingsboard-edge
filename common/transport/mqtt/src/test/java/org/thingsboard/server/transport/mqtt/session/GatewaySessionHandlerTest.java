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
package org.thingsboard.server.transport.mqtt.session;

import org.junit.Test;

import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.mock;

public class GatewaySessionHandlerTest {

    @Test
    public void givenWeakHashMap_WhenGC_thenMapIsEmpty() {
        WeakHashMap<String, Lock> map = new WeakHashMap<>();

        String deviceName = new String("device"); //constants are static and doesn't affected by GC, so use new instead
        map.put(deviceName, new ReentrantLock());
        assertTrue(map.containsKey(deviceName));

        deviceName = null;
        System.gc();

        await().atMost(10, TimeUnit.SECONDS).until(() -> !map.containsKey("device"));
    }

    @Test
    public void givenConcurrentReferenceHashMap_WhenGC_thenMapIsEmpty() {
        GatewaySessionHandler gsh = mock(GatewaySessionHandler.class);
        willCallRealMethod().given(gsh).createWeakMap();

        ConcurrentMap<String, Lock> map = gsh.createWeakMap();
        map.put("device", new ReentrantLock());
        assertTrue(map.containsKey("device"));

        System.gc();

        await().atMost(10, TimeUnit.SECONDS).until(() -> !map.containsKey("device"));
    }

}