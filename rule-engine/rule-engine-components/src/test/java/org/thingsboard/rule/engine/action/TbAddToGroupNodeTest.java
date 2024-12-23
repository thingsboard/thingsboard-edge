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
package org.thingsboard.rule.engine.action;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbPeContext;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.group.EntityGroupService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TbAddToGroupNodeTest {

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("b744dd76-79ea-442c-a7b8-c06c7f568488"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("48641148-1f52-4d9e-ad74-238515365df5"));
    private final EntityGroupId ENTITY_GROUP_ID = new EntityGroupId(UUID.fromString("dc91a98a-055c-4493-aae9-2de393f4f5ab"));
    private final ListeningExecutor dbCallbackExecutor = new TestDbCallbackExecutor();

    private TbAddToGroupNode node;
    private TbAddToGroupConfiguration config;

    @Mock
    private TbContext ctxMock;
    @Mock
    private TbPeContext peContextMock;
    @Mock
    private EntityGroupService entityGroupServiceMock;
    @Mock
    private DeviceService deviceServiceMock;

    @BeforeEach
    public void setUp() {
        node = new TbAddToGroupNode();
        config = new TbAddToGroupConfiguration().defaultConfiguration();
    }

    @Test
    public void givenEntityWithoutEntityGroup_whenOnMsg_thenAddToExistingGroup() throws TbNodeException {
        config.setGroupNamePattern("${groupName}");
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        initMocks();
        when(entityGroupServiceMock.findEntityGroupByTypeAndNameAsync(any(), any(), any(), any()))
                .thenReturn(Futures.immediateFuture(Optional.of(new EntityGroup(ENTITY_GROUP_ID))));

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(new TbMsgMetaData(Map.of("groupName", "Device Group")))
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        verify(peContextMock).getOwner(TENANT_ID, DEVICE_ID);
        verify(entityGroupServiceMock).findEntityGroupByTypeAndNameAsync(TENANT_ID, TENANT_ID, EntityType.DEVICE, "Device Group");
        verify(entityGroupServiceMock).addEntityToEntityGroup(TENANT_ID, ENTITY_GROUP_ID, DEVICE_ID);
        verify(ctxMock).tellNext(msg, TbNodeConnectionType.SUCCESS);
        verifyNoMoreInteractions(ctxMock, peContextMock, entityGroupServiceMock);
    }

    @Test
    public void givenEntityAndEntityGroupNotExistAndCreateGroupIfNotExistsIsTrue_whenOnMsg_thenCreateAndAddEntityToNewGroup() throws TbNodeException {
        config.setGroupNamePattern("${groupName}");
        config.setCreateGroupIfNotExists(true);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        initMocks();
        when(entityGroupServiceMock.findEntityGroupByTypeAndNameAsync(any(), any(), any(), any()))
                .thenReturn(Futures.immediateFuture(Optional.empty()));
        when(entityGroupServiceMock.saveEntityGroup(any(), any(), any())).thenReturn(new EntityGroup(ENTITY_GROUP_ID));

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(new TbMsgMetaData(Map.of("groupName", "Another Device Group")))
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        verify(peContextMock).getOwner(TENANT_ID, DEVICE_ID);
        verify(entityGroupServiceMock).findEntityGroupByTypeAndNameAsync(TENANT_ID, TENANT_ID, EntityType.DEVICE, "Another Device Group");
        EntityGroup newEntityGroup = new EntityGroup();
        newEntityGroup.setName("Another Device Group");
        newEntityGroup.setType(EntityType.DEVICE);
        verify(entityGroupServiceMock).saveEntityGroup(TENANT_ID, TENANT_ID, newEntityGroup);
        verify(entityGroupServiceMock).addEntityToEntityGroup(TENANT_ID, ENTITY_GROUP_ID, DEVICE_ID);
        verify(ctxMock).tellNext(msg, TbNodeConnectionType.SUCCESS);
        verifyNoMoreInteractions(ctxMock, peContextMock, entityGroupServiceMock);
    }

    @Test
    void givenEntityWithEntityGroupAndRemoveFromCurrentGroups_whenOnMsg_thenAddToExistingGroupAndRemoveFromCurrentGroups() throws TbNodeException {
        EntityGroupId currentEntityGroupId = ENTITY_GROUP_ID;
        EntityGroupId newEntityGroupId = new EntityGroupId(UUID.fromString("0aba647a-c081-42da-83eb-2fe9dc920bcb"));
        Device device = new Device(DEVICE_ID);
        device.setTenantId(TENANT_ID);
        config.setGroupNamePattern("${groupName}");
        config.setRemoveFromCurrentGroups(true);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        initMocks();
        when(entityGroupServiceMock.findEntityGroupByTypeAndNameAsync(any(), any(), any(), any()))
                .thenReturn(Futures.immediateFuture(Optional.of(new EntityGroup(newEntityGroupId))));
        when(entityGroupServiceMock.findEntityGroupsForEntityAsync(any(), any()))
                .thenReturn(Futures.immediateFuture(List.of(currentEntityGroupId)));
        when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
        when(deviceServiceMock.findDeviceById(any(), any())).thenReturn(device);
        when(entityGroupServiceMock.findEntityGroupByTypeAndNameAsync(any(), any(), any(), eq(EntityGroup.GROUP_ALL_NAME)))
                .thenReturn(Futures.immediateFuture(Optional.of(new EntityGroup(currentEntityGroupId))));

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(new TbMsgMetaData(Map.of("groupName", "Device Group")))
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        verify(peContextMock).getOwner(TENANT_ID, DEVICE_ID);
        verify(entityGroupServiceMock).findEntityGroupByTypeAndNameAsync(TENANT_ID, TENANT_ID, EntityType.DEVICE, "Device Group");
        verify(entityGroupServiceMock).findEntityGroupsForEntityAsync(TENANT_ID, DEVICE_ID);
        verify(deviceServiceMock).findDeviceById(TENANT_ID, DEVICE_ID);
        verify(entityGroupServiceMock).findEntityGroupByTypeAndNameAsync(TENANT_ID, TENANT_ID, EntityType.DEVICE, EntityGroup.GROUP_ALL_NAME);
        verify(entityGroupServiceMock).addEntityToEntityGroup(TENANT_ID, newEntityGroupId, DEVICE_ID);
        verify(ctxMock).tellNext(msg, TbNodeConnectionType.SUCCESS);
        verifyNoMoreInteractions(ctxMock, peContextMock, entityGroupServiceMock);
    }

    @Test
    public void givenDefaultConfig_whenInit_thenOk() {
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        assertThatNoException().isThrownBy(() -> node.init(ctxMock, configuration));
    }

    private void initMocks() {
        when(ctxMock.getPeContext()).thenReturn(peContextMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(peContextMock.getOwner(any(), any())).thenReturn(TENANT_ID);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        when(peContextMock.getEntityGroupService()).thenReturn(entityGroupServiceMock);
    }
}
