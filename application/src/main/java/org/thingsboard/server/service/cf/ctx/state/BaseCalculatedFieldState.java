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

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.utils.CalculatedFieldUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.thingsboard.server.utils.CalculatedFieldUtils.toSingleValueArgumentProto;

@Data
@AllArgsConstructor
public abstract class BaseCalculatedFieldState implements CalculatedFieldState {

    protected List<String> requiredArguments;
    protected Map<String, ArgumentEntry> arguments;
    protected boolean sizeExceedsLimit;

    protected long lastUpdateTimestamp = -1;

    public BaseCalculatedFieldState(List<String> requiredArguments) {
        this.requiredArguments = requiredArguments;
        this.arguments = new HashMap<>();
    }

    public BaseCalculatedFieldState() {
        this(new ArrayList<>(), new HashMap<>(), false, -1);
    }

    @Override
    public boolean updateState(CalculatedFieldCtx ctx, Map<String, ArgumentEntry> argumentValues) {
        if (arguments == null) {
            arguments = new HashMap<>();
        }

        boolean stateUpdated = false;

        for (Map.Entry<String, ArgumentEntry> entry : argumentValues.entrySet()) {
            String key = entry.getKey();
            ArgumentEntry newEntry = entry.getValue();

            checkArgumentSize(key, newEntry, ctx);

            ArgumentEntry existingEntry = arguments.get(key);
            boolean entryUpdated;

            if (existingEntry == null || newEntry.isForceResetPrevious()) {
                validateNewEntry(newEntry);
                arguments.put(key, newEntry);
                entryUpdated = true;
            } else {
                entryUpdated = existingEntry.updateEntry(newEntry);
            }

            if (entryUpdated) {
                stateUpdated = true;
                updateLastUpdateTimestamp(newEntry);
            }

        }

        return stateUpdated;
    }

    @Override
    public boolean isReady() {
        return arguments.keySet().containsAll(requiredArguments) &&
                arguments.values().stream().noneMatch(ArgumentEntry::isEmpty);
    }

    @Override
    public void checkStateSize(CalculatedFieldEntityCtxId ctxId, long maxStateSize) {
        if (!sizeExceedsLimit && maxStateSize > 0 && CalculatedFieldUtils.toProto(ctxId, this).getSerializedSize() > maxStateSize) {
            arguments.clear();
            sizeExceedsLimit = true;
        }
    }

    @Override
    public void checkArgumentSize(String name, ArgumentEntry entry, CalculatedFieldCtx ctx) {
        if (entry instanceof TsRollingArgumentEntry) {
            return;
        }
        if (entry instanceof SingleValueArgumentEntry singleValueArgumentEntry) {
            if (ctx.getMaxSingleValueArgumentSize() > 0 && toSingleValueArgumentProto(name, singleValueArgumentEntry).getSerializedSize() > ctx.getMaxSingleValueArgumentSize()) {
                throw new IllegalArgumentException("Single value size exceeds the maximum allowed limit. The argument will not be used for calculation.");
            }
        }
    }

    protected abstract void validateNewEntry(ArgumentEntry newEntry);

    private void updateLastUpdateTimestamp(ArgumentEntry entry) {
        if (entry instanceof SingleValueArgumentEntry singleValueArgumentEntry) {
            this.lastUpdateTimestamp = singleValueArgumentEntry.getTs();
        } else if (entry instanceof TsRollingArgumentEntry tsRollingArgumentEntry) {
            Map.Entry<Long, Double> lastEntry = tsRollingArgumentEntry.getTsRecords().lastEntry();
            this.lastUpdateTimestamp = (lastEntry != null) ? lastEntry.getKey() : System.currentTimeMillis();
        }
    }

}
