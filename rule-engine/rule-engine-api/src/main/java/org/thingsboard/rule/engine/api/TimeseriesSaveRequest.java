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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.SettableFuture;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.thingsboard.common.util.NoOpFutureCallback;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TimeseriesSaveRequest implements CalculatedFieldSystemAwareRequest {

    private final TenantId tenantId;
    private final CustomerId customerId;
    private final EntityId entityId;
    private final List<TsKvEntry> entries;
    private final long ttl;
    private final Strategy strategy;
    private final boolean overwriteValue;
    private final List<CalculatedFieldId> previousCalculatedFieldIds;
    private final UUID tbMsgId;
    private final TbMsgType tbMsgType;
    private final FutureCallback<Void> callback;

    public record Strategy(boolean saveTimeseries, boolean saveLatest, boolean sendWsUpdate, boolean processCalculatedFields) {

        public static final Strategy PROCESS_ALL = new Strategy(true, true, true, true);
        public static final Strategy WS_ONLY = new Strategy(false, false, true, false);
        public static final Strategy LATEST_AND_WS = new Strategy(false, true, true, false);
        public static final Strategy SKIP_ALL = new Strategy(false, false, false, false);

    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private TenantId tenantId;
        private CustomerId customerId;
        private EntityId entityId;
        private List<TsKvEntry> entries;
        private long ttl;
        private Strategy strategy = Strategy.PROCESS_ALL;
        private boolean overwriteValue;
        private List<CalculatedFieldId> previousCalculatedFieldIds;
        private UUID tbMsgId;
        private TbMsgType tbMsgType;
        private FutureCallback<Void> callback;

        Builder() {}

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder customerId(CustomerId customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder entityId(EntityId entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder entries(List<TsKvEntry> entries) {
            this.entries = entries;
            return this;
        }

        public Builder entry(TsKvEntry entry) {
            return entries(List.of(entry));
        }

        public Builder entry(KvEntry kvEntry) {
            return entry(new BasicTsKvEntry(System.currentTimeMillis(), kvEntry));
        }

        public Builder ttl(long ttl) {
            this.ttl = ttl;
            return this;
        }

        public Builder strategy(Strategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder overwriteValue(boolean overwriteValue) {
            this.overwriteValue = overwriteValue;
            return this;
        }

        public Builder previousCalculatedFieldIds(List<CalculatedFieldId> previousCalculatedFieldIds) {
            this.previousCalculatedFieldIds = previousCalculatedFieldIds;
            return this;
        }

        public Builder tbMsgId(UUID tbMsgId) {
            this.tbMsgId = tbMsgId;
            return this;
        }

        public Builder tbMsgType(TbMsgType tbMsgType) {
            this.tbMsgType = tbMsgType;
            return this;
        }

        public Builder callback(FutureCallback<Void> callback) {
            this.callback = callback;
            return this;
        }

        public Builder future(SettableFuture<Void> future) {
            return callback(new FutureCallback<>() {
                @Override
                public void onSuccess(Void result) {
                    future.set(result);
                }

                @Override
                public void onFailure(Throwable t) {
                    future.setException(t);
                }
            });
        }

        public TimeseriesSaveRequest build() {
            return new TimeseriesSaveRequest(
                    tenantId, customerId, entityId, entries, ttl, strategy, overwriteValue,
                    previousCalculatedFieldIds, tbMsgId, tbMsgType, requireNonNullElse(callback, NoOpFutureCallback.instance())
            );
        }

    }

}
