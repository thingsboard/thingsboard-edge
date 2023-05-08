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
package org.thingsboard.integration.http.chirpstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.controller.JsonHttpIntegrationMsg;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.http.basic.BasicHttpIntegration;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.msg.TbMsg;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zbeacon on 06.09.20.
 */
@Slf4j
public class ChirpStackIntegration extends BasicHttpIntegration<JsonHttpIntegrationMsg> {

    private static final String DEVICES_ENDPOINT = "/api/devices";
    private static final String DEV_EUI = "DevEUI";
    private static final String F_PORT = "fPort";
    private static final String DATA = "data";
    private static final String CONFIRMED = "confirmed";
    private static final String DEVICE_DOWNLINK_QUEUE = "deviceQueueItem";

    private String applicationServerUrl = "";
    private String applicationServerAPIToken = "";
    private final RestTemplate httpClient = new RestTemplate();

    private String devicesUrl;


    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        JsonNode json = configuration.getConfiguration();
        if (json.get("clientConfiguration").has("applicationServerAPIToken")) {
            applicationServerUrl = json.get("clientConfiguration").get("applicationServerUrl").asText();
            applicationServerAPIToken = json.get("clientConfiguration").get("applicationServerAPIToken").asText();
        }
        devicesUrl = applicationServerUrl + DEVICES_ENDPOINT;
    }

    private HttpEntity<JsonNode> createRequest(JsonNode deviceInfo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(applicationServerAPIToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (deviceInfo != null) {
            return new HttpEntity<>(deviceInfo, headers);
        } else {
            return new HttpEntity<>(headers);
        }
    }

    @Override
    protected ResponseEntity doProcess(JsonHttpIntegrationMsg msg) throws Exception {
        List<UplinkData> uplinkDataList = convertToUplinkDataList(context, msg.getMsgInBytes(), metadataTemplate);
        if (uplinkDataList != null) {
            for (UplinkData data : uplinkDataList) {
                processUplinkData(context, data);
                log.trace("[{}] Processing uplink data", data);
            }
        }
        return fromStatus(HttpStatus.OK);
    }

    @Override
    protected String getTypeUplink(JsonHttpIntegrationMsg msg) {
        return "Uplink";
    }

    @Override
    public void onDownlinkMsg(IntegrationDownlinkMsg downlink) {
        TbMsg msg = downlink.getTbMsg();
        if (StringUtils.isEmpty(this.applicationServerAPIToken)) {
            Exception e = new RuntimeException("Cannot send downlink because of Application Server API Token was not set.");
            log.warn("Failed to process downLink message", e);
            reportDownlinkError(context, msg, "ERROR", e);
        }
        logDownlink(context, "Downlink: " + msg.getType(), msg);
        if (downlinkConverter != null) {
            processDownLinkMsg(context, msg);
        }
    }

    private void processDownLinkMsg(IntegrationContext context, TbMsg msg) {
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
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
                    if (!metadata.containsKey(DEV_EUI)) {
                        throw new ThingsboardException("DevEUI is missing in the downlink metadata!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                    }
                    if (!metadata.containsKey(F_PORT)) {
                        throw new ThingsboardException("FPort is missing in the downlink metadata!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                    }
                    String payload = new String(downlink.getData(), StandardCharsets.UTF_8);
                    ObjectNode body = mapper.createObjectNode();
                    if (metadata.containsKey(CONFIRMED)) {
                        body.with(DEVICE_DOWNLINK_QUEUE).put(CONFIRMED, metadata.get(CONFIRMED));
                    }
                    body.with(DEVICE_DOWNLINK_QUEUE).put(DATA, payload);
                    body.with(DEVICE_DOWNLINK_QUEUE).put(F_PORT, metadata.get(F_PORT));
                    httpClient.postForEntity(devicesUrl + "/" + metadata.get(DEV_EUI) + "/queue", createRequest(body), String.class);
                    reportDownlinkOk(context, downlink);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to process downLink message", e);
            reportDownlinkError(context, msg, "ERROR", e);
        }
    }

}