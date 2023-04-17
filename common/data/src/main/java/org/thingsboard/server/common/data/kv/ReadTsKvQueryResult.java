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
package org.thingsboard.server.common.data.kv;

import lombok.Data;
import org.thingsboard.server.common.data.query.TsValue;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReadTsKvQueryResult {

    private final int queryId;
    // Holds the data list;
    private final List<TsKvEntry> data;
    // Holds the max ts of the records that match aggregation intervals (not the ts of the aggregation window, but the ts of the last record among all the intervals)
    private final long lastEntryTs;

    public TsValue[] toTsValues() {
        if (data != null && !data.isEmpty()) {
            List<TsValue> queryValues = new ArrayList<>();
            for (TsKvEntry v : data) {
                queryValues.add(v.toTsValue()); // TODO: add count here.
            }
            return queryValues.toArray(new TsValue[queryValues.size()]);
        } else {
            return new TsValue[0];
        }
    }

    public TsValue toTsValue(ReadTsKvQuery query) {
        if (data == null || data.isEmpty()) {
            if (Aggregation.SUM.equals(query.getAggregation()) || Aggregation.COUNT.equals(query.getAggregation())) {
                long ts = query.getStartTs() + (query.getEndTs() - query.getStartTs()) / 2;
                return new TsValue(ts, "0");
            } else {
                return TsValue.EMPTY;
            }
        }
        if (data.size() > 1) {
            throw new RuntimeException("Query Result has multiple data points!");
        }
        return data.get(0).toTsValue();
    }

}
