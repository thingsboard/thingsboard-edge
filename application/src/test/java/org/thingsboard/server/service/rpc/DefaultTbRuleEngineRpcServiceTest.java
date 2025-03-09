/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.rpc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class DefaultTbRuleEngineRpcServiceTest {

    @Mock
    private TbClusterService tbClusterServiceMock;

    @InjectMocks
    private DefaultTbRuleEngineRpcService tbRuleEngineRpcService;

    @Test
    public void givenTbMsg_whenSendRestApiCallReply_thenPushNotificationToCore() {
        // GIVEN
        String serviceId = "tb-core-0";
        UUID requestId = UUID.fromString("f64a20df-eb1e-46a3-ba6f-0b3ae053ee0a");
        DeviceId deviceId = new DeviceId(UUID.fromString("1d9f771a-7cdc-4ac7-838c-ba193d05a012"));
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.REST_API_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        var restApiCallResponseMsgProto = TransportProtos.RestApiCallResponseMsgProto.newBuilder()
                .setRequestIdMSB(requestId.getMostSignificantBits())
                .setRequestIdLSB(requestId.getLeastSignificantBits())
                .setResponse(TbMsg.toByteString(msg))
                .build();

        // WHEN
        tbRuleEngineRpcService.sendRestApiCallReply(serviceId, requestId, msg);

        // THEN
        then(tbClusterServiceMock).should().pushNotificationToCore(serviceId, restApiCallResponseMsgProto, null);
    }
}
