// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cloud.rpc.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.SendEmailUplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * Converts a SEND_EMAIL cloud event into a {@link SendEmailUplinkMsg}. The Edge does not render or
 * transmit the mail itself; it forwards the serialized {@code EdgeMailRequest} (carried in the cloud
 * event body) to the Cloud, which resolves the config, renders and sends via its own SMTP.
 */
@Slf4j
@Component
@TbCoreComponent
public class MailCloudProcessor {

    public UplinkMsg convertSendEmailEventToUplink(CloudEvent cloudEvent) {
        log.trace("Executing convertSendEmailEventToUplink, cloudEvent [{}]", cloudEvent);
        TenantId tenantId = cloudEvent.getTenantId();
        SendEmailUplinkMsg sendEmailUplinkMsg = SendEmailUplinkMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setRequest(JacksonUtil.toString(cloudEvent.getEntityBody()))
                .build();

        return UplinkMsg.newBuilder()
                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                .addSendEmailUplinkMsg(sendEmailUplinkMsg)
                .build();
    }

}
