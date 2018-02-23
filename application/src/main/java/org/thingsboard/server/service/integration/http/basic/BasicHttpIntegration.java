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
package org.thingsboard.server.service.integration.http.basic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.service.converter.DownLinkMetaData;
import org.thingsboard.server.service.converter.DownlinkData;
import org.thingsboard.server.service.converter.UplinkData;
import org.thingsboard.server.service.converter.UplinkMetaData;
import org.thingsboard.server.service.integration.IntegrationContext;
import org.thingsboard.server.service.integration.TbIntegrationInitParams;
import org.thingsboard.server.service.integration.downlink.DownLinkMsg;
import org.thingsboard.server.service.integration.http.AbstractHttpIntegration;
import org.thingsboard.server.service.integration.http.HttpIntegrationMsg;
import org.thingsboard.server.service.integration.msg.RPCCallIntegrationMsg;
import org.thingsboard.server.service.integration.msg.SharedAttributesUpdateIntegrationMsg;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;

@Slf4j
public class BasicHttpIntegration extends AbstractHttpIntegration<HttpIntegrationMsg> {

    public static final String HEADER = "Header:";
    private boolean securityEnabled = false;
    private Map<String, String> headersFilter = new HashMap<>();

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        JsonNode json = configuration.getConfiguration();
        securityEnabled = json.has("enableSecurity") && json.get("enableSecurity").asBoolean();
        if (securityEnabled && json.has("headersFilter")) {
            JsonNode headersFilterNode = json.get("headersFilter");
            for (Iterator<Map.Entry<String, JsonNode>> it = headersFilterNode.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> headerFilter = it.next();
                headersFilter.put(headerFilter.getKey(), headerFilter.getValue().asText());
            }
        }
    }

    @Override
    protected ResponseEntity doProcess(IntegrationContext context, HttpIntegrationMsg msg) throws Exception {
        if (checkSecurity(msg)) {
            Map<Device, UplinkData> result = processUplinkData(context, msg);
            if (result.isEmpty()) {
                return fromStatus(HttpStatus.NO_CONTENT);
            } else {
                return processDownLinkData(context, result);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    @Override
    public void onSharedAttributeUpdate(IntegrationContext context, SharedAttributesUpdateIntegrationMsg msg) {
        logDownlink(context, "SharedAttributeUpdate", msg);
        if (downlinkConverter != null) {
            context.getDownlinkService().put(msg);
        }
    }

    @Override
    public void onRPCCall(IntegrationContext context, RPCCallIntegrationMsg msg) {
        logDownlink(context, "RPCCall", msg);
        if (downlinkConverter != null) {
            context.getDownlinkService().put(msg);
        }
    }

    private ResponseEntity processDownLinkData(IntegrationContext context, Map<Device, UplinkData> uplinkData) throws Exception {
        if (downlinkConverter != null) {
            List<DownLinkMsg> pendingDownlinks = new ArrayList<>();
            for (Device device : uplinkData.keySet()) {
                DownLinkMsg pending = context.getDownlinkService().get(configuration.getId(), device.getId());
                if (pending != null) {
                    pendingDownlinks.add(pending);
                }
            }

            List<DownlinkData> result = downlinkConverter.convertDownLink(context.getConverterContext(), pendingDownlinks, new DownLinkMetaData(Collections.emptyMap()));

            for (Device device : uplinkData.keySet()) {
                context.getDownlinkService().remove(configuration.getId(), device.getId());
            }

            if (result.size() == 1 && !result.get(0).isEmpty()) {
                DownlinkData downlink = result.get(0);
                ResponseEntity response = null;
                switch (downlink.getContentType()) {
                    case "JSON":
                        response = convertJson(downlink);
                        break;
                    case "TEXT":
                        response = convertText(downlink);
                        break;
                    case "BINARY":
                        response = convertBinary(downlink);
                        break;
                    default:
                        throw new RuntimeException("Not supported content type: " + downlink.getContentType());
                }
                logDownlink(context, "Downlink", response);
                return response;
            }
        }

        return fromStatus(HttpStatus.NO_CONTENT);
    }

    private ResponseEntity convertJson(DownlinkData downlink) {
        HttpHeaders responseHeaders = getHttpHeaders(downlink, "application/json");
        return new ResponseEntity(new String(downlink.getData(), StandardCharsets.UTF_8), responseHeaders, HttpStatus.OK);
    }

    private ResponseEntity convertText(DownlinkData downlink) {
        HttpHeaders responseHeaders = getHttpHeaders(downlink, "text/plain");
        return new ResponseEntity(new String(downlink.getData(), StandardCharsets.UTF_8), responseHeaders, HttpStatus.OK);
    }

    private ResponseEntity convertBinary(DownlinkData downlink) {
        HttpHeaders responseHeaders = getHttpHeaders(downlink, "application/octet-stream");
        return new ResponseEntity(downlink.getData(), responseHeaders, HttpStatus.OK);
    }

    private HttpHeaders getHttpHeaders(DownlinkData downlink, String defaultContentType) {
        HttpHeaders responseHeaders = new HttpHeaders();

        boolean contentTypePresent = false;
        for (Entry<String, String> kv : downlink.getMetadata().entrySet()) {
            if (kv.getKey().startsWith(HEADER)) {
                String headerName = kv.getKey().substring(HEADER.length());
                responseHeaders.add(headerName, kv.getValue());
                if (headerName.equalsIgnoreCase("Content-Type")) {
                    contentTypePresent = true;
                }
            }
        }
        if (!contentTypePresent) {
            responseHeaders.add("Content-Type", defaultContentType);
        }
        return responseHeaders;
    }

    protected <T> void logDownlink(IntegrationContext context, String updateType, T msg) {
        if (configuration.isDebugMode()) {
            try {
                persistDebug(context, updateType, "JSON", mapper.writeValueAsString(msg), downlinkConverter != null ? "OK" : "FAILURE", null);
            } catch (Exception e) {
                log.warn("Failed to persist debug message", e);
            }
        }
    }

    protected Map<Device, UplinkData> processUplinkData(IntegrationContext context, HttpIntegrationMsg msg) throws Exception {
        byte[] data = mapper.writeValueAsBytes(msg.getMsg());
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        msg.getRequestHeaders().forEach(
                (header, value) -> {
                    mdMap.put("Header:" + header, value);
                }
        );

        List<UplinkData> uplinkDataList = convertToUplinkDataList(context, data, new UplinkMetaData(getUplinkContentType(), mdMap));
        if (uplinkDataList != null && !uplinkDataList.isEmpty()) {
            Map<Device, UplinkData> result = new HashMap<>();
            for (UplinkData uplinkData : uplinkDataList) {
                result.put(processUplinkData(context, uplinkData), uplinkData);
                log.info("[{}] Processing uplink data", uplinkData);
            }
            return result;
        } else {
            return Collections.emptyMap();
        }
    }

    protected boolean checkSecurity(HttpIntegrationMsg msg) throws Exception {
        if (securityEnabled) {
            Map<String, String> requestHeaders = msg.getRequestHeaders();
            log.trace("Validating request using the following request headers: {}", requestHeaders);
            for (Map.Entry<String, String> headerFilter : headersFilter.entrySet()) {
                String value = requestHeaders.get(headerFilter.getKey().toLowerCase());
                if (value == null || !value.equals(headerFilter.getValue())) {
                    return false;
                }
            }
        }
        return true;
    }

}
