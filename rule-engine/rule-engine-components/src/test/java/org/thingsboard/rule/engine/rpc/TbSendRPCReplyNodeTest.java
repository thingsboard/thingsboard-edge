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
package org.thingsboard.rule.engine.rpc;

import com.google.common.util.concurrent.SettableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.api.RuleEngineRpcService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.dao.edge.EdgeEventService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TbSendRPCReplyNodeTest {

    private static final String DUMMY_SERVICE_ID = "testServiceId";
    private static final int DUMMY_REQUEST_ID = 0;
    private static final UUID DUMMY_SESSION_ID = UUID.randomUUID();
    private static final String DUMMY_DATA = "{\"key\":\"value\"}";

    TbSendRPCReplyNode node;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final DeviceId deviceId = new DeviceId(UUID.randomUUID());

    @Mock
    private TbContext ctx;

    @Mock
    private RuleEngineRpcService rpcService;

    @Mock
    private EdgeEventService edgeEventService;

    @Mock
    private ListeningExecutor listeningExecutor;

    @Before
    public void setUp() throws TbNodeException {
        node = new TbSendRPCReplyNode();
        TbSendRpcReplyNodeConfiguration config = new TbSendRpcReplyNodeConfiguration().defaultConfiguration();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
    }

    @Test
    public void sendReplyToTransport() {
        Mockito.when(ctx.getRpcService()).thenReturn(rpcService);


        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, getDefaultMetadata(),
                TbMsgDataType.JSON, DUMMY_DATA, null, null);

        node.onMsg(ctx, msg);

        verify(rpcService).sendRpcReplyToDevice(DUMMY_SERVICE_ID, DUMMY_SESSION_ID, DUMMY_REQUEST_ID, DUMMY_DATA);
        verify(edgeEventService, never()).saveAsync(any());
    }

    @Test
    public void sendReplyToEdgeQueue() {
        Mockito.when(ctx.getTenantId()).thenReturn(tenantId);
        Mockito.when(ctx.getEdgeEventService()).thenReturn(edgeEventService);
        Mockito.when(edgeEventService.saveAsync(any())).thenReturn(SettableFuture.create());
        Mockito.when(ctx.getDbCallbackExecutor()).thenReturn(listeningExecutor);

        TbMsgMetaData defaultMetadata = getDefaultMetadata();
        defaultMetadata.putValue(DataConstants.EDGE_ID, UUID.randomUUID().toString());
        defaultMetadata.putValue(DataConstants.DEVICE_ID, UUID.randomUUID().toString());
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, defaultMetadata,
                TbMsgDataType.JSON, DUMMY_DATA, null, null);

        node.onMsg(ctx, msg);

        verify(edgeEventService).saveAsync(any());
        verify(rpcService, never()).sendRpcReplyToDevice(DUMMY_SERVICE_ID, DUMMY_SESSION_ID, DUMMY_REQUEST_ID, DUMMY_DATA);
    }

    private TbMsgMetaData getDefaultMetadata() {
        TbSendRpcReplyNodeConfiguration config = new TbSendRpcReplyNodeConfiguration().defaultConfiguration();
        TbMsgMetaData metadata = new TbMsgMetaData();
        metadata.putValue(config.getServiceIdMetaDataAttribute(), DUMMY_SERVICE_ID);
        metadata.putValue(config.getSessionIdMetaDataAttribute(), DUMMY_SESSION_ID.toString());
        metadata.putValue(config.getRequestIdMetaDataAttribute(), Integer.toString(DUMMY_REQUEST_ID));
        return metadata;
    }
}
