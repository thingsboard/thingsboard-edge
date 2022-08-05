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
package org.thingsboard.rule.engine.telemetry;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
public class TbMsgDeleteAttributesTest {
    final ObjectMapper mapper = new ObjectMapper();

    DeviceId deviceId;
    TbMsgDeleteAttributes node;
    TbMsgDeleteAttributesConfiguration config;
    TbNodeConfiguration nodeConfiguration;
    TbContext ctx;
    TbMsgCallback callback;

    RuleEngineTelemetryService telemetryService;

    @BeforeEach
    void setUp() throws TbNodeException {
        deviceId = new DeviceId(UUID.randomUUID());
        callback = mock(TbMsgCallback.class);
        ctx = mock(TbContext.class);
        config = new TbMsgDeleteAttributesConfiguration().defaultConfiguration();
        config.setKeys(List.of("${TestAttribute_1}", "$[TestAttribute_2]", "$[TestAttribute_3]"));
        nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));
        node = spy(new TbMsgDeleteAttributes());
        node.init(ctx, nodeConfiguration);
        telemetryService = mock(RuleEngineTelemetryService.class);

        willReturn(telemetryService).given(ctx).getTelemetryService();
        willAnswer(invocation -> {
            TelemetryNodeCallback callBack = invocation.getArgument(4);
            callBack.onSuccess(null);
            return null;
        }).given(telemetryService).deleteAndNotify(
                any(), any(), anyString(), anyList(), any());
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    void givenDefaultConfig_whenInit_thenOK() {
        assertThat(node.config).isEqualTo(config);
    }

    @Test
    void givenDefaultConfig_whenInit_thenFail() {
        config.setKeys(Collections.emptyList());
        nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));
        assertThatThrownBy(() -> node.init(ctx, nodeConfiguration)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenDefaultConfig_whenVerify_thenOK() {
        TbMsgDeleteAttributesConfiguration defaultConfig = new TbMsgDeleteAttributesConfiguration().defaultConfiguration();
        assertThat(defaultConfig.getScope()).isEqualTo(DataConstants.SERVER_SCOPE);
        assertThat(defaultConfig.getKeys()).isEqualTo(Collections.emptyList());
    }

    @Test
    void givenMsg_whenOnMsg_thenVerifyOutput() throws Exception {
        final Map<String, String> mdMap = Map.of(
                "TestAttribute_1", "temperature",
                "city", "NY"
        );
        final TbMsgMetaData metaData = new TbMsgMetaData(mdMap);
        final String data = "{\"TestAttribute_2\": \"humidity\", \"TestAttribute_3\": \"voltage\"}";

        TbMsg msg = TbMsg.newMsg("POST_ATTRIBUTES_REQUEST", deviceId, metaData, data, callback);
        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());
        verify(telemetryService, times(1)).deleteAndNotify(any(), any(), anyString(), anyList(), any());
    }

    @Test
    void giveInvalidScope_whenOnMsg_thenTellFailure() throws Exception {
        final TbMsgMetaData metaData = new TbMsgMetaData();
        final String data = "{}";

        config.setScope("INVALID_SCOPE");
        nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        TbMsg msg = TbMsg.newMsg("POST_ATTRIBUTES_REQUEST", deviceId, metaData, data, callback);
        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(ctx, never()).tellSuccess(any());
        verify(ctx, times(1)).tellFailure(newMsgCaptor.capture(), exceptionCaptor.capture());

        assertThat(newMsgCaptor.getValue()).isSameAs(msg);
        assertThat(exceptionCaptor.getValue()).isInstanceOf(IllegalArgumentException.class);
    }
}