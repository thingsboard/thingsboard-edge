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
package org.thingsboard.integration.http.kpn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.controller.HttpIntegrationMsg;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.http.basic.BasicHttpIntegration;
import org.thingsboard.server.common.msg.TbMsg;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class KpnIntegration<T extends HttpIntegrationMsg<?>> extends BasicHttpIntegration<HttpIntegrationMsg<?>> {

    private final static String FPORT_PARAM = "FPort";
    private final static String SUB_CUSTOMER_ID_PARAM = "subCustomerId";
    private final static String GRIP_BASE_URL = "https://auth.grip-on-it.com";
    private final static String KPN_BASE_URL = "https://api.kpnthings.com/";
    private final static String DOWNLINK_PATH = "api/actuator/downlinks";
    private final static String ACCESS_REQUEST_PATH = "api/v1/lifecycle/clients/" + SUB_CUSTOMER_ID_PARAM + "/context-switch";
    private final static String APPLICATION_ID = "4dc82561-f65f-523g-dek9-6c79ec314f02";
    private final static String SIGNED_BODY_REQUEST_HEADER = "things-message-token";

    private final RestTemplate httpClient = new RestTemplate();
    private AtomicLong tokenExpiresIn = new AtomicLong(0);
    private String token;
    private String gripAuthEndpoint;

    private Map<String, String> headersFilter = new HashMap<>();
    private ConcurrentHashMap<String, Boolean> subCustomersRetrievedAccess = new ConcurrentHashMap<>();

    private KpnConfiguration kpnConfiguration;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        JsonNode json = configuration.getConfiguration();
        kpnConfiguration = JacksonUtil.fromString(JacksonUtil.toString(configuration.getConfiguration()), KpnConfiguration.class);
        headersFilter.clear();
        if (kpnConfiguration.getEnableSecurity() && json.has("headersFilter")) {
            JsonNode headersFilterNode = json.get("headersFilter");
            for (Iterator<Map.Entry<String, JsonNode>> it = headersFilterNode.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> headerFilter = it.next();
                headersFilter.put(headerFilter.getKey(), headerFilter.getValue().asText());
            }
        }
        gripAuthEndpoint = GRIP_BASE_URL + "/v2/" + kpnConfiguration.getGripTenantId() + "/oidc/idp/c1/token";
    }

    @Override
    public void onDownlinkMsg(IntegrationDownlinkMsg msg) {
        if (downlinkConverter != null) {
            processDownLinkMsg(context, msg.getTbMsg());
        }
    }

    private void processDownLinkMsg(IntegrationContext context, TbMsg msg) {
        String status = "OK";
        Exception exception = null;
        logDownlink(context, "Downlink: " + msg.getType(), msg);
        try {
            if (doProcessDownLinkMsg(context, msg)) {
                integrationStatistics.incMessagesProcessed();
            }
        } catch (Exception e) {
            log.warn("Failed to process downLink message", e);
            exception = e;
            status = "ERROR";
        }
        reportDownlinkError(context, msg, status, exception);
    }

    private boolean doProcessDownLinkMsg(IntegrationContext context, TbMsg msg) throws Exception {
        boolean downlinkResult = false;
        if (System.currentTimeMillis() >= tokenExpiresIn.get()) {
            refreshToken();
        }
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        mdMap.putAll(msg.getMetaData().getData());
        try {
            List<DownlinkData> result = downlinkConverter.convertDownLink(context.getDownlinkConverterContext(), Collections.singletonList(msg), new IntegrationMetaData(mdMap));
            for (DownlinkData downlinkData : result) {
                downlinkResult = true;
                try {
                    sendDownlinkRequest(downlinkData, mdMap);
                    reportDownlinkOk(context, downlinkData);
                } catch (Exception e) {
                    reportDownlinkError(context, msg, "FAILED", e);
                    downlinkResult = false;
                }
            }
        } catch (Exception e) {
            reportDownlinkError(context, msg, "FAILED", e);
            downlinkResult = false;
        }
        return downlinkResult;
    }

    @Override
    protected boolean checkSecurity(HttpIntegrationMsg<?> msg) throws Exception {
        Map<String, String> requestHeaders = msg.getRequestHeaders();
        log.trace("Validating request using the following request headers: {}", requestHeaders);
        String signed = requestHeaders.get(SIGNED_BODY_REQUEST_HEADER);
        byte[] dataForHash = ArrayUtils.addAll(msg.getMsgInBytes(), kpnConfiguration.getDestinationSharedSecret().getBytes());
        String hashed = new DigestUtils("SHA-256").digestAsHex(dataForHash);
        if (!signed.equals(hashed)) {
            return false;
        }
        if (kpnConfiguration.getEnableSecurity()) {
            for (Map.Entry<String, String> headerFilter : headersFilter.entrySet()) {
                String value = requestHeaders.get(headerFilter.getKey().toLowerCase());
                if (value == null || !value.equals(headerFilter.getValue())) {
                    return false;
                }
            }
        }
        return true;
    }

    private void sendDownlinkRequest(DownlinkData downlinkData, Map<String, String> mdMap) throws Exception {
        if (mdMap.containsKey(FPORT_PARAM) || downlinkData.getMetadata().containsKey(FPORT_PARAM)) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/vnd.kpnthings.actuator.v1.response+json");
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);
            String subCustomerId = mdMap.get(SUB_CUSTOMER_ID_PARAM);
            if (subCustomerId != null) {
                if (!subCustomersRetrievedAccess.containsKey(subCustomerId)) {
                    requestSubCustomerAccess(subCustomerId);
                }
                headers.set("x-client-id", mdMap.get(SUB_CUSTOMER_ID_PARAM));
            }
            String fPort = downlinkData.getMetadata().containsKey(FPORT_PARAM) ? downlinkData.getMetadata().get(FPORT_PARAM) : mdMap.get(FPORT_PARAM);
            String uriString = UriComponentsBuilder.fromHttpUrl(KPN_BASE_URL + DOWNLINK_PATH)
                    .query("port=" + fPort)
                    .encode()
                    .toUriString();
            RequestEntity<Object> requestEntity = new RequestEntity<>(downlinkData.getData(), headers, HttpMethod.POST, URI.create(uriString));
            ResponseEntity<ObjectNode> response = httpClient.exchange(requestEntity, ObjectNode.class);
            if (HttpStatus.FORBIDDEN.equals(response.getStatusCode()) && subCustomerId != null) {
                requestSubCustomerAccess(subCustomerId);
                response = httpClient.exchange(requestEntity, ObjectNode.class);
            }
            if (!validateResponse(response)) {
                HttpStatus responseStatusCode = response.getStatusCode();
                throw new RuntimeException(String.format("Received response: %s, %s with body: %s",
                        responseStatusCode.name(), responseStatusCode.getReasonPhrase(), response.getBody()));
            }
        } else {
            throw new RuntimeException("Cannot find FPort in metadata!");
        }
    }

    private void requestSubCustomerAccess(String subCustomerId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        String accessRequestUrl = KPN_BASE_URL + ACCESS_REQUEST_PATH.replace(SUB_CUSTOMER_ID_PARAM, subCustomerId);
        RequestEntity<Object> requestEntity = new RequestEntity<>(null, headers, HttpMethod.POST, URI.create(accessRequestUrl));
        ResponseEntity<ObjectNode> response = httpClient.exchange(requestEntity, ObjectNode.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            subCustomersRetrievedAccess.put(subCustomerId, true);
        } else {
            throw new RuntimeException(String.format("Cannot retrieve access for sub customer with id %s", subCustomerId));
        }
    }

    private void refreshToken() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json; charset=utf-8");
        ObjectNode bodyNode = JacksonUtil.newObjectNode();
        bodyNode.put("grant_type", "client_credentials");
        bodyNode.put("audience", APPLICATION_ID);
        bodyNode.put("client_id", kpnConfiguration.getApiId());
        bodyNode.put("client_secret", kpnConfiguration.getApiKey());
        RequestEntity<Object> requestEntity = new RequestEntity<>(bodyNode, headers, HttpMethod.POST, URI.create(gripAuthEndpoint));
        ResponseEntity<ObjectNode> response = httpClient.exchange(requestEntity.getUrl(),
                Objects.requireNonNull(requestEntity.getMethod()),
                requestEntity,
                ObjectNode.class);
        ObjectNode responseBody = response.getBody();
        if (HttpStatus.OK.equals(response.getStatusCode()) && responseBody != null) {
            token = responseBody.get("access_token").asText();
            tokenExpiresIn.set(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(responseBody.get("expires_in").asLong() - 600));
        } else {
            throw new RuntimeException(String.format("Cannot retrieve access token from GRIP with status: %s and body: %s",
                    response.getStatusCode().name(),
                    responseBody));
        }
    }

    private boolean validateResponse(ResponseEntity<ObjectNode> responseEntity) {
        return responseEntity != null
                && responseEntity.getStatusCode().is2xxSuccessful()
                && responseEntity.getBody() != null;
    }
}
