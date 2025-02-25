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
package org.thingsboard.script.api.tbel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import static org.thingsboard.script.api.tbel.TbelCfTsDoubleVal.OBJ_SIZE;

public class TbelCfTsRollingArg implements TbelCfArg, Iterable<TbelCfTsDoubleVal> {

    @Getter
    private final TbTimeWindow timeWindow;
    @Getter
    private final List<TbelCfTsDoubleVal> values;

    @JsonCreator
    public TbelCfTsRollingArg(
            @JsonProperty("timeWindow") TbTimeWindow timeWindow,
            @JsonProperty("values") List<TbelCfTsDoubleVal> values
    ) {
        this.timeWindow = timeWindow;
        this.values = Collections.unmodifiableList(values);
    }

    public TbelCfTsRollingArg(int limit, long timeWindow, List<TbelCfTsDoubleVal> values) {
        long ts = System.currentTimeMillis();
        this.timeWindow = new TbTimeWindow(ts - timeWindow, ts, limit);
        this.values = Collections.unmodifiableList(values);
    }

    @Override
    public long memorySize() {
        return 12 + values.size() * OBJ_SIZE;
    }

    @JsonIgnore
    public List<TbelCfTsDoubleVal> getValue() {
        return values;
    }

    public double max() {
        double max = Double.MIN_VALUE;
        for (TbelCfTsDoubleVal value : values) {
            double val = value.getValue();
            if (max < val) {
                max = val;
            }
        }
        return max;
    }

    @JsonIgnore
    public int getSize() {
        return values.size();
    }

    @Override
    public Iterator<TbelCfTsDoubleVal> iterator() {
        return values.iterator();
    }

    @Override
    public void forEach(Consumer<? super TbelCfTsDoubleVal> action) {
        values.forEach(action);
    }

    @Override
    public String getType() {
        return "TS_ROLLING";
    }

}
