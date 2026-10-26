// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cloud.rpc.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.SendNotificationUplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * Converts a SEND_NOTIFICATION cloud event into a {@link SendNotificationUplinkMsg}. The Edge forwards the
 * serialized {@code EdgeNotificationRequest} (carried in the cloud event body) to the Cloud, which resolves
 * the channel credentials and delivers (Slack post / FCM push).
 */
@Slf4j
@Component
@TbCoreComponent
public class SendNotificationCloudProcessor {

    public UplinkMsg convertSendNotificationEventToUplink(CloudEvent cloudEvent) {
        log.trace("Executing convertSendNotificationEventToUplink, cloudEvent [{}]", cloudEvent);
        TenantId tenantId = cloudEvent.getTenantId();
        SendNotificationUplinkMsg sendNotificationUplinkMsg = SendNotificationUplinkMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setRequest(JacksonUtil.toString(cloudEvent.getEntityBody()))
                .build();

        return UplinkMsg.newBuilder()
                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                .addSendNotificationUplinkMsg(sendNotificationUplinkMsg)
                .build();
    }

}
