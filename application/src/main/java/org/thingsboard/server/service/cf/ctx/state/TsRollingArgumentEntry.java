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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.script.api.tbel.TbelCfTsDoubleVal;
import org.thingsboard.script.api.tbel.TbelCfTsRollingArg;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class TsRollingArgumentEntry implements ArgumentEntry {

    private Integer limit;
    private Long timeWindow;
    private TreeMap<Long, Double> tsRecords = new TreeMap<>();

    private boolean forceResetPrevious;

    public TsRollingArgumentEntry(List<TsKvEntry> kvEntries, int limit, long timeWindow) {
        this.limit = limit;
        this.timeWindow = timeWindow;
        kvEntries.forEach(tsKvEntry -> addTsRecord(tsKvEntry.getTs(), tsKvEntry));
    }

    public TsRollingArgumentEntry(TreeMap<Long, Double> tsRecords, int limit, long timeWindow) {
        this.tsRecords = tsRecords;
        this.limit = limit;
        this.timeWindow = timeWindow;
    }

    public TsRollingArgumentEntry(int limit, long timeWindow) {
        this.tsRecords = new TreeMap<>();
        this.limit = limit;
        this.timeWindow = timeWindow;
    }

    public TsRollingArgumentEntry(Integer limit, Long timeWindow, TreeMap<Long, Double> tsRecords) {
        this.limit = limit;
        this.timeWindow = timeWindow;
        this.tsRecords = tsRecords;
    }

    @Override
    public ArgumentEntryType getType() {
        return ArgumentEntryType.TS_ROLLING;
    }

    @Override
    public boolean isEmpty() {
        return tsRecords.isEmpty();
    }

    @JsonIgnore
    @Override
    public Object getValue() {
        return tsRecords;
    }

    @Override
    public TbelCfArg toTbelCfArg() {
        List<TbelCfTsDoubleVal> values = new ArrayList<>(tsRecords.size());
        for (var e : tsRecords.entrySet()) {
            values.add(new TbelCfTsDoubleVal(e.getKey(), e.getValue()));
        }
        return new TbelCfTsRollingArg(timeWindow, values);
    }

    @Override
    public boolean updateEntry(ArgumentEntry entry) {
        if (entry instanceof TsRollingArgumentEntry tsRollingEntry) {
            updateTsRollingEntry(tsRollingEntry);
        } else if (entry instanceof SingleValueArgumentEntry singleValueEntry) {
            updateSingleValueEntry(singleValueEntry);
        } else {
            throw new IllegalArgumentException("Unsupported argument entry type for rolling argument entry: " + entry.getType());
        }
        return true;
    }

    private void updateTsRollingEntry(TsRollingArgumentEntry tsRollingEntry) {
        for (Map.Entry<Long, Double> tsRecordEntry : tsRollingEntry.getTsRecords().entrySet()) {
            addTsRecord(tsRecordEntry.getKey(), tsRecordEntry.getValue());
        }
    }

    private void updateSingleValueEntry(SingleValueArgumentEntry singleValueEntry) {
        addTsRecord(singleValueEntry.getTs(), singleValueEntry.getKvEntryValue());
    }

    private void addTsRecord(Long ts, KvEntry value) {
        try {
            switch (value.getDataType()) {
                case LONG -> value.getLongValue().ifPresent(aLong -> tsRecords.put(ts, aLong.doubleValue()));
                case DOUBLE -> value.getDoubleValue().ifPresent(aDouble -> tsRecords.put(ts, aDouble));
                case BOOLEAN -> value.getBooleanValue().ifPresent(aBoolean -> tsRecords.put(ts, aBoolean ? 1.0 : 0.0));
                case STRING -> value.getStrValue().ifPresent(aString -> tsRecords.put(ts, Double.parseDouble(aString)));
                case JSON -> value.getJsonValue().ifPresent(aString -> tsRecords.put(ts, Double.parseDouble(aString)));
            }
        } catch (Exception e) {
            tsRecords.put(ts, Double.NaN);
            log.debug("Invalid value '{}' for time series rolling arguments. Only numeric values are supported.", value.getValue());
        } finally {
            cleanupExpiredRecords();
        }
    }

    private void addTsRecord(Long ts, double value) {
        tsRecords.put(ts, value);
        cleanupExpiredRecords();
    }

    private void cleanupExpiredRecords() {
        if (tsRecords.size() > limit) {
            tsRecords.pollFirstEntry();
        }
        tsRecords.entrySet().removeIf(tsRecord -> tsRecord.getKey() < System.currentTimeMillis() - timeWindow);
    }

}
