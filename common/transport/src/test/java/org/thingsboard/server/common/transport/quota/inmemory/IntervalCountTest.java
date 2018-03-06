/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.common.transport.quota.inmemory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.transport.quota.Clock;

import static org.junit.Assert.assertEquals;

/**
 * @author Vitaliy Paromskiy
 * @version 1.0
 */
public class IntervalCountTest {

    @Before
    public void init() {
        Clock.setMillis(1000L);
    }

    @After
    public void clear() {
        Clock.reset();
    }

    @Test
    public void ticksInSameIntervalAreSummed() {
        IntervalCount intervalCount = new IntervalCount(100L);
        assertEquals(1L, intervalCount.resetIfExpiredAndTick());
        Clock.shift(100);
        assertEquals(2L, intervalCount.resetIfExpiredAndTick());
    }

    @Test
    public void oldDataCleanedWhenIntervalExpired() {
        IntervalCount intervalCount = new IntervalCount(100L);
        assertEquals(1L, intervalCount.resetIfExpiredAndTick());
        Clock.shift(101);
        assertEquals(1L, intervalCount.resetIfExpiredAndTick());
    }

    @Test
    public void silenceDurationCalculatedFromLastTick() {
        IntervalCount intervalCount = new IntervalCount(100L);
        assertEquals(1L, intervalCount.resetIfExpiredAndTick());
        Clock.shift(10L);
        assertEquals(10L, intervalCount.silenceDuration());
    }

}