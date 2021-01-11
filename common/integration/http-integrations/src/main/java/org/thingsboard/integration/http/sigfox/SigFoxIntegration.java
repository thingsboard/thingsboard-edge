/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.integration.http.sigfox;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.DownLinkMsg;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.controller.HttpIntegrationMsg;
import org.thingsboard.integration.http.basic.BasicHttpIntegration;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
    protected ResponseEntity doProcess(HttpIntegrationMsg msg) throws Exception {
        if (checkSecurity(msg)) {
            Map<String, UplinkData> result = processUplinkData(context, msg);
            if (result.isEmpty()) {
                return fromStatus(HttpStatus.NO_CONTENT);
            } else if (result.size() > 1) {
                return fromStatus(HttpStatus.BAD_REQUEST);
            } else {
                Entry<String, UplinkData> entry = result.entrySet().stream().findFirst().get();
                String deviceIdAttributeName = metadataTemplate.getKvMap().getOrDefault("SigFoxDeviceIdAttributeName", "device");
                String sigFoxDeviceId = msg.getMsg().get(deviceIdAttributeName).asText();
                return processDownLinkData(context, entry.getKey(), msg, sigFoxDeviceId);
            }
        } else {
            return fromStatus(HttpStatus.FORBIDDEN);
        }
    }

    private ResponseEntity processDownLinkData(IntegrationContext context, String deviceName, HttpIntegrationMsg msg, String sigFoxDeviceId) throws Exception {
        if (downlinkConverter != null) {
            DownLinkMsg pending = context.getDownlinkMsg(deviceName);
            if (pending != null && !pending.isEmpty()) {
                Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
                msg.getRequestHeaders().forEach(
                        (header, value) -> {
                            mdMap.put("header:" + header, value);
                        }
                );
                List<DownlinkData> result = downlinkConverter.convertDownLink(context.getDownlinkConverterContext(), pending.getMsgs(), new IntegrationMetaData(mdMap));
                context.removeDownlinkMsg(deviceName);
                if (result.size() == 1 && !result.get(0).isEmpty()) {
                    DownlinkData downlink = result.get(0);
                    ObjectNode json = mapper.createObjectNode();
                    json.putObject(sigFoxDeviceId).put("downlinkData", new String(downlink.getData(), StandardCharsets.UTF_8));
                    HttpHeaders responseHeaders = new HttpHeaders();
                    responseHeaders.add("Content-Type", "application/json");
                    ResponseEntity response = new ResponseEntity(json, responseHeaders, HttpStatus.OK);
                    logDownlink(context, "Downlink", response);
                    return response;
                }
            }
        }

        return fromStatus(HttpStatus.NO_CONTENT);
    }

}
