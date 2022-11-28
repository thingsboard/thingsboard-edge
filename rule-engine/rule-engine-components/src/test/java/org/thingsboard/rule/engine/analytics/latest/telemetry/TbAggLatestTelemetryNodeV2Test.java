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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.thingsboard.common.util.AbstractListeningExecutor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.analytics.incoming.MathFunction;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbPeContext;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class TbAggLatestTelemetryNodeV2Test {

    public static final String FILTER_FUNCTION = "return Number(attributes['temperature']) > 42;";
    private static final String TB_AGG_LATEST_NODE_MSG = "TbAggLatestNodeMsg";
    @Mock
    private TbContext ctx;
    @Mock
    private TbPeContext peCtx;
    @Mock
    private RelationService relationService;
    @Mock
    private TimeseriesService timeseriesService;
    @Mock
    private ScriptEngine scriptEngine;
    private AbstractListeningExecutor executor;
    private TbAggLatestTelemetryNodeV2 node;
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
        when(ctx.getPeContext()).thenReturn(peCtx);
        when(ctx.getDbCallbackExecutor()).thenReturn(executor);
        when(ctx.getRelationService()).thenReturn(relationService);
        when(ctx.getTimeseriesService()).thenReturn(timeseriesService);

        initMock();
    }

    @After
    public void after() {
        executor.destroy();
        node.destroy();
    }

    private TbAggLatestTelemetryNodeV2Configuration getConfigNode() {
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

        return config;
    }

    private void initMock() {
        DeviceId deviceA = new DeviceId(UUID.randomUUID());
        DeviceId deviceB = new DeviceId(UUID.randomUUID());
        EntityRelation relationA = new EntityRelation(assetId, deviceA, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(assetId, deviceB, EntityRelation.CONTAINS_TYPE);

        TsKvEntry deviceATemperature = new BasicTsKvEntry(System.currentTimeMillis(), new DoubleDataEntry("temperature", 42.0));
        TsKvEntry deviceBTemperature = new BasicTsKvEntry(System.currentTimeMillis(), new DoubleDataEntry("temperature", 44.0));

        Mockito.when(relationService.findByFromAndTypeAsync(tenantId, assetId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON))
                .thenReturn(Futures.immediateFuture(Arrays.asList(relationA, relationB)));
        Mockito.when(timeseriesService.findLatest(tenantId, deviceA, new HashSet<>(Arrays.asList("temperature"))))
                .thenReturn(Futures.immediateFuture(Arrays.asList(deviceATemperature)));
        Mockito.when(timeseriesService.findLatest(tenantId, deviceB, new HashSet<>(Arrays.asList("temperature"))))
                .thenReturn(Futures.immediateFuture(Arrays.asList(deviceBTemperature)));
    }

    private void checkMsg(TbMsg Msg, boolean checkSum) {
        Assert.assertNotNull(Msg);
        Assert.assertNotNull(Msg.getData());
        ObjectNode objectNode = (ObjectNode) JacksonUtil.toJsonNode(Msg.getData());
        Assert.assertEquals(43.0, objectNode.get("latestAvgTemperature").asDouble(), 0.0);
        Assert.assertEquals(2, objectNode.get("deviceCount").asInt());

        if (checkSum) {
            //check filtered
            Assert.assertTrue(objectNode.has("sumTemperature"));
            Assert.assertEquals(44, objectNode.get("sumTemperature").asInt());
        }
    }

    @Test
    public void testSimpleAggregationWithoutFilter() throws TbNodeException, ExecutionException, InterruptedException {
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(getConfigNode())));

        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), assetId, new TbMsgMetaData(), JacksonUtil.toString(JacksonUtil.newObjectNode()));
        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, Mockito.timeout(5000).times(1)).enqueueForTellNext(captor.capture(), eq(SUCCESS));

        checkMsg(captor.getValue(), false);
    }

    @Test
    public void testSimpleAggregationWithFilter() throws TbNodeException, ExecutionException, InterruptedException {

        TbAggLatestTelemetryNodeV2Configuration config = getConfigNode();
        List<AggLatestMapping> aggMappings = config.getAggMappings();

        AggLatestMapping sumMapping = new AggLatestMapping();
        sumMapping.setSource("temperature");
        sumMapping.setSourceScope("LATEST_TELEMETRY");
        sumMapping.setAggFunction(MathFunction.SUM);
        sumMapping.setDefaultValue(0);
        sumMapping.setTarget("sumTemperature");

        AggLatestMappingFilter filter = new AggLatestMappingFilter();
        filter.setLatestTsKeyNames(Collections.singletonList("temperature"));
        filter.setScriptLang(ScriptLanguage.TBEL);
        filter.setTbelFilterFunction(FILTER_FUNCTION);
        sumMapping.setFilter(filter);

        aggMappings.add(sumMapping);
        config.setAggMappings(aggMappings);

        //Mock for filter
        when(peCtx.createAttributesScriptEngine(ScriptLanguage.TBEL, FILTER_FUNCTION)).thenReturn(scriptEngine);
        when(scriptEngine.executeAttributesFilterAsync(ArgumentMatchers.anyMap())).then(
                (Answer<ListenableFuture<Boolean>>) invocation -> {
                    Map<String, BasicTsKvEntry> attributes = (Map<String, BasicTsKvEntry>) (invocation.getArguments())[0];
                    if (attributes.containsKey("temperature")) {
                        try {
                            double temperature = attributes.get("temperature").getDoubleValue().get();
                            return Futures.immediateFuture(temperature > 42);
                        } catch (NumberFormatException e) {
                            return Futures.immediateFuture(false);
                        }
                    }
                    return Futures.immediateFuture(false);
                }
        );

        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), assetId, new TbMsgMetaData(), JacksonUtil.toString(JacksonUtil.newObjectNode()));
        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, Mockito.timeout(5000).times(1)).enqueueForTellNext(captor.capture(), eq(SUCCESS));

        checkMsg(captor.getValue(), true);
    }

    @Test
    public void testSimpleAggregationWithDeduplication() throws TbNodeException, ExecutionException, InterruptedException {
        int countMsg = 5;
        long deduplicationInSec = 60;
        TbAggLatestTelemetryNodeV2Configuration config = getConfigNode();
        config.setDeduplicationInSec(deduplicationInSec); //delay 60 sec
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        //Mock for tellSelf
        doAnswer((Answer<TbMsg>) invocationOnMock -> {
            String type = (String) (invocationOnMock.getArguments())[1];
            EntityId originator = (EntityId) (invocationOnMock.getArguments())[2];
            TbMsgMetaData metaData = (TbMsgMetaData) (invocationOnMock.getArguments())[4];
            String data = (String) (invocationOnMock.getArguments())[5];
            return TbMsg.newMsg(type, originator, metaData.copy(), data);
        }).when(ctx).newMsg(ArgumentMatchers.isNull(), eq(TB_AGG_LATEST_NODE_MSG), ArgumentMatchers.nullable(EntityId.class),
                any(), ArgumentMatchers.any(TbMsgMetaData.class), anyString());

        doAnswer((Answer<Void>) invocation -> {
            TbMsg msg = (TbMsg) (invocation.getArguments())[0];
            node.onMsg(ctx, msg);
            return null;
        }).when(ctx).tellSelf(any(TbMsg.class), anyLong());

        //create Msg
        for (int count = 0; count < countMsg; count++) {
            TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), assetId, new TbMsgMetaData(), JacksonUtil.toString(JacksonUtil.newObjectNode()));
            node.onMsg(ctx, msg);
        }

        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsg> captorForDelayedMsg = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, Mockito.timeout(5000).times(countMsg)).enqueueForTellNext(captor.capture(), eq(SUCCESS));
        verify(ctx, Mockito.timeout(5000).times(countMsg - 1)).tellSelf(captorForDelayedMsg.capture(), anyLong());

        List<TbMsg> resultMsg = captor.getAllValues();
        List<TbMsg> delayedMsg = captorForDelayedMsg.getAllValues();

        Assert.assertNotNull(resultMsg);
        Assert.assertEquals(countMsg, resultMsg.size());

        Assert.assertNotNull(delayedMsg);
        Assert.assertEquals(countMsg - 1, delayedMsg.size());

        resultMsg.forEach(tbMsg -> checkMsg(tbMsg, false));

        //check delayed Msg
        delayedMsg.forEach(tbMsg -> {
            Assert.assertNotNull(tbMsg);
            Assert.assertEquals(TB_AGG_LATEST_NODE_MSG, tbMsg.getType());
        });
    }
}
