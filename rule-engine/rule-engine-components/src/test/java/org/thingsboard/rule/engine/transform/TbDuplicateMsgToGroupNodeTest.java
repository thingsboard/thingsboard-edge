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
import org.thingsboard.rule.engine.api.TbPeContext;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.group.EntityGroupService;

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
class TbDuplicateMsgToGroupNodeTest {

    private final DeviceId ORIGINATOR_ID = new DeviceId(UUID.fromString("b0b69592-ae0e-4496-a5c7-b4ef81a4461b"));
    private final TenantId TENANT_ID = new TenantId(UUID.fromString("ffce9463-8b23-429b-9c0f-322ff12c2cc3"));

    private final ListeningExecutor dbCallbackExecutor = new TestDbCallbackExecutor();

    private TbDuplicateMsgToGroupNode node;
    private TbDuplicateMsgToGroupNodeConfiguration config;

    @Mock
    private TbContext ctxMock;
    @Mock
    private TbPeContext peCtxMock;
    @Mock
    private EntityGroupService entityGroupServiceMock;

    @BeforeEach
    void setUp() {
        node = new TbDuplicateMsgToGroupNode();
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
        assertThat(config.isEntityGroupIsMessageOriginator()).isEqualTo(true);
        assertThat(config.getEntityGroupId()).isEqualTo(null);
        assertThat(config.getGroupOwnerId()).isEqualTo(null);
    }

    @Test
    public void givenConfigWithUnspecifiedEntityGroupId_whenInit_thenThrowException() {
        // GIVEN-WHEN
        var configuration = new TbDuplicateMsgToGroupNodeConfiguration().defaultConfiguration();
        configuration.setEntityGroupIsMessageOriginator(false);

        assertThatThrownBy(() -> initWithConfig(configuration))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("EntityGroupId should be specified!");
    }

    @Test
    public void givenDefaultConfig_whenOnMsg_thenDuplicateToGroupEntities() throws TbNodeException {
        // GIVEN
        init();

        var originator = new EntityGroupId(UUID.randomUUID());
        var msg = getTbMsg(originator);

        EntityId firstUserId = new UserId(UUID.randomUUID());
        EntityId secondUserId = new UserId(UUID.randomUUID());
        var groupUserIdsList = List.of(firstUserId, secondUserId);

        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getPeContext()).thenReturn(peCtxMock);
        when(peCtxMock.getEntityGroupService()).thenReturn(entityGroupServiceMock);

        when(entityGroupServiceMock.findAllEntityIdsAsync(
                eq(TENANT_ID), eq(originator), eq(new PageLink(Integer.MAX_VALUE))))
                .thenReturn(Futures.immediateFuture(groupUserIdsList));

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
        verify(entityGroupServiceMock)
                .findAllEntityIdsAsync(eq(TENANT_ID), eq(originator), eq(new PageLink(Integer.MAX_VALUE)));
        verify(ctxMock, never()).transformMsgOriginator(any(TbMsg.class), any(EntityId.class));
        verify(ctxMock, never()).tellFailure(any(), any(Throwable.class));

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> onSuccessCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> onFailureCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(ctxMock, times(groupUserIdsList.size())).enqueueForTellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS), onSuccessCaptor.capture(), onFailureCaptor.capture());
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
            assertThat(newMsg.getOriginator()).isSameAs(groupUserIdsList.get(i));
        });
    }

    @Test
    public void givenDefaultConfig_whenOnMsg_thenMsgOriginatorIsNotAnEntityGroup() throws TbNodeException {
        // GIVEN
        init();

        var originator = new DeviceId(UUID.randomUUID());
        var msg = getTbMsg(originator);

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getPeContext()).thenReturn(peCtxMock);
        when(peCtxMock.getEntityGroupService()).thenReturn(entityGroupServiceMock);

        // WHEN
        assertThatThrownBy(() -> node.onMsg(ctxMock, msg))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Message originator is not an entity group!");

        // THEN
        verify(entityGroupServiceMock, never())
                .findAllEntityIdsAsync(any(TenantId.class), any(), eq(new PageLink(Integer.MAX_VALUE)));
        verify(ctxMock, never()).ack(any());
        verify(ctxMock, never()).tellFailure(any(), any(Throwable.class));
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, never()).enqueueForTellNext(any(), eq(TbNodeConnectionType.SUCCESS), any(), any());
    }

    @Test
    public void givenEntityGroupIdSpecifiedInConfig_whenOnMsg_thenGroupIsFoundWithOneEntity() throws TbNodeException {
        // GIVEN
        var entityGroupId = new EntityGroupId(UUID.randomUUID());

        var configuration = new TbDuplicateMsgToGroupNodeConfiguration().defaultConfiguration();
        configuration.setEntityGroupIsMessageOriginator(false);
        configuration.setEntityGroupId(entityGroupId);
        initWithConfig(configuration);

        var msg = getTbMsg();

        EntityId userId = new UserId(UUID.randomUUID());

        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getPeContext()).thenReturn(peCtxMock);
        when(peCtxMock.getEntityGroupService()).thenReturn(entityGroupServiceMock);

        when(entityGroupServiceMock.findAllEntityIdsAsync(
                eq(TENANT_ID), eq(entityGroupId), eq(new PageLink(Integer.MAX_VALUE))))
                .thenReturn(Futures.immediateFuture(List.of(userId)));

        doAnswer((Answer<TbMsg>) invocationOnMock -> {
            TbMsg tbMsg = (TbMsg) (invocationOnMock.getArguments())[0];
            EntityId originator = (EntityId) (invocationOnMock.getArguments())[1];
            return TbMsg.transformMsgOriginator(tbMsg, originator);
        }).when(ctxMock).transformMsgOriginator(
                eq(msg),
                eq(userId));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(entityGroupServiceMock)
                .findAllEntityIdsAsync(eq(TENANT_ID), eq(entityGroupId), eq(new PageLink(Integer.MAX_VALUE)));
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
        assertThat(actualMsg.getOriginator()).isSameAs(userId);
    }

    @Test
    public void givenDefaultConfig_whenOnMsg_thenGroupIsFoundWithNoEntitiesInside() throws TbNodeException {
        // GIVEN
        init();

        var originator = new EntityGroupId(UUID.randomUUID());
        var msg = getTbMsg(originator);

        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getPeContext()).thenReturn(peCtxMock);
        when(peCtxMock.getEntityGroupService()).thenReturn(entityGroupServiceMock);

        when(entityGroupServiceMock.findAllEntityIdsAsync(
                eq(TENANT_ID), eq(originator), eq(new PageLink(Integer.MAX_VALUE))))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(entityGroupServiceMock)
                .findAllEntityIdsAsync(eq(TENANT_ID), eq(originator), eq(new PageLink(Integer.MAX_VALUE)));
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
        initWithConfig(new TbDuplicateMsgToGroupNodeConfiguration().defaultConfiguration());
    }

    private void initWithConfig(TbDuplicateMsgToGroupNodeConfiguration configuration) throws TbNodeException {
        config = configuration;
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);
    }

    private TbMsg getTbMsg() {
        return getTbMsg(ORIGINATOR_ID);
    }

    private static TbMsg getTbMsg(EntityId originator) {
        return TbMsg.newMsg(
                TbMsgType.POST_TELEMETRY_REQUEST, originator, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
    }

}
