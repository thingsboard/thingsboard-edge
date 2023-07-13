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
package org.thingsboard.integration.http.particle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.controller.JsonHttpIntegrationMsg;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.http.basic.BasicHttpIntegration;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ParticleIntegration extends BasicHttpIntegration<JsonHttpIntegrationMsg> {

    private ParticleConfiguration particleConfiguration;
    private RestTemplate httpClient;
    private final static String BASE_URL = "https://api.particle.io";;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        particleConfiguration = JacksonUtil.fromString(JacksonUtil.toString(configuration.getConfiguration()), ParticleConfiguration.class);

        if (particleConfiguration.isAllowDownlink()) {
            initRestClient();
            particleConfiguration.getCredentials().setInterceptor(httpClient, BASE_URL);
        }
    }

    @Override
    protected ResponseEntity doProcess(JsonHttpIntegrationMsg msg) throws Exception {
        if (checkSecurity(msg)) {
            Map<String, UplinkData> result = processUplinkData(context, msg);
            if (result.isEmpty()) {
                return fromStatus(HttpStatus.NO_CONTENT);
            } else {
                return fromStatus(HttpStatus.OK);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    @Override
    public void onDownlinkMsg(IntegrationDownlinkMsg downlink) {
        TbMsg msg = downlink.getTbMsg();
        logDownlink(context, "Downlink: " + msg.getType(), msg);
        if (downlinkConverter != null) {
            try {
                processDownLinkMsg(context, msg);
            } catch (Exception e) {
                reportDownlinkError(context, msg, "FAILURE", e);
            }
        }
    }

    private void processDownLinkMsg(IntegrationContext context, TbMsg msg) {
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        mdMap.putAll(msg.getMetaData().values());
        try {
            List<DownlinkData> result = downlinkConverter.convertDownLink(
                    context.getDownlinkConverterContext(),
                    Collections.singletonList(msg),
                    new IntegrationMetaData(mdMap));
            if (!result.isEmpty()) {
                for (DownlinkData downlink : result) {
                    if (downlink.isEmpty()) {
                        continue;
                    }
                    Map<String, String> metadata = downlink.getMetadata();
                    if (metadata.get("deviceId") == null) {
                        throw new RuntimeException("Device id not found in metadata");
                    }
                    ObjectNode body = JacksonUtil.newObjectNode();
                    String function = "";
                    if (!body.has("method") && !body.has("function")) {
                        throw new RuntimeException("Field \"method\" or \"function\" is required in message");
                    } else if (body.has("method")) {
                        function = body.get("method").asText();
                    } else if (body.has("function")) {
                        function = body.get("function").asText();
                    }

                    String arg = "";
                    if (body.has("params")) {
                        arg = body.get("params").asText();
                    } else if (body.has("arg")) {
                        arg = body.get("arg").asText();
                    }

                    String url = BASE_URL + "/v1/devices/" + metadata.get("deviceId") + "/" + function;
                    HashMap<Object, Object> requestBody = new HashMap<>();
                    if (StringUtils.isNotBlank(arg)) {
                        requestBody.put("arg", arg);
                    }
                    ResponseEntity<JsonNode> responseEntity = httpClient.postForEntity(url, requestBody, JsonNode.class);
                    if (responseEntity.getStatusCode().is2xxSuccessful()) {
                        reportDownlinkOk(context, downlink);
                    } else {
                        reportDownlinkError(context, msg, "ERROR", new RuntimeException(responseEntity.getStatusCode().getReasonPhrase()));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to process downLink message", e);
            reportDownlinkError(context, msg, "ERROR", e);
        }
    }


    private void initRestClient() {
        httpClient = new RestTemplate();
        httpClient.setErrorHandler(new DefaultResponseErrorHandler() {
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
    }
}
