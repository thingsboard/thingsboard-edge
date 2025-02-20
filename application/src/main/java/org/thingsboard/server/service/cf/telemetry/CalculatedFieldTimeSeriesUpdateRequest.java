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
package org.thingsboard.server.service.cf.telemetry;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class CalculatedFieldTimeSeriesUpdateRequest implements CalculatedFieldTelemetryUpdateRequest {

    private TenantId tenantId;
    private EntityId entityId;
    private List<? extends KvEntry> kvEntries;
    private List<CalculatedFieldId> previousCalculatedFieldIds;

    public CalculatedFieldTimeSeriesUpdateRequest(TimeseriesSaveRequest request) {
        this.tenantId = request.getTenantId();
        this.entityId = request.getEntityId();
        this.kvEntries = request.getEntries();
        this.previousCalculatedFieldIds = request.getPreviousCalculatedFieldIds();
    }

    @Override
    public Map<String, KvEntry> getMappedTelemetry(CalculatedFieldCtx ctx, EntityId referencedEntityId) {
        Map<String, KvEntry> mappedKvEntries = new HashMap<>();
        Map<TbPair<EntityId, ReferencedEntityKey>, String> referencedKeys = ctx.getReferencedEntityKeys();

        kvEntries.forEach(entry -> {
            String key = entry.getKey();

            ReferencedEntityKey tsLatestKey = new ReferencedEntityKey(key, ArgumentType.TS_LATEST, null);
            String argTsLatestName = referencedKeys.get(new TbPair<>(referencedEntityId, tsLatestKey));

            if (argTsLatestName != null) {
                mappedKvEntries.put(argTsLatestName, entry);
            } else {
                ReferencedEntityKey tsRollingKey = new ReferencedEntityKey(key, ArgumentType.TS_ROLLING, null);
                String argTsRollingName = referencedKeys.get(new TbPair<>(referencedEntityId, tsRollingKey));

                if (argTsRollingName != null) {
                    mappedKvEntries.put(argTsRollingName, entry);
                }
            }
        });

        return mappedKvEntries;
    }
}
