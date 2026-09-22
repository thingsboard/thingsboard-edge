/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
