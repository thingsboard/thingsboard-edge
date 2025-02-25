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
package org.thingsboard.rule.engine.api;

import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.NoOpFutureCallback;

import static org.assertj.core.api.Assertions.assertThat;

class TimeseriesSaveRequestTest {

    @Test
    void testDefaultSaveStrategyIsProcessAll() {
        var request = TimeseriesSaveRequest.builder().build();

        assertThat(request.getStrategy()).isEqualTo(TimeseriesSaveRequest.Strategy.PROCESS_ALL);
    }

    @Test
    void testSaveAllStrategy() {
        assertThat(TimeseriesSaveRequest.Strategy.PROCESS_ALL).isEqualTo(new TimeseriesSaveRequest.Strategy(true, true, true, true));
    }

    @Test
    void testWsOnlyStrategy() {
        assertThat(TimeseriesSaveRequest.Strategy.WS_ONLY).isEqualTo(new TimeseriesSaveRequest.Strategy(false, false, true, false));
    }

    @Test
    void testLatestAndWsStrategy() {
        assertThat(TimeseriesSaveRequest.Strategy.LATEST_AND_WS).isEqualTo(new TimeseriesSaveRequest.Strategy(false, true, true, false));
    }

    @Test
    void testSkipAllStrategy() {
        assertThat(TimeseriesSaveRequest.Strategy.SKIP_ALL).isEqualTo(new TimeseriesSaveRequest.Strategy(false, false, false, false));
    }

    @Test
    void testDefaultCallbackIsNoOp() {
        var request = TimeseriesSaveRequest.builder().build();

        assertThat(request.getCallback()).isEqualTo(NoOpFutureCallback.instance());
    }

    @Test
    void testNullCallbackIsNoOp() {
        var request = TimeseriesSaveRequest.builder().callback(null).build();

        assertThat(request.getCallback()).isEqualTo(NoOpFutureCallback.instance());
    }

}
