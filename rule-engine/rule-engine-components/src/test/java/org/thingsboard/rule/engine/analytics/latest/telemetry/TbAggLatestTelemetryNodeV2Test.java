/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.verification.Times;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;
import org.thingsboard.common.util.AbstractListeningExecutor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.analytics.incoming.MathFunction;
import org.thingsboard.rule.engine.analytics.latest.ParentEntitiesRelationsQuery;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbPeContext;
import org.thingsboard.rule.engine.api.TbRelationTypes;
import org.thingsboard.rule.engine.data.RelationsQuery;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class TbAggLatestTelemetryNodeV2Test {

    private final Gson gson = new Gson();

    @Mock
    private TbContext ctx;

    @Mock
    private TbPeContext peCtx;

    @Mock
    private RelationService relationService;

    @Mock
    private AttributesService attributesService;

    @Mock
    private TimeseriesService timeseriesService;

    @Mock
    private ScriptEngine scriptEngine;

    private AbstractListeningExecutor executor;
    private TbAggLatestTelemetryNodeV2 node;
    private TbNodeConfiguration nodeConfiguration;
    private TenantId tenantId;
    private AssetId assetId;

    @Before
    public void before() {
        tenantId = new TenantId(UUID.randomUUID());
        assetId = new AssetId(UUID.randomUUID());
        node = new TbAggLatestTelemetryNodeV2();

        executor = new AbstractListeningExecutor() {
            @Override
            protected int getThreadPollSize() {
                return 1;
            }
        };
        executor.init();

        when(ctx.getTenantId()).thenReturn(tenantId);
        when(ctx.getDbCallbackExecutor()).thenReturn(executor);
        when(ctx.getRelationService()).thenReturn(relationService);
        when(ctx.getTimeseriesService()).thenReturn(timeseriesService);
    }

    @After
    public void after() {
        executor.destroy();
        node.destroy();
    }

    @Test
    public void testSimpleAggregationWithoutFilter() throws TbNodeException, ExecutionException, InterruptedException {
        TbAggLatestTelemetryNodeV2Configuration config = new TbAggLatestTelemetryNodeV2Configuration();

        List<AggLatestMapping> aggMappings = new ArrayList<>();

        AggLatestMapping avgTempMapping = new AggLatestMapping();
        avgTempMapping.setSource("temperature");
        avgTempMapping.setSourceScope("LATEST_TELEMETRY");
        avgTempMapping.setAggFunction(MathFunction.AVG);
        avgTempMapping.setDefaultValue(0);
        avgTempMapping.setTarget("latestAvgTemperature");
        aggMappings.add(avgTempMapping);

        AggLatestMapping countMapping = new AggLatestMapping();
        countMapping.setSource("temperature");
        countMapping.setSourceScope("LATEST_TELEMETRY");
        countMapping.setAggFunction(MathFunction.COUNT);
        countMapping.setDefaultValue(0);
        countMapping.setTarget("deviceCount");
        aggMappings.add(countMapping);

        config.setAggMappings(aggMappings);

        config.setDirection(EntitySearchDirection.FROM);
        config.setRelationType(EntityRelation.CONTAINS_TYPE);
        config.setOutMsgType(SessionMsgType.POST_TELEMETRY_REQUEST.name());

        DeviceId deviceA = new DeviceId(UUID.randomUUID());
        DeviceId deviceB = new DeviceId(UUID.randomUUID());
        EntityRelation relationA = new EntityRelation(assetId, deviceA, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(assetId, deviceB, EntityRelation.CONTAINS_TYPE);

        TsKvEntry deviceATemperature = new BasicTsKvEntry(System.currentTimeMillis(), new DoubleDataEntry("temperature", 42.0));
        TsKvEntry deviceBTemperature = new BasicTsKvEntry(System.currentTimeMillis(), new DoubleDataEntry("temperature", 44.0));

        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        Mockito.when(relationService.findByFromAndTypeAsync(tenantId, assetId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON))
                .thenReturn(Futures.immediateFuture(Arrays.asList(relationA, relationB)));
        Mockito.when(timeseriesService.findLatest(tenantId, deviceA, new HashSet<>(Arrays.asList("temperature"))))
                .thenReturn(Futures.immediateFuture(Arrays.asList(deviceATemperature)));
        Mockito.when(timeseriesService.findLatest(tenantId, deviceB, new HashSet<>(Arrays.asList("temperature"))))
                .thenReturn(Futures.immediateFuture(Arrays.asList(deviceBTemperature)));

        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), assetId, new TbMsgMetaData(), JacksonUtil.toString(JacksonUtil.newObjectNode()));
        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, Mockito.timeout(5000).times(1)).enqueueForTellNext(captor.capture(), eq(SUCCESS));

        TbMsg resultMsg = captor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        ObjectNode objectNode = (ObjectNode) JacksonUtil.toJsonNode(resultMsg.getData());
        Assert.assertEquals(43.0, objectNode.get("latestAvgTemperature").asDouble(), 0.0);
        Assert.assertEquals(2, objectNode.get("deviceCount").asInt());
    }
}
