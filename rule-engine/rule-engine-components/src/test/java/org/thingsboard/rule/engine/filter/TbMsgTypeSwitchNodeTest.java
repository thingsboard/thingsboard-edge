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
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class TbMsgTypeSwitchNodeTest {

    private static final DeviceId DEVICE_ID = new DeviceId(UUID.randomUUID());

    private TbMsgTypeSwitchNode node;

    private TbContext ctx;

    @BeforeEach
    void setUp() {
        ctx = mock(TbContext.class);
        node = new TbMsgTypeSwitchNode();
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    void givenAllTypes_whenOnMsg_then_allTypesSupported() throws TbNodeException {
        // GIVEN
        List<TbMsg> tbMsgList = new ArrayList<>();
        var tbMsgTypes = TbMsgType.values();
        for (var msgType : tbMsgTypes) {
            tbMsgList.add(getTbMsg(msgType));
        }

        // WHEN
        for (TbMsg tbMsg : tbMsgList) {
            node.onMsg(ctx, tbMsg);
        }

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<String> nodeConnectionCapture = ArgumentCaptor.forClass(String.class);
        verify(ctx, times(tbMsgList.size())).tellNext(newMsgCaptor.capture(), nodeConnectionCapture.capture());
        verify(ctx, never()).tellFailure(any(), any());
        var resultMsgs = newMsgCaptor.getAllValues();
        var resultNodeConnections = nodeConnectionCapture.getAllValues();
        for (int i = 0; i < resultMsgs.size(); i++) {
            var msg = resultMsgs.get(i);
            assertThat(msg).isNotNull();
            assertThat(msg.getType()).isNotNull();
            assertThat(msg.getType()).isEqualTo(msg.getInternalType().name());
            assertThat(msg).isSameAs(tbMsgList.get(i));
            assertThat(resultNodeConnections.get(i))
                    .isEqualTo(TbMsgType.getRuleNodeConnectionOrElseOther(msg.getInternalType()));
        }
    }

    private TbMsg getTbMsg(TbMsgType msgType) {
        return TbMsg.newMsg(msgType, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
    }

}
