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
package org.thingsboard.server.service.cf.ctx.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;

import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TsRollingArgumentEntryTest {

    private TsRollingArgumentEntry entry;

    private final long ts = System.currentTimeMillis();

    @BeforeEach
    void setUp() {
        TreeMap<Long, Double> values = new TreeMap<>();
        values.put(ts - 40, 10.0);
        values.put(ts - 30, 12.0);
        values.put(ts - 20, 17.0);

        entry = new TsRollingArgumentEntry(5, 30000L, values);
    }

    @Test
    void testArgumentEntryType() {
        assertThat(entry.getType()).isEqualTo(ArgumentEntryType.TS_ROLLING);
    }

    @Test
    void testUpdateEntryWhenSingleValueEntryPassed() {
        SingleValueArgumentEntry newEntry = new SingleValueArgumentEntry(ts - 10, new DoubleDataEntry("key", 23.0), 123L);

        assertThat(entry.updateEntry(newEntry)).isTrue();
        assertThat(entry.getTsRecords()).hasSize(4);
        assertThat(entry.getTsRecords().get(ts - 10)).isEqualTo(23.0);
    }

    @Test
    void testUpdateEntryWhenRollingEntryPassed() {
        TsRollingArgumentEntry newEntry = new TsRollingArgumentEntry();
        TreeMap<Long, Double> values = new TreeMap<>();
        values.put(ts - 10, 7.0);
        values.put(ts - 5, 1.0);
        newEntry.setTsRecords(values);

        assertThat(entry.updateEntry(newEntry)).isTrue();
        assertThat(entry.getTsRecords()).hasSize(5);
        assertThat(entry.getTsRecords()).isEqualTo(Map.of(
                ts - 40, 10.0,
                ts - 30, 12.0,
                ts - 20, 17.0,
                ts - 10, 7.0,
                ts - 5, 1.0
        ));
    }

    @Test
    void testUpdateEntryWhenValueIsNotNumber() {
        SingleValueArgumentEntry newEntry = new SingleValueArgumentEntry(ts - 10, new StringDataEntry("key", "string"), 123L);

        assertThatThrownBy(() -> entry.updateEntry(newEntry))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Time series rolling arguments supports only numeric values.");
    }

    @Test
    void testUpdateEntryWhenOldTelemetry() {
        TsRollingArgumentEntry newEntry = new TsRollingArgumentEntry();
        TreeMap<Long, Double> values = new TreeMap<>();
        values.put(ts - 40000, 4.0);// will not be used for calculation
        values.put(ts - 45000, 2.0);// will not be used for calculation
        values.put(ts - 5, 0.0);
        newEntry.setTsRecords(values);

        entry = new TsRollingArgumentEntry(3, 30000L);
        assertThat(entry.updateEntry(newEntry)).isTrue();
        assertThat(entry.getTsRecords()).hasSize(1);
        assertThat(entry.getTsRecords()).isEqualTo(Map.of(
                ts - 5, 0.0
        ));
    }

    @Test
    void testPerformCalculationWhenArgumentsMoreThanLimit() {
        TsRollingArgumentEntry newEntry = new TsRollingArgumentEntry();
        TreeMap<Long, Double> values = new TreeMap<>();
        values.put(ts - 20, 1000.0);// will not be used
        values.put(ts - 18, 0.0);
        values.put(ts - 16, 0.0);
        values.put(ts - 14, 0.0);
        newEntry.setTsRecords(values);

        entry = new TsRollingArgumentEntry(3, 30000L);
        assertThat(entry.updateEntry(newEntry)).isTrue();
        assertThat(entry.getTsRecords()).hasSize(3);
        assertThat(entry.getTsRecords()).isEqualTo(Map.of(
                ts - 18, 0.0,
                ts - 16, 0.0,
                ts - 14, 0.0
        ));
    }

}