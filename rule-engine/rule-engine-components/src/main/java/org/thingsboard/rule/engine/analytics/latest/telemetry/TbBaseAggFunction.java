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
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.thingsboard.server.common.data.kv.KvEntry;

import java.util.Optional;

public abstract class TbBaseAggFunction implements TbAggFunction {

    private boolean hasResult = false;

    @Override
    public void update(Optional<KvEntry> entry, double defaultValue) {
        double value = extractDoubleValue(entry, defaultValue);
        doUpdate(value);
        hasResult = true;
    }

    @Override
    public Optional<JsonElement> result() {
        if (hasResult) {
            return Optional.of(new JsonPrimitive(prepareResult()));
        } else {
            return Optional.empty();
        }
    }

    protected abstract void doUpdate(double value);

    protected abstract double prepareResult();

    private double extractDoubleValue(Optional<KvEntry> entry, double defaultValue) {
        double result = defaultValue;
        if (entry.isPresent() && entry.get().getValue() != null) {
            KvEntry kvEntry = entry.get();
            switch (kvEntry.getDataType()) {
                case LONG:
                    result = kvEntry.getLongValue().get();
                    break;
                case DOUBLE:
                    result = kvEntry.getDoubleValue().get();
                    break;
                case BOOLEAN:
                    result = kvEntry.getBooleanValue().get() ? 1 : 0;
                    break;
                case STRING:
                    String str = kvEntry.getStrValue().get();
                    try {
                        result = Double.parseDouble(str);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Aggregation failed. Unable to parse value ["+ str +"]" +
                                " of attribute [" + kvEntry.getKey() + "] to Double");
                    }
                    break;
            }
        }
        return result;
    }
}
