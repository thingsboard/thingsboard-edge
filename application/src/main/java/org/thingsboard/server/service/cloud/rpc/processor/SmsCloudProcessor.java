// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cloud.rpc.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.SendSmsUplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * Converts a SEND_SMS cloud event into a {@link SendSmsUplinkMsg}. The Edge does not resolve or transmit
 * the SMS itself; it forwards the serialized {@code EdgeSmsRequest} (carried in the cloud event body) to
 * the Cloud, which resolves the config and sends via its own provider.
 */
@Slf4j
@Component
@TbCoreComponent
public class SmsCloudProcessor {

    public UplinkMsg convertSendSmsEventToUplink(CloudEvent cloudEvent) {
        log.trace("Executing convertSendSmsEventToUplink, cloudEvent [{}]", cloudEvent);
        TenantId tenantId = cloudEvent.getTenantId();
        SendSmsUplinkMsg sendSmsUplinkMsg = SendSmsUplinkMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setRequest(JacksonUtil.toString(cloudEvent.getEntityBody()))
                .build();

        return UplinkMsg.newBuilder()
                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                .addSendSmsUplinkMsg(sendSmsUplinkMsg)
                .build();
    }

}
