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
package org.thingsboard.rule.engine.transform;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.data.RelationsQuery;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TbDuplicateMsgToRelatedNodeTest {

    private final DeviceId ORIGINATOR_ID = new DeviceId(UUID.fromString("61e68586-466f-41e3-abca-9457b80da8d6"));
    private final TenantId TENANT_ID = new TenantId(UUID.fromString("9cc6c3c8-b90f-4d00-bfb2-fd38ccf79f63"));

    private final ListeningExecutor dbCallbackExecutor = new TestDbCallbackExecutor();

    private TbDuplicateMsgToRelatedNode node;
    private TbDuplicateMsgToRelatedNodeConfiguration config;

    @Mock
    private TbContext ctxMock;
    @Mock
    private RelationService relationServiceMock;

    @BeforeEach
    void setUp() {
        node = new TbDuplicateMsgToRelatedNode();
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    public void givenDefaultConfig_whenInit_thenOK() throws TbNodeException {
        // GIVEN-WHEN
        init();

        // THEN
        assertThat(config.getRelationsQuery()).isEqualTo(getDefaultRelationQuery());
    }

    @Test
    public void givenConfigWithUnspecifiedRelationQuery_whenInit_thenThrowException() {
        // GIVEN-WHEN
        var configuration = new TbDuplicateMsgToRelatedNodeConfiguration().defaultConfiguration();
        configuration.setRelationsQuery(null);
        assertThatThrownBy(() -> initWithConfig(configuration))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Relation query should be specified!");
    }

    @Test
    public void givenDefaultConfig_whenOnMsg_thenDuplicateToRelatedEntities() throws TbNodeException {
        // GIVEN
        init();

        var msg = getTbMsg();

        EntityId firstRelatedEntityId = new AssetId(UUID.randomUUID());
        var firstRelation = new EntityRelation();
        firstRelation.setFrom(ORIGINATOR_ID);
        firstRelation.setTo(firstRelatedEntityId);
        firstRelation.setTypeGroup(RelationTypeGroup.COMMON);
        firstRelation.setType(EntityRelation.CONTAINS_TYPE);

        EntityId secondRelatedEntityId = new AssetId(UUID.randomUUID());
        var secondRelation = new EntityRelation();
        firstRelation.setFrom(ORIGINATOR_ID);
        firstRelation.setTo(secondRelatedEntityId);
        firstRelation.setTypeGroup(RelationTypeGroup.COMMON);
        firstRelation.setType(EntityRelation.CONTAINS_TYPE);

        var relationList = List.of(firstRelation, secondRelation);

        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);

        var entityRelationsQuery = buildQuery(config.getRelationsQuery());
        when(relationServiceMock.findByQuery(
                eq(TENANT_ID), eq(entityRelationsQuery)))
                .thenReturn(Futures.immediateFuture(relationList));

        doAnswer((Answer<TbMsg>) invocationOnMock -> {
            String queueName = (String) (invocationOnMock.getArguments())[0];
            String type = (String) (invocationOnMock.getArguments())[1];
            EntityId entityId = (EntityId) (invocationOnMock.getArguments())[2];
            CustomerId customerId = (CustomerId) (invocationOnMock.getArguments())[3];
            TbMsgMetaData metaData = (TbMsgMetaData) (invocationOnMock.getArguments())[4];
            String data = (String) (invocationOnMock.getArguments())[5];
            return TbMsg.newMsg(queueName, type, entityId, customerId, metaData.copy(), data);
        }).when(ctxMock).newMsg(
                eq(msg.getQueueName()),
                eq(msg.getType()),
                nullable(EntityId.class),
                nullable(CustomerId.class),
                eq(msg.getMetaData()),
                eq(msg.getData())
        );

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(relationServiceMock)
                .findByQuery(eq(TENANT_ID), eq(entityRelationsQuery));
        verify(ctxMock, never()).transformMsgOriginator(any(TbMsg.class), any(EntityId.class));
        verify(ctxMock, never()).tellFailure(any(), any(Throwable.class));

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> onSuccessCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> onFailureCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(ctxMock, times(relationList.size())).enqueueForTellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS), onSuccessCaptor.capture(), onFailureCaptor.capture());

        for (Runnable successCaptor : onSuccessCaptor.getAllValues()) {
            successCaptor.run();
        }

        verify(ctxMock).ack(msg);

        List<TbMsg> allValues = newMsgCaptor.getAllValues();
        IntStream.range(0, allValues.size()).forEach(i -> {
            TbMsg newMsg = allValues.get(i);
            assertThat(newMsg).isNotNull();
            assertThat(newMsg).isNotSameAs(msg);
            assertThat(newMsg.getType()).isSameAs(msg.getType());
            assertThat(newMsg.getData()).isSameAs(msg.getData());
            assertThat(newMsg.getMetaData()).isEqualTo(msg.getMetaData());
            assertThat(newMsg.getOriginator()).isSameAs(relationList.get(i).getTo());
        });
    }

    @Test
    public void givenDefaultConfig_whenOnMsg_thenOneRelatedEntityFound() throws TbNodeException {
        init();

        var msg = getTbMsg();

        EntityId relatedEntityId = new AssetId(UUID.randomUUID());
        var relation = new EntityRelation();
        relation.setFrom(ORIGINATOR_ID);
        relation.setTo(relatedEntityId);
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        relation.setType(EntityRelation.CONTAINS_TYPE);

        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);

        var entityRelationsQuery = buildQuery(config.getRelationsQuery());
        when(relationServiceMock.findByQuery(
                eq(TENANT_ID), eq(entityRelationsQuery)))
                .thenReturn(Futures.immediateFuture(List.of(relation)));

        doAnswer((Answer<TbMsg>) invocationOnMock -> {
            TbMsg tbMsg = (TbMsg) (invocationOnMock.getArguments())[0];
            EntityId originator = (EntityId) (invocationOnMock.getArguments())[1];
            return TbMsg.transformMsgOriginator(tbMsg, originator);
        }).when(ctxMock).transformMsgOriginator(
                eq(msg),
                eq(relatedEntityId));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(relationServiceMock)
                .findByQuery(eq(TENANT_ID), eq(entityRelationsQuery));
        verify(ctxMock, never()).newMsg(anyString(),
                anyString(),
                any(EntityId.class),
                any(CustomerId.class),
                any(TbMsgMetaData.class),
                anyString());
        verify(ctxMock, never()).tellFailure(any(), any(Throwable.class));
        verify(ctxMock, never()).enqueueForTellNext(any(), eq(TbNodeConnectionType.SUCCESS), any(), any());
        verify(ctxMock, never()).ack(any());

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock).tellSuccess(newMsgCaptor.capture());

        var actualMsg = newMsgCaptor.getValue();

        assertThat(actualMsg).isNotNull();
        assertThat(actualMsg).isNotSameAs(msg);
        assertThat(actualMsg.getType()).isSameAs(msg.getType());
        assertThat(actualMsg.getData()).isSameAs(msg.getData());
        assertThat(actualMsg.getMetaData()).isEqualTo(msg.getMetaData());
        assertThat(actualMsg.getOriginator()).isSameAs(relatedEntityId);
    }

    @Test
    public void givenDefaultConfig_whenOnMsg_thenNoRelatedEntitiesFound() throws TbNodeException {
        init();

        var msg = getTbMsg();

        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);

        var entityRelationsQuery = buildQuery(config.getRelationsQuery());
        when(relationServiceMock.findByQuery(
                eq(TENANT_ID), eq(entityRelationsQuery)))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(relationServiceMock)
                .findByQuery(eq(TENANT_ID), eq(entityRelationsQuery));
        verify(ctxMock, never()).newMsg(anyString(),
                anyString(),
                any(EntityId.class),
                any(CustomerId.class),
                any(TbMsgMetaData.class),
                anyString());
        verify(ctxMock, never()).transformMsgOriginator(any(TbMsg.class), any(EntityId.class));
        verify(ctxMock, never()).enqueueForTellNext(any(), eq(TbNodeConnectionType.SUCCESS), any(), any());
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, never()).ack(msg);

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);

        verify(ctxMock).tellFailure(eq(msg), throwableCaptor.capture());

        String expectedExceptionMessage = "Message or messages list are empty!";

        Throwable actualThrowable = throwableCaptor.getValue();
        assertInstanceOf(RuntimeException.class, actualThrowable);
        assertThat(actualThrowable.getMessage()).isEqualTo(expectedExceptionMessage);
    }


    private void init() throws TbNodeException {
        initWithConfig(new TbDuplicateMsgToRelatedNodeConfiguration().defaultConfiguration());
    }

    private void initWithConfig(TbDuplicateMsgToRelatedNodeConfiguration configuration) throws TbNodeException {
        config = configuration;
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);
    }

    private TbMsg getTbMsg() {
        return TbMsg.newMsg(
                TbMsgType.POST_TELEMETRY_REQUEST, ORIGINATOR_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
    }

    private RelationsQuery getDefaultRelationQuery() {
        var relationsQuery = new RelationsQuery();
        relationsQuery.setDirection(EntitySearchDirection.FROM);
        relationsQuery.setMaxLevel(1);
        var entityTypeFilter = new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.emptyList());
        relationsQuery.setFilters(Collections.singletonList(entityTypeFilter));
        return relationsQuery;
    }

    private EntityRelationsQuery buildQuery(RelationsQuery relationsQuery) {
        var query = new EntityRelationsQuery();
        var parameters = new RelationsSearchParameters(
                ORIGINATOR_ID,
                relationsQuery.getDirection(),
                relationsQuery.getMaxLevel(),
                relationsQuery.isFetchLastLevelOnly()
        );
        query.setParameters(parameters);
        query.setFilters(relationsQuery.getFilters());
        return query;
    }

}
