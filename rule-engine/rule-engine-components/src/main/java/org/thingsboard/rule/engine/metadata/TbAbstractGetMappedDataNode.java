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
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.EntitiesFieldsAsyncLoader;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.thingsboard.common.util.DonAsynchron.withCallback;
import static org.thingsboard.server.common.data.DataConstants.SERVER_SCOPE;

@Slf4j
public abstract class TbAbstractGetMappedDataNode<T extends EntityId, C extends TbGetMappedDataNodeConfiguration> extends TbAbstractNodeWithFetchTo<C> {

    protected void checkIfMappingIsNotEmptyOrElseThrow(Map<String, String> dataMapping) throws TbNodeException {
        if (dataMapping == null || dataMapping.isEmpty()) {
            throw new TbNodeException("At least one mapping entry should be specified!");
        }
    }

    protected void processFieldsData(TbContext ctx, TbMsg msg, T entityId, ObjectNode msgDataAsJsonNode, boolean ignoreNullStrings) {
        var mappingsMap = processFieldsMappingPatterns(msg);
        withCallback(getEntityFieldsAsync(ctx, entityId, mappingsMap, ignoreNullStrings),
                data -> putFieldsDataAndTell(ctx, msg, msgDataAsJsonNode, data),
                t -> ctx.tellFailure(msg, t),
                MoreExecutors.directExecutor());
    }

    protected void processAttributesKvEntryData(TbContext ctx, TbMsg msg, T entityId, ObjectNode msgDataAsJsonNode) {
        var mappingsMap = processKvEntryMappingPatterns(msg);
        var sourceKeys = List.copyOf(mappingsMap.keySet());
        withCallback(getAttributesAsync(ctx, entityId, sourceKeys),
                data -> putKvEntryDataAndTell(ctx, msg, data, mappingsMap, msgDataAsJsonNode),
                t -> ctx.tellFailure(msg, t),
                MoreExecutors.directExecutor());
    }

    protected void processTsKvEntryData(TbContext ctx, TbMsg msg, T entityId, ObjectNode msgDataAsJsonNode) {
        var mappingsMap = processKvEntryMappingPatterns(msg);
        var sourceKeys = List.copyOf(mappingsMap.keySet());
        withCallback(getLatestTelemetryAsync(ctx, entityId, sourceKeys),
                data -> putKvEntryDataAndTell(ctx, msg, data, mappingsMap, msgDataAsJsonNode),
                t -> ctx.tellFailure(msg, t),
                MoreExecutors.directExecutor());
    }

    private void putFieldsDataAndTell(TbContext ctx, TbMsg msg, ObjectNode msgDataAsJsonNode, Map<String, String> targetKeysToSourceValuesMap) {
        TbMsgMetaData msgMetaData = msg.getMetaData().copy();
        for (var entry : targetKeysToSourceValuesMap.entrySet()) {
            var targetKeyName = entry.getKey();
            var sourceFieldValue = entry.getValue();
            if (FetchTo.DATA.equals(fetchTo)) {
                msgDataAsJsonNode.put(targetKeyName, sourceFieldValue);
            } else if (FetchTo.METADATA.equals(fetchTo)) {
                msgMetaData.putValue(targetKeyName, sourceFieldValue);
            }
        }
        TbMsg outMsg = transformMessage(msg, msgDataAsJsonNode, msgMetaData);
        ctx.tellSuccess(outMsg);
    }

    private void putKvEntryDataAndTell(TbContext ctx, TbMsg msg, List<? extends KvEntry> data, Map<String, String> map, ObjectNode msgData) {
        var msgMetaData = msg.getMetaData().copy();
        for (KvEntry entry : data) {
            String targetKey = map.get(entry.getKey());
            enrichMessage(msgData, msgMetaData, entry, targetKey);
        }
        ctx.tellSuccess(transformMessage(msg, msgData, msgMetaData));
    }

    private Map<String, String> processFieldsMappingPatterns(TbMsg msg) {
        var mappingsMap = new HashMap<String, String>();
        config.getDataMapping().forEach((sourceField, targetKey) -> {
            String patternProcessedTargetKey = TbNodeUtils.processPattern(targetKey, msg);
            mappingsMap.put(sourceField, patternProcessedTargetKey);
        });
        return mappingsMap;
    }

    private Map<String, String> processKvEntryMappingPatterns(TbMsg msg) {
        var mappingsMap = new HashMap<String, String>();
        config.getDataMapping().forEach((sourceKey, targetKey) -> {
            String patternProcessedSourceKey = TbNodeUtils.processPattern(sourceKey, msg);
            String patternProcessedTargetKey = TbNodeUtils.processPattern(targetKey, msg);
            mappingsMap.put(patternProcessedSourceKey, patternProcessedTargetKey);
        });
        return mappingsMap;
    }

    private ListenableFuture<Map<String, String>> getEntityFieldsAsync(TbContext ctx, EntityId entityId, Map<String, String> mappingsMap, boolean ignoreNullStrings) {
        return Futures.transform(EntitiesFieldsAsyncLoader.findAsync(ctx, entityId),
                fieldsData -> {
                    var targetKeysToSourceValuesMap = new HashMap<String, String>();
                    for (var mappingEntry : mappingsMap.entrySet()) {
                        var sourceFieldName = mappingEntry.getKey();
                        var targetKeyName = mappingEntry.getValue();
                        var sourceFieldValue = fieldsData.getFieldValue(sourceFieldName, ignoreNullStrings);
                        if (sourceFieldValue != null) {
                            targetKeysToSourceValuesMap.put(targetKeyName, sourceFieldValue);
                        }
                    }
                    return targetKeysToSourceValuesMap;
                }, ctx.getDbCallbackExecutor()
        );
    }

    private ListenableFuture<List<KvEntry>> getAttributesAsync(TbContext ctx, EntityId entityId, List<String> attrKeys) {
        var latest = ctx.getAttributesService().find(ctx.getTenantId(), entityId, SERVER_SCOPE, attrKeys);
        return Futures.transform(latest, l ->
                        l.stream()
                                .map(i -> (KvEntry) i)
                                .collect(Collectors.toList()),
                ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<List<KvEntry>> getLatestTelemetryAsync(TbContext ctx, EntityId entityId, List<String> timeseriesKeys) {
        var latest = ctx.getTimeseriesService().findLatest(ctx.getTenantId(), entityId, timeseriesKeys);
        return Futures.transform(latest, l ->
                        l.stream()
                                .map(i -> (KvEntry) i)
                                .collect(Collectors.toList()),
                ctx.getDbCallbackExecutor());
    }

}
