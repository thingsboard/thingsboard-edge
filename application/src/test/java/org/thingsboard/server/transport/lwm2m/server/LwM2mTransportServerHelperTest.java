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
package org.thingsboard.server.transport.lwm2m.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_TELEMETRY;

class LwM2mTransportServerHelperTest {

    public static final String KEY_SW_STATE = "sw_state";
    public static final String DOWNLOADING = "DOWNLOADING";

    long now;
    List<TransportProtos.KeyValueProto> kvList;
    ConcurrentMap<String, AtomicLong> keyTsLatestMap;
    LwM2mTransportServerHelper helper;
    LwM2mTransportContext context;


    @BeforeEach
    void setUp() {
        now = System.currentTimeMillis();
        context = mock(LwM2mTransportContext.class);
        helper = spy(new LwM2mTransportServerHelper(context));
        willReturn(now).given(helper).getCurrentTimeMillis();
        kvList = List.of(
                TransportProtos.KeyValueProto.newBuilder().setKey(KEY_SW_STATE).setStringV(DOWNLOADING).build(),
                TransportProtos.KeyValueProto.newBuilder().setKey(LOG_LWM2M_TELEMETRY).setStringV("Transport log example").build()
        );
        keyTsLatestMap = new ConcurrentHashMap<>();
    }

    @Test
    void givenKeyAndLatestTsMapAndCurrentTs_whenGetTs_thenVerifyNoGetTsByKeyCall() {
        assertThat(helper.getTs(null, null)).isEqualTo(now);
        assertThat(helper.getTs(null, keyTsLatestMap)).isEqualTo(now);
        assertThat(helper.getTs(emptyList(), null)).isEqualTo(now);
        assertThat(helper.getTs(emptyList(), keyTsLatestMap)).isEqualTo(now);
        assertThat(helper.getTs(kvList, null)).isEqualTo(now);

        verify(helper, never()).getTsByKey(anyString(), anyMap(), anyLong());
        verify(helper, times(5)).getCurrentTimeMillis();
    }

    @Test
    void givenKeyAndLatestTsMapAndCurrentTs_whenGetTs_thenVerifyGetTsByKeyCallByFirstKey() {
        assertThat(helper.getTs(kvList, keyTsLatestMap)).isEqualTo(now);

        verify(helper, times(1)).getTsByKey(kvList.get(0).getKey(), keyTsLatestMap, now);
        verify(helper, times(1)).getTsByKey(anyString(), anyMap(), anyLong());
    }

    @Test
    void givenKeyAndEmptyLatestTsMap_whenGetTsByKey_thenAddToMapAndReturnNow() {
        assertThat(keyTsLatestMap).as("ts latest map before").isEmpty();

        assertThat(helper.getTsByKey(KEY_SW_STATE, keyTsLatestMap, now)).as("getTsByKey").isEqualTo(now);

        assertThat(keyTsLatestMap).as("ts latest map after").hasSize(1);
        assertThat(keyTsLatestMap.get(KEY_SW_STATE)).as("key present").isNotNull();
        assertThat(keyTsLatestMap.get(KEY_SW_STATE).get()).as("ts in map by key").isEqualTo(now);
    }

    @Test
    void givenKeyAndLatestTsMapWithExistedKey_whenGetTsByKey_thenCallSwapOrIncrementMethod() {
        keyTsLatestMap.put(KEY_SW_STATE, new AtomicLong());
        keyTsLatestMap.put("other", new AtomicLong());
        assertThat(keyTsLatestMap).as("ts latest map").hasSize(2);
        willReturn(now).given(helper).compareAndSwapOrIncrementTsAtomically(any(AtomicLong.class), anyLong());

        assertThat(helper.getTsByKey(KEY_SW_STATE, keyTsLatestMap, now)).as("getTsByKey").isEqualTo(now);

        verify(helper, times(1)).compareAndSwapOrIncrementTsAtomically(keyTsLatestMap.get(KEY_SW_STATE), now);
        verify(helper, times(1)).compareAndSwapOrIncrementTsAtomically(any(AtomicLong.class), anyLong());
    }

    @Test
    void givenMapWithTsValueLessThanNow_whenCompareAndSwapOrIncrementTsAtomically_thenReturnNow() {
        keyTsLatestMap.put(KEY_SW_STATE, new AtomicLong(now - 1));
        assertThat(helper.compareAndSwapOrIncrementTsAtomically(keyTsLatestMap.get(KEY_SW_STATE), now)).isEqualTo(now);
    }

    @Test
    void givenMapWithTsValueEqualsNow_whenCompareAndSwapOrIncrementTsAtomically_thenReturnNowIncremented() {
        keyTsLatestMap.put(KEY_SW_STATE, new AtomicLong(now));
        assertThat(helper.compareAndSwapOrIncrementTsAtomically(keyTsLatestMap.get(KEY_SW_STATE), now)).isEqualTo(now + 1);
    }

    @Test
    void givenMapWithTsValueGreaterThanNow_whenCompareAndSwapOrIncrementTsAtomically_thenReturnGreaterThanNowIncremented() {
        final long nextHourTs = now + TimeUnit.HOURS.toMillis(1);
        keyTsLatestMap.put(KEY_SW_STATE, new AtomicLong(nextHourTs));
        assertThat(helper.compareAndSwapOrIncrementTsAtomically(keyTsLatestMap.get(KEY_SW_STATE), now)).isEqualTo(nextHourTs + 1);
    }

}
