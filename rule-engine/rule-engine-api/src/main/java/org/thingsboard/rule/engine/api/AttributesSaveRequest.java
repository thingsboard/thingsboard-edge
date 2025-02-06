/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
import lombok.ToString;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;

import java.util.List;

@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AttributesSaveRequest {

    private final TenantId tenantId;
    private final EntityId entityId;
    private final AttributeScope scope;
    private final List<AttributeKvEntry> entries;
    private final boolean notifyDevice;
    private final FutureCallback<Void> callback;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private TenantId tenantId;
        private EntityId entityId;
        private AttributeScope scope;
        private List<AttributeKvEntry> entries;
        private boolean notifyDevice = true;
        private FutureCallback<Void> callback;

        Builder() {}

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder entityId(EntityId entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder scope(AttributeScope scope) {
            this.scope = scope;
            return this;
        }

        @Deprecated
        public Builder scope(String scope) {
            try {
                this.scope = AttributeScope.valueOf(scope);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid attribute scope '" + scope + "'");
            }
            return this;
        }

        public Builder entries(List<AttributeKvEntry> entries) {
            this.entries = entries;
            return this;
        }

        public Builder entry(AttributeKvEntry entry) {
            return entries(List.of(entry));
        }

        public Builder entry(KvEntry kvEntry) {
            return entry(new BaseAttributeKvEntry(kvEntry, System.currentTimeMillis()));
        }

        public Builder notifyDevice(boolean notifyDevice) {
            this.notifyDevice = notifyDevice;
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

        public AttributesSaveRequest build() {
            return new AttributesSaveRequest(tenantId, entityId, scope, entries, notifyDevice, callback);
        }

    }

}
