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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.analytics.incoming.MathFunction;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
public class AggLatestMapping {

    private String source;
    private String sourceScope;
    private double defaultValue;
    private String target;
    private MathFunction aggFunction;
    private AggLatestMappingFilter filter;

    public ListenableFuture<Optional<JsonObject>> aggregate(TbContext ctx, Map<String, ScriptEngine> attributesScriptEngineMap,
                                            ListenableFuture<List<EntityId>> entityIds) {
        ListenableFuture<List<EntityId>> filteredEntityIds =
                filter != null
                        ? Futures.transformAsync(entityIds, ids -> filter.filterEntityIds(ctx, attributesScriptEngineMap, ids), ctx.getDbCallbackExecutor())
                        : entityIds;

        return Futures.transform(filteredEntityIds, ids -> {
            TbAggFunction aggregation = TbAggFunctionFactory.createAggFunction(aggFunction);
            ids.forEach(entityId -> {
                Optional<KvEntry> entry = aggregation.fetchAttrValue() ? fetchValue(ctx, entityId) : Optional.empty();
                aggregation.update(entry, defaultValue);
            });
            Optional<JsonElement> result = aggregation.result();
            if (result.isPresent()) {
                JsonObject obj = new JsonObject();
                obj.add(target, result.get());
                return Optional.of(obj);
            } else {
                return Optional.empty();
            }
        }, ctx.getDbCallbackExecutor());
    }

    private Optional<KvEntry> fetchValue(TbContext ctx, EntityId entityId) {
        try {
            if ("LATEST_TELEMETRY".equals(sourceScope)) {
                ListenableFuture<List<TsKvEntry>> latest = ctx.getTimeseriesService().findLatest(ctx.getTenantId(), entityId, Collections.singletonList(source));
                List<TsKvEntry> latestTs = latest.get();
                if (latestTs != null && !latestTs.isEmpty() && latestTs.get(0).getValue() != null) {
                    return Optional.of(latestTs.get(0));
                }
            } else {
                ListenableFuture<Optional<AttributeKvEntry>> latest = ctx.getAttributesService().find(ctx.getTenantId(), entityId, sourceScope, source);
                Optional<AttributeKvEntry> latestAttr = latest.get();
                if (latestAttr.isPresent()) {
                    return Optional.of(latestAttr.get());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch value of attribute/telemetry telemetry [" + source + "] of entity [" + entityId + "]", e);
        }
        return Optional.empty();
    }

}
