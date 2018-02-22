/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.service.integration.http.sigfox;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.service.converter.DownLinkMetaData;
import org.thingsboard.server.service.converter.DownlinkData;
import org.thingsboard.server.service.converter.UplinkData;
import org.thingsboard.server.service.integration.IntegrationContext;
import org.thingsboard.server.service.integration.TbIntegrationInitParams;
import org.thingsboard.server.service.integration.downlink.DownLinkMsg;
import org.thingsboard.server.service.integration.http.HttpIntegrationMsg;
import org.thingsboard.server.service.integration.http.basic.BasicHttpIntegration;
import org.thingsboard.server.service.integration.msg.RPCCallIntegrationMsg;
import org.thingsboard.server.service.integration.msg.SharedAttributesUpdateIntegrationMsg;
import org.thingsboard.server.service.integration.msg.ToDeviceIntegrationMsg;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Slf4j
public class SigFoxIntegration extends BasicHttpIntegration {

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
    }

    @Override
    protected ResponseEntity doProcess(IntegrationContext context, HttpIntegrationMsg msg) throws Exception {
        if (checkSecurity(msg)) {
            Map<Device, UplinkData> result = processUplinkData(context, msg);
            if (result.isEmpty()) {
                return fromStatus(HttpStatus.NO_CONTENT);
            } else if (result.size() > 1) {
                return fromStatus(HttpStatus.BAD_REQUEST);
            } else {
                Entry<Device, UplinkData> entry = result.entrySet().stream().findFirst().get();
                String deviceIdAttributeName = metadataTemplate.getKvMap().getOrDefault("SigFoxDeviceIdAttributeName", "device");
                String sigFoxDeviceId = msg.getMsg().get(deviceIdAttributeName).asText();
                return processDownLinkData(context, entry.getKey(), entry.getValue(), sigFoxDeviceId);
            }
        } else {
            return fromStatus(HttpStatus.FORBIDDEN);
        }
    }

    protected ResponseEntity processDownLinkData(IntegrationContext context, Device device, UplinkData uplink, String sigFoxDeviceId) throws Exception {
        if (downlinkConverter != null) {
            DownLinkMsg pending = context.getDownlinkService().get(configuration.getId(), device.getId());
            if (pending != null) {
                List<DownlinkData> result = downlinkConverter.convertDownLink(context.getConverterContext(), Collections.singletonList(pending), new DownLinkMetaData(Collections.emptyMap()));
                if (result.size() == 0 || result.size() > 1) {
                    fromStatus(HttpStatus.NO_CONTENT);
                } else {
                    DownlinkData downlink = result.get(0);
                    ObjectNode json = mapper.createObjectNode();
                    json.putObject(sigFoxDeviceId).put("downlinkData", new String(downlink.getData(), StandardCharsets.UTF_8));
                    ResponseEntity response = new ResponseEntity(json, HttpStatus.OK);
                    response.getHeaders().add("Content-Type", "application/json");
                    return response;
                }
            }
        }

        return fromStatus(HttpStatus.NO_CONTENT);
    }

    @Override
    public void onSharedAttributeUpdate(IntegrationContext context, SharedAttributesUpdateIntegrationMsg msg) {
        logUpdate(context, "SharedAttributeUpdate", msg);
        if (downlinkConverter != null) {
            context.getDownlinkService().put(msg);
        }
    }

    @Override
    public void onRPCCall(IntegrationContext context, RPCCallIntegrationMsg msg) {
        logUpdate(context, "RPCCall", msg);
        if (downlinkConverter != null) {
            context.getDownlinkService().put(msg);
        }
    }

    private <T extends ToDeviceIntegrationMsg> void logUpdate(IntegrationContext context, String updateType, T msg) {
        if (configuration.isDebugMode()) {
            try {
                persistDebug(context, "Downlink", updateType, mapper.writeValueAsString(msg), downlinkConverter != null ? "OK" : "FAILURE", null);
            } catch (Exception e) {
                log.warn("Failed to persist debug message", e);
            }
        }
    }

}
