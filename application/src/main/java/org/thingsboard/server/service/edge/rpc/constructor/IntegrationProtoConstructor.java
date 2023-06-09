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
package org.thingsboard.server.service.edge.rpc.constructor;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.gen.edge.v1.IntegrationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

@Component
@Slf4j
public class IntegrationProtoConstructor {

    public IntegrationUpdateMsg constructIntegrationUpdateMsg(UpdateMsgType msgType, Integration integration, JsonNode configuration) {
        IntegrationUpdateMsg.Builder builder = IntegrationUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(integration.getId().getId().getMostSignificantBits())
                .setIdLSB(integration.getId().getId().getLeastSignificantBits())
                .setName(integration.getName())
                .setDefaultConverterIdMSB(integration.getDefaultConverterId().getId().getMostSignificantBits())
                .setDefaultConverterIdLSB(integration.getDefaultConverterId().getId().getLeastSignificantBits())
                .setRoutingKey(integration.getRoutingKey())
                .setType(integration.getType().name())
                .setDebugMode(integration.isDebugMode())
                .setEnabled(integration.isEnabled())
                .setRemote(integration.isRemote())
                .setAllowCreateDevicesOrAssets(integration.isAllowCreateDevicesOrAssets())
                .setConfiguration(JacksonUtil.toString(configuration));
        if (integration.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(integration.getAdditionalInfo()));
        }
        if (integration.getSecret() != null) {
            builder.setSecret(integration.getSecret());
        }
        if (integration.getDownlinkConverterId() != null) {
            builder.setDownlinkConverterIdMSB(integration.getDownlinkConverterId().getId().getMostSignificantBits())
                    .setDownlinkConverterIdLSB(integration.getDownlinkConverterId().getId().getLeastSignificantBits());
        }
        return builder.build();
    }

    public IntegrationUpdateMsg constructIntegrationDeleteMsg(IntegrationId integrationId) {
        return IntegrationUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(integrationId.getId().getMostSignificantBits())
                .setIdLSB(integrationId.getId().getLeastSignificantBits()).build();
    }
}
