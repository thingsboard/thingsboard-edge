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
package org.thingsboard.rule.engine.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class TbCheckMessageNodeTest {

    private static final DeviceId DEVICE_ID = new DeviceId(UUID.randomUUID());
    private static final TbMsg EMPTY_POST_ATTRIBUTES_MSG = TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);

    private TbCheckMessageNode node;

    private TbContext ctx;

    @BeforeEach
    void setUp() {
        ctx = mock(TbContext.class);
        node = new TbCheckMessageNode();
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    void givenDefaultConfig_whenOnMsg_then_True() throws TbNodeException {
        // GIVEN
        var configuration = new TbCheckMessageNodeConfiguration().defaultConfiguration();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(configuration)));

        // WHEN
        node.onMsg(ctx, EMPTY_POST_ATTRIBUTES_MSG);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(EMPTY_POST_ATTRIBUTES_MSG);
    }

    @Test
    void givenCustomConfigWithoutCheckAllKeysAndWithEmptyLists_whenOnMsg_then_False() throws TbNodeException {
        // GIVEN
        var configuration = new TbCheckMessageNodeConfiguration().defaultConfiguration();
        configuration.setCheckAllKeys(false);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(configuration)));

        // WHEN
        node.onMsg(ctx, EMPTY_POST_ATTRIBUTES_MSG);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(EMPTY_POST_ATTRIBUTES_MSG);
    }

    @Test
    void givenCustomConfigWithCheckAllKeys_whenOnMsg_then_True() throws TbNodeException {
        // GIVEN
        var configuration = new TbCheckMessageNodeConfiguration().defaultConfiguration();
        configuration.setMessageNames(List.of("temperature-0"));
        configuration.setMetadataNames(List.of("deviceName", "deviceType", "ts"));
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(configuration)));

        TbMsg tbMsg = getTbMsg();

        // WHEN
        node.onMsg(ctx, tbMsg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(tbMsg);
    }

    @Test
    void givenCustomConfigWithCheckAllKeys_whenOnMsg_then_False() throws TbNodeException {
        // GIVEN
        var configuration = new TbCheckMessageNodeConfiguration().defaultConfiguration();
        configuration.setMessageNames(List.of("temperature-0", "temperature-1"));
        configuration.setMetadataNames(List.of("deviceName", "deviceType", "ts"));
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(configuration)));

        TbMsg tbMsg = getTbMsg();

        // WHEN
        node.onMsg(ctx, tbMsg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(tbMsg);
    }

    @Test
    void givenCustomConfigWithoutCheckAllKeys_whenOnMsg_then_True() throws TbNodeException {
        // GIVEN
        var configuration = new TbCheckMessageNodeConfiguration().defaultConfiguration();
        configuration.setMessageNames(List.of("temperature-0", "temperature-1"));
        configuration.setCheckAllKeys(false);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(configuration)));

        TbMsg tbMsg = getTbMsg();

        // WHEN
        node.onMsg(ctx, tbMsg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(tbMsg);
    }

    @Test
    void givenCustomConfigWithoutCheckAllKeysAndEmptyMsg_whenOnMsg_then_False() throws TbNodeException {
        // GIVEN
        var configuration = new TbCheckMessageNodeConfiguration().defaultConfiguration();
        configuration.setMessageNames(List.of("temperature-0", "temperature-1"));
        configuration.setCheckAllKeys(false);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(configuration)));

        TbMsg tbMsg = getTbMsg(true);

        // WHEN
        node.onMsg(ctx, tbMsg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(tbMsg);
    }

    private TbMsg getTbMsg() {
        return getTbMsg(false);
    }

    private TbMsg getTbMsg(boolean emptyData) {
        String data = emptyData ? TbMsg.EMPTY_JSON_OBJECT : "{\"temperature-0\": 25}";
        var metadata = new TbMsgMetaData();
        metadata.putValue(DataConstants.DEVICE_NAME, "Test Device");
        metadata.putValue(DataConstants.DEVICE_TYPE, DataConstants.DEFAULT_DEVICE_TYPE);
        metadata.putValue("ts", String.valueOf(System.currentTimeMillis()));
        return TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, DEVICE_ID, metadata, data);
    }

}
