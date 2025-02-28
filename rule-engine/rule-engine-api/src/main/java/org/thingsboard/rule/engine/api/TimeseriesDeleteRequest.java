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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.msg.TbMsgType;

import java.util.List;
import java.util.UUID;

@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TimeseriesDeleteRequest implements CalculatedFieldSystemAwareRequest {

    private final TenantId tenantId;
    private final EntityId entityId;
    private final List<String> keys;
    private final List<DeleteTsKvQuery> deleteHistoryQueries;
    private final List<CalculatedFieldId> previousCalculatedFieldIds;
    private final UUID tbMsgId;
    private final TbMsgType tbMsgType;
    private final FutureCallback<List<String>> callback;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private TenantId tenantId;
        private EntityId entityId;
        private List<String> keys;
        private List<DeleteTsKvQuery> deleteHistoryQueries;
        private List<CalculatedFieldId> previousCalculatedFieldIds;
        private UUID tbMsgId;
        private TbMsgType tbMsgType;
        private FutureCallback<List<String>> callback;

        Builder() {
        }

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder entityId(EntityId entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder keys(List<String> keys) {
            this.keys = keys;
            return this;
        }

        public Builder deleteHistoryQueries(List<DeleteTsKvQuery> deleteHistoryQueries) {
            this.deleteHistoryQueries = deleteHistoryQueries;
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

        public Builder callback(FutureCallback<List<String>> callback) {
            this.callback = callback;
            return this;
        }

        public TimeseriesDeleteRequest build() {
            return new TimeseriesDeleteRequest(tenantId, entityId, keys, deleteHistoryQueries, previousCalculatedFieldIds, tbMsgId, tbMsgType, callback);
        }

    }

}
