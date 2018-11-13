/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.thingsboard.server.common.data.DataConstants.*;

@Data
public class AggLatestMappingFilter {

    private List<String> clientAttributeNames;
    private List<String> sharedAttributeNames;
    private List<String> serverAttributeNames;
    private List<String> latestTsKeyNames;

    private String filterFunction;

    @JsonIgnore
    private Predicate<EntityId> filter;

    public Predicate<EntityId> getFilterFunction(TbContext ctx, Map<String, ScriptEngine> attributesScriptEngineMap) {
        if (filter == null) {
            filter = entityId -> {
                try {
                    Map<String,String> attributes = new HashMap<>();
                    prepareAttributes(ctx, attributes, entityId, CLIENT_SCOPE, clientAttributeNames, "cs_");
                    prepareAttributes(ctx, attributes, entityId, SHARED_SCOPE, sharedAttributeNames, "shared_");
                    prepareAttributes(ctx, attributes, entityId, SERVER_SCOPE, serverAttributeNames, "ss_");
                    prepareTimeseries(ctx, attributes, entityId, latestTsKeyNames);

                    ScriptEngine attributesScriptEngine = attributesScriptEngineMap.computeIfAbsent(filterFunction,
                            function -> ctx.getPeContext().createAttributesJsScriptEngine(function));

                    return attributesScriptEngine.executeAttributesFilter(attributes);
                } catch (Exception e) {
                    throw new RuntimeException("[" + entityId + "] Failed to execute attributes mapping filter!", e);
                }
            };
        }
        return filter;
    }

    private void prepareAttributes(TbContext ctx, Map<String,String> attributes, EntityId entityId, String scope, List<String> keys, String prefix) throws Exception {
        if (keys != null && !keys.isEmpty()) {
            ListenableFuture<List<AttributeKvEntry>> latest = ctx.getAttributesService().find(ctx.getTenantId(), entityId, scope, keys);
            latest.get().forEach(r -> {
                if (r.getValue() != null) {
                    attributes.put(prefix + r.getKey(), r.getValueAsString());
                }
            });
        }
    }

    private void prepareTimeseries(TbContext ctx, Map<String,String> attributes, EntityId entityId, List<String> keys) throws Exception {
        if (keys != null && !keys.isEmpty()) {
            ListenableFuture<List<TsKvEntry>> latest = ctx.getTimeseriesService().findLatest(ctx.getTenantId(), entityId, keys);
            latest.get().forEach(r -> {
                if (r.getValue() != null) {
                    attributes.put(r.getKey(), r.getValueAsString());
                }
            });
        }
    }

}
