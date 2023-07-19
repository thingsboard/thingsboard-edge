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
package org.thingsboard.rule.engine.action;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbPeContext;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TbCreateRelationNodeTest {

    private TbCreateRelationNode node;

    @Mock
    private TbContext ctx;
    @Mock
    private TbPeContext peCtx;
    @Mock
    private AssetService assetService;
    @Mock
    private ConverterService converterService;
    @Mock
    private RelationService relationService;

    private TbMsg msg;

    private RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
    private RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

    private ListeningExecutor dbExecutor;

    @Before
    public void before() {
        dbExecutor = new TestDbCallbackExecutor();
    }

    @Test
    public void testCreateNewRelation() throws TbNodeException {
        init(createRelationNodeConfig());

        DeviceId deviceId = new DeviceId(Uuids.timeBased());

        AssetId assetId = new AssetId(Uuids.timeBased());
        Asset asset = new Asset();
        asset.setId(assetId);

        when(assetService.findAssetByTenantIdAndName(any(), eq("AssetName"))).thenReturn(asset);
        when(assetService.findAssetByIdAsync(any(), eq(assetId))).thenReturn(Futures.immediateFuture(asset));

        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("name", "AssetName");
        metaData.putValue("type", "AssetType");
        msg = TbMsg.newMsg(TbMsgType.ENTITY_CREATED, deviceId, metaData, TbMsgDataType.JSON, TbMsg.EMPTY_JSON_OBJECT, ruleChainId, ruleNodeId);

        when(ctx.getRelationService().checkRelationAsync(any(), eq(assetId), eq(deviceId), eq(EntityRelation.CONTAINS_TYPE), eq(RelationTypeGroup.COMMON)))
                .thenReturn(Futures.immediateFuture(false));
        when(ctx.getRelationService().saveRelationAsync(any(), eq(new EntityRelation(assetId, deviceId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON))))
                .thenReturn(Futures.immediateFuture(true));

        node.onMsg(ctx, msg);
        verify(ctx).tellNext(msg, TbNodeConnectionType.SUCCESS);
    }

    @Test
    public void testCreateNewRelationWithConverter() throws TbNodeException {
        TbCreateRelationNodeConfiguration configuration = createRelationNodeConfig();
        configuration.setEntityType(EntityType.CONVERTER.name());
        init(configuration);

        DeviceId deviceId = new DeviceId(Uuids.timeBased());


        ConverterId converterId = new ConverterId(Uuids.timeBased());
        Converter converter = new Converter();
        converter.setId(converterId);

        when(converterService.findConverterByName(any(), eq("ConvName"))).thenReturn(Optional.of(converter));
        when(converterService.findConverterByIdAsync(any(), eq(converterId))).thenReturn(Futures.immediateFuture(converter));

        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("name", "ConvName");
        metaData.putValue("type", "ConvType");
        msg = TbMsg.newMsg(TbMsgType.ENTITY_CREATED, deviceId, metaData, TbMsgDataType.JSON, TbMsg.EMPTY_JSON_OBJECT, ruleChainId, ruleNodeId);

        when(ctx.getRelationService().checkRelationAsync(any(), eq(converterId), eq(deviceId), eq(EntityRelation.CONTAINS_TYPE), eq(RelationTypeGroup.COMMON)))
                .thenReturn(Futures.immediateFuture(false));
        when(ctx.getRelationService().saveRelationAsync(any(), eq(new EntityRelation(converterId, deviceId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON))))
                .thenReturn(Futures.immediateFuture(true));

        node.onMsg(ctx, msg);
        verify(ctx).tellNext(msg, TbNodeConnectionType.SUCCESS);
    }

    @Test
    public void testDeleteCurrentRelationsCreateNewRelation() throws TbNodeException {
        init(createRelationNodeConfigWithRemoveCurrentRelations());

        DeviceId deviceId = new DeviceId(Uuids.timeBased());

        AssetId assetId = new AssetId(Uuids.timeBased());
        Asset asset = new Asset();
        asset.setId(assetId);

        when(assetService.findAssetByTenantIdAndName(any(), eq("AssetName"))).thenReturn(asset);
        when(assetService.findAssetByIdAsync(any(), eq(assetId))).thenReturn(Futures.immediateFuture(asset));

        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("name", "AssetName");
        metaData.putValue("type", "AssetType");
        msg = TbMsg.newMsg(TbMsgType.ENTITY_CREATED, deviceId, metaData, TbMsgDataType.JSON, TbMsg.EMPTY_JSON_OBJECT, ruleChainId, ruleNodeId);

        EntityRelation relation = new EntityRelation();
        when(ctx.getRelationService().findByToAndTypeAsync(any(), eq(msg.getOriginator()), eq(EntityRelation.CONTAINS_TYPE), eq(RelationTypeGroup.COMMON)))
                .thenReturn(Futures.immediateFuture(Collections.singletonList(relation)));
        when(ctx.getRelationService().deleteRelationAsync(any(), eq(relation))).thenReturn(Futures.immediateFuture(true));
        when(ctx.getRelationService().checkRelationAsync(any(), eq(assetId), eq(deviceId), eq(EntityRelation.CONTAINS_TYPE), eq(RelationTypeGroup.COMMON)))
                .thenReturn(Futures.immediateFuture(false));
        when(ctx.getRelationService().saveRelationAsync(any(), eq(new EntityRelation(assetId, deviceId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON))))
                .thenReturn(Futures.immediateFuture(true));

        node.onMsg(ctx, msg);
        verify(ctx).tellNext(msg, TbNodeConnectionType.SUCCESS);
    }

    @Test
    public void testCreateNewRelationAndChangeOriginator() throws TbNodeException {
        init(createRelationNodeConfigWithChangeOriginator());

        DeviceId deviceId = new DeviceId(Uuids.timeBased());

        AssetId assetId = new AssetId(Uuids.timeBased());
        Asset asset = new Asset();
        asset.setId(assetId);

        when(assetService.findAssetByTenantIdAndName(any(), eq("AssetName"))).thenReturn(asset);
        when(assetService.findAssetByIdAsync(any(), eq(assetId))).thenReturn(Futures.immediateFuture(asset));

        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("name", "AssetName");
        metaData.putValue("type", "AssetType");
        msg = TbMsg.newMsg(TbMsgType.ENTITY_CREATED, deviceId, metaData, TbMsgDataType.JSON, TbMsg.EMPTY_JSON_OBJECT, ruleChainId, ruleNodeId);

        when(ctx.getRelationService().checkRelationAsync(any(), eq(assetId), eq(deviceId), eq(EntityRelation.CONTAINS_TYPE), eq(RelationTypeGroup.COMMON)))
                .thenReturn(Futures.immediateFuture(false));
        when(ctx.getRelationService().saveRelationAsync(any(), eq(new EntityRelation(assetId, deviceId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON))))
                .thenReturn(Futures.immediateFuture(true));

        node.onMsg(ctx, msg);
        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        verify(ctx).transformMsgOriginator(msgCaptor.capture(), originatorCaptor.capture());

        assertEquals(assetId, originatorCaptor.getValue());
    }

    public void init(TbCreateRelationNodeConfiguration configuration) throws TbNodeException {
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(configuration));

        when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);
        when(ctx.getRelationService()).thenReturn(relationService);
        when(ctx.getAssetService()).thenReturn(assetService);
        when(ctx.getPeContext()).thenReturn(peCtx);
        when(peCtx.getConverterService()).thenReturn(converterService);

        node = new TbCreateRelationNode();
        node.init(ctx, nodeConfiguration);
    }

    private TbCreateRelationNodeConfiguration createRelationNodeConfig() {
        TbCreateRelationNodeConfiguration configuration = new TbCreateRelationNodeConfiguration();
        configuration.setDirection(EntitySearchDirection.FROM.name());
        configuration.setRelationType(EntityRelation.CONTAINS_TYPE);
        configuration.setEntityCacheExpiration(300);
        configuration.setEntityType(EntityType.ASSET.name());
        configuration.setEntityNamePattern("${name}");
        configuration.setEntityTypePattern("${type}");
        configuration.setCreateEntityIfNotExists(false);
        configuration.setChangeOriginatorToRelatedEntity(false);
        configuration.setRemoveCurrentRelations(false);
        return configuration;
    }

    private TbCreateRelationNodeConfiguration createRelationNodeConfigWithRemoveCurrentRelations() {
        TbCreateRelationNodeConfiguration configuration = createRelationNodeConfig();
        configuration.setRemoveCurrentRelations(true);
        return configuration;
    }

    private TbCreateRelationNodeConfiguration createRelationNodeConfigWithChangeOriginator() {
        TbCreateRelationNodeConfiguration configuration = createRelationNodeConfig();
        configuration.setChangeOriginatorToRelatedEntity(true);
        return configuration;
    }
}
