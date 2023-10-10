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
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbPeContext;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
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
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TbDuplicateMsgToGroupByNameNodeTest {

    private static final DeviceId ORIGINATOR_ID = new DeviceId(UUID.randomUUID());
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());

    private static final ListeningExecutor dbCallbackExecutor = new TestDbCallbackExecutor();

    private TbDuplicateMsgToGroupByNameNode node;
    private TbDuplicateMsgToGroupByNameNodeConfiguration config;

    private TbContext ctxMock;
    private TbPeContext peCtxMock;
    private EntityGroupService entityGroupServiceMock;

    @BeforeEach
    void setUp() {
        ctxMock = mock(TbContext.class);
        peCtxMock = mock(TbPeContext.class);
        entityGroupServiceMock = mock(EntityGroupService.class);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getPeContext()).thenReturn(peCtxMock);
        when(peCtxMock.getEntityGroupService()).thenReturn(entityGroupServiceMock);

        node = new TbDuplicateMsgToGroupByNameNode();
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
        assertThat(config.getGroupType()).isEqualTo(EntityType.USER);
        assertThat(config.getGroupName()).isEqualTo(EntityGroup.GROUP_ALL_NAME);
        assertThat(config.isSearchEntityGroupForTenantOnly()).isEqualTo(false);
    }

    @Test
    public void givenConfigWithUnsupportedGroupType_whenInit_thenThrowException() throws TbNodeException {
        var configuration = new TbDuplicateMsgToGroupByNameNodeConfiguration().defaultConfiguration();
        for (var groupType : EntityType.values()) {
            if (groupType.isGroupEntityType()) {
                configuration.setGroupType(groupType);
                configuration.setGroupName(groupType.getNormalName());
                initWithConfig(configuration);
                assertThat(config.getGroupType()).isEqualTo(groupType);
                assertThat(config.getGroupName()).isEqualTo(groupType.getNormalName());
                assertThat(config.isSearchEntityGroupForTenantOnly()).isEqualTo(false);
            } else {
                var exception = assertThrows(IllegalArgumentException.class, () -> {
                    configuration.setGroupType(groupType);
                    configuration.setGroupName(groupType.getNormalName());
                    initWithConfig(configuration);
                });
                assertThat(exception.getMessage())
                        .isEqualTo("Entity Type :" + config.getGroupType() + " is not a group entity. " +
                                "Only " + EntityType.GROUP_ENTITY_TYPES + " types are allowed!");
            }
        }
    }

    @Test
    public void givenConfigWithEmptyName_whenInit_thenThrowException() {
        // GIVEN-WHEN
        var configuration = new TbDuplicateMsgToGroupByNameNodeConfiguration().defaultConfiguration();
        configuration.setGroupName("");
        var exception = assertThrows(IllegalArgumentException.class, () -> initWithConfig(configuration));
        assertThat(exception.getMessage())
                .isEqualTo("Group name should be specified!");
    }

    @Test
    public void givenDefaultConfig_whenOnMsg_thenDuplicateToGroupEntities() throws TbNodeException {
        // GIVEN
        init();

        var msg = getTbMsg();

        var ownerId = new CustomerId(UUID.randomUUID());

        var userGroupId = new EntityGroupId(UUID.randomUUID());

        var userGroup = new EntityGroup();
        userGroup.setId(userGroupId);
        userGroup.setName(config.getGroupName());
        userGroup.setType(config.getGroupType());
        userGroup.setOwnerId(ownerId);
        userGroup.setTenantId(TENANT_ID);

        EntityId firstUserId = new UserId(UUID.randomUUID());
        EntityId secondUserId = new UserId(UUID.randomUUID());
        var groupUserIdsList = List.of(firstUserId, secondUserId);

        when(peCtxMock.getOwner(TENANT_ID, ORIGINATOR_ID)).thenReturn(ownerId);

        when(entityGroupServiceMock.findEntityGroupByTypeAndName(
                eq(TENANT_ID), eq(ownerId), eq(config.getGroupType()), eq(config.getGroupName())))
                .thenReturn(Optional.of(userGroup));
        when(entityGroupServiceMock.findAllEntityIdsAsync(
                eq(TENANT_ID), eq(userGroupId), eq(new PageLink(Integer.MAX_VALUE))))
                .thenReturn(Futures.immediateFuture(groupUserIdsList));

        doAnswer((Answer<TbMsg>) invocationOnMock -> {
            String queueName = (String) (invocationOnMock.getArguments())[0];
            String type = (String) (invocationOnMock.getArguments())[1];
            EntityId originator = (EntityId) (invocationOnMock.getArguments())[2];
            CustomerId customerId = (CustomerId) (invocationOnMock.getArguments())[3];
            TbMsgMetaData metaData = (TbMsgMetaData) (invocationOnMock.getArguments())[4];
            String data = (String) (invocationOnMock.getArguments())[5];
            return TbMsg.newMsg(queueName, type, originator, customerId, metaData.copy(), data);
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
        verify(peCtxMock, times(1)).getOwner(any(), any());
        verify(entityGroupServiceMock, times(1))
                .findEntityGroupByTypeAndName(any(TenantId.class), any(EntityId.class), any(EntityType.class), anyString());
        verify(ctxMock, never()).transformMsgOriginator(any(TbMsg.class), any(EntityId.class));
        verify(ctxMock, never()).tellFailure(any(), any(Throwable.class));

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> onSuccessCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> onFailureCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(ctxMock, times(groupUserIdsList.size())).enqueueForTellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS), onSuccessCaptor.capture(), onFailureCaptor.capture());
        for (Runnable successCaptor : onSuccessCaptor.getAllValues()) {
            successCaptor.run();
        }
        verify(ctxMock, times(1)).ack(msg);
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
    public void givenDefaultConfig_whenOnMsg_thenGroupIsNotFound() throws TbNodeException {
        // GIVEN
        init();

        var msg = getTbMsg();
        var ownerId = new CustomerId(UUID.randomUUID());

        when(peCtxMock.getOwner(eq(TENANT_ID), eq(ORIGINATOR_ID))).thenReturn(ownerId);
        when(peCtxMock.getOwner(eq(TENANT_ID), eq(ownerId))).thenReturn(TENANT_ID);
        when(entityGroupServiceMock.findEntityGroupByTypeAndName(
                eq(TENANT_ID), any(), eq(config.getGroupType()), eq(config.getGroupName())))
                .thenReturn(Optional.empty());

        // WHEN
        var exception = assertThrows(RuntimeException.class, () -> node.onMsg(ctxMock, msg));

        // THEN
        verify(peCtxMock, times(2)).getOwner(any(), any());
        verify(entityGroupServiceMock, times(2))
                .findEntityGroupByTypeAndName(any(TenantId.class), any(EntityId.class), any(EntityType.class), anyString());
        assertThat(exception.getMessage())
                .isEqualTo(String.format("Can't find group with type: %s name: %s!", config.getGroupType(), config.getGroupName()));
        verify(ctxMock, never()).ack(any());
        verify(ctxMock, never()).tellFailure(any(), any(Throwable.class));
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, never()).enqueueForTellNext(any(), eq(TbNodeConnectionType.SUCCESS), any(), any());
    }

    @Test
    public void givenDefaultConfig_whenOnMsg_thenGroupIsFoundOnTenantLevelWithOneEntity() throws TbNodeException {
        // GIVEN
        init();

        var msg = getTbMsg();

        var ownerId = new CustomerId(UUID.randomUUID());

        var userGroupId = new EntityGroupId(UUID.randomUUID());

        var userGroup = new EntityGroup();
        userGroup.setId(userGroupId);
        userGroup.setName(config.getGroupName());
        userGroup.setType(config.getGroupType());
        userGroup.setOwnerId(ownerId);
        userGroup.setTenantId(TENANT_ID);

        EntityId userId = new UserId(UUID.randomUUID());

        when(peCtxMock.getOwner(eq(TENANT_ID), eq(ORIGINATOR_ID))).thenReturn(ownerId);
        when(peCtxMock.getOwner(eq(TENANT_ID), eq(ownerId))).thenReturn(TENANT_ID);

        when(entityGroupServiceMock.findEntityGroupByTypeAndName(
                eq(TENANT_ID), eq(ownerId), eq(config.getGroupType()), eq(config.getGroupName())))
                .thenReturn(Optional.empty());
        when(entityGroupServiceMock.findEntityGroupByTypeAndName(
                eq(TENANT_ID), eq(TENANT_ID), eq(config.getGroupType()), eq(config.getGroupName())))
                .thenReturn(Optional.of(userGroup));
        when(entityGroupServiceMock.findAllEntityIdsAsync(
                eq(TENANT_ID), eq(userGroupId), eq(new PageLink(Integer.MAX_VALUE))))
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
        verify(peCtxMock, times(2)).getOwner(any(), any());
        verify(entityGroupServiceMock, times(2))
                .findEntityGroupByTypeAndName(any(TenantId.class), any(EntityId.class), any(EntityType.class), anyString());
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
        verify(ctxMock, times(1)).tellSuccess(newMsgCaptor.capture());
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

        var msg = getTbMsg();

        var ownerId = new CustomerId(UUID.randomUUID());

        var userGroupId = new EntityGroupId(UUID.randomUUID());

        var userGroup = new EntityGroup();
        userGroup.setId(userGroupId);
        userGroup.setName(config.getGroupName());
        userGroup.setType(config.getGroupType());
        userGroup.setOwnerId(ownerId);
        userGroup.setTenantId(TENANT_ID);

        when(peCtxMock.getOwner(TENANT_ID, ORIGINATOR_ID)).thenReturn(ownerId);

        when(entityGroupServiceMock.findEntityGroupByTypeAndName(
                eq(TENANT_ID), eq(ownerId), eq(config.getGroupType()), eq(config.getGroupName())))
                .thenReturn(Optional.of(userGroup));
        when(entityGroupServiceMock.findAllEntityIdsAsync(
                eq(TENANT_ID), eq(userGroupId), eq(new PageLink(Integer.MAX_VALUE))))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(peCtxMock, times(1)).getOwner(any(), any());
        verify(entityGroupServiceMock, times(1))
                .findEntityGroupByTypeAndName(any(TenantId.class), any(EntityId.class), any(EntityType.class), anyString());
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
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctxMock, times(1)).tellFailure(newMsgCaptor.capture(), throwableCaptor.capture());
        var actualMsg = newMsgCaptor.getValue();
        assertThat(actualMsg).isNotNull();
        assertThat(actualMsg).isSameAs(msg);

        String expectedExceptionMessage = "Message or messages list are empty!";

        Throwable actualThrowable = throwableCaptor.getValue();
        assertInstanceOf(RuntimeException.class, actualThrowable);
        assertThat(actualThrowable.getMessage()).isEqualTo(expectedExceptionMessage);
    }

    @Test
    public void givenSearchOnlyOnTenantLevel_whenOnMsg_thenDuplicateToGroupEntities() throws TbNodeException {
        // GIVEN
        var configuration = new TbDuplicateMsgToGroupByNameNodeConfiguration().defaultConfiguration();
        configuration.setSearchEntityGroupForTenantOnly(true);
        initWithConfig(configuration);

        var msg = getTbMsg();

        var userGroupId = new EntityGroupId(UUID.randomUUID());

        var userGroup = new EntityGroup();
        userGroup.setId(userGroupId);
        userGroup.setName(config.getGroupName());
        userGroup.setType(config.getGroupType());
        userGroup.setOwnerId(TENANT_ID);
        userGroup.setTenantId(TENANT_ID);

        EntityId firstUserId = new UserId(UUID.randomUUID());
        EntityId secondUserId = new UserId(UUID.randomUUID());
        var groupUserIdsList = List.of(firstUserId, secondUserId);

        when(entityGroupServiceMock.findEntityGroupByTypeAndName(
                eq(TENANT_ID), eq(TENANT_ID), eq(config.getGroupType()), eq(config.getGroupName())))
                .thenReturn(Optional.of(userGroup));
        when(entityGroupServiceMock.findAllEntityIdsAsync(
                eq(TENANT_ID), eq(userGroupId), eq(new PageLink(Integer.MAX_VALUE))))
                .thenReturn(Futures.immediateFuture(groupUserIdsList));
        doAnswer((Answer<TbMsg>) invocationOnMock -> {
            String queueName = (String) (invocationOnMock.getArguments())[0];
            String type = (String) (invocationOnMock.getArguments())[1];
            EntityId originator = (EntityId) (invocationOnMock.getArguments())[2];
            CustomerId customerId = (CustomerId) (invocationOnMock.getArguments())[3];
            TbMsgMetaData metaData = (TbMsgMetaData) (invocationOnMock.getArguments())[4];
            String data = (String) (invocationOnMock.getArguments())[5];
            return TbMsg.newMsg(queueName, type, originator, customerId, metaData.copy(), data);
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
        verify(peCtxMock, never()).getOwner(any(), any());
        verify(entityGroupServiceMock, times(1))
                .findEntityGroupByTypeAndName(any(TenantId.class), any(EntityId.class), any(EntityType.class), anyString());
        verify(ctxMock, never()).transformMsgOriginator(any(TbMsg.class), any(EntityId.class));
        verify(ctxMock, never()).tellFailure(any(), any(Throwable.class));

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> onSuccessCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> onFailureCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(ctxMock, times(groupUserIdsList.size())).enqueueForTellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS), onSuccessCaptor.capture(), onFailureCaptor.capture());
        for (Runnable successCaptor : onSuccessCaptor.getAllValues()) {
            successCaptor.run();
        }
        verify(ctxMock, times(1)).ack(msg);
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

    private void init() throws TbNodeException {
        initWithConfig(new TbDuplicateMsgToGroupByNameNodeConfiguration().defaultConfiguration());
    }

    private void initWithConfig(TbDuplicateMsgToGroupByNameNodeConfiguration configuration) throws TbNodeException {
        config = configuration;
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);
    }

    private static TbMsg getTbMsg() {
        return TbMsg.newMsg(
                TbMsgType.POST_TELEMETRY_REQUEST, ORIGINATOR_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
    }

}
