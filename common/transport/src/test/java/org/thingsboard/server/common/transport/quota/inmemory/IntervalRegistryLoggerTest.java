/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
package org.thingsboard.server.common.transport.quota.inmemory;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.transport.quota.host.HostIntervalRegistryLogger;
import org.thingsboard.server.common.transport.quota.host.HostRequestIntervalRegistry;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Vitaliy Paromskiy
 * @version 1.0
 */
public class IntervalRegistryLoggerTest {

    private IntervalRegistryLogger logger;

    private HostRequestIntervalRegistry requestRegistry = mock(HostRequestIntervalRegistry.class);

    @Before
    public void init() {
        logger = new HostIntervalRegistryLogger(3, 10, requestRegistry);
    }

    @Test
    public void onlyMaxHostsCollected() {
        Map<String, Long> map = ImmutableMap.of("a", 8L, "b", 3L, "c", 1L, "d", 3L);
        Map<String, Long> actual = logger.getTopElements(map);
        Map<String, Long> expected = ImmutableMap.of("a", 8L, "b", 3L, "d", 3L);

        assertEquals(expected, actual);
    }

    @Test
    public void emptyMapProcessedCorrectly() {
        Map<String, Long> map = Collections.emptyMap();
        Map<String, Long> actual = logger.getTopElements(map);
        Map<String, Long> expected = Collections.emptyMap();

        assertEquals(expected, actual);
    }

}