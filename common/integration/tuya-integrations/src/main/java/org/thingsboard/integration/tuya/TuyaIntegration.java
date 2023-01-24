/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.integration.tuya;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.common.util.HmacSHA256Util;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.api.data.UplinkContentType;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.integration.tuya.mq.MessageVO;
import org.thingsboard.integration.tuya.mq.MqConsumer;
import org.thingsboard.integration.tuya.mq.TuyaMessageUtil;
import org.thingsboard.integration.tuya.util.TuyaToken;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.msg.TbMsg;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
public class TuyaIntegration extends AbstractIntegration<TuyaIntegrationMsg> {

    private static final String EMPTY_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private final static String GET_TOKEN_URL_PATH = "/v1.0/token";
    private final static String POST_COMMANDS_URL_PATH = "/v1.0/iot-03/devices/{deviceId}/commands";
    private final static Set<Integer> unauthorizedErrorCodes = new HashSet<>(Arrays.asList(1010, 1011, 1012));

    private final RestTemplate httpClient = new RestTemplate();
    private TuyaToken accessToken;
    private ExecutorService executor;
    private TuyaIntegrationConfiguration tuyaIntegrationConfiguration;
    private MqConsumer mqConsumer;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        accessToken = null;
        executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName(getClass().getSimpleName() + "-loop"));
        tuyaIntegrationConfiguration = getClientConfiguration(configuration, TuyaIntegrationConfiguration.class);
        mqConsumer = createMqConsumer(tuyaIntegrationConfiguration.getAccessId(), tuyaIntegrationConfiguration.getAccessKey());
        mqConsumer.connect(false);
        this.executor.submit(() -> {
            try {
                mqConsumer.start();
            } catch (Exception e) {
                log.warn("[{}] During processing Tuya integration error caught!", configuration.getId(), e);
            }
        });
    }

    @Override
    public void destroy() {
        if (mqConsumer != null) {
            try {
                mqConsumer.stop();
            } catch (Exception e) {
                log.error("[{}] Cannot stop message queue consumer!", configuration.getId(), e);
            }
        }
        if (executor != null) {
            List<Runnable> runnables = executor.shutdownNow();
            log.debug("Stopped executor service, list of returned runnables: {}", runnables);
        }
    }

    @Override
    public void process(TuyaIntegrationMsg msg) {
        if (this.configuration.isEnabled()) {
            Exception exception = null;
            try {
                doProcess(msg);
                integrationStatistics.incMessagesProcessed();
            } catch (Exception e) {
                log.debug("[{}] Failed to apply data converter function: {}", configuration.getId(), e.getMessage(), e);
                exception = e;
                integrationStatistics.incErrorsOccurred();
            }
            try {
                persistDebug(this.context, "Uplink", UplinkContentType.JSON, msg.toString(), exception == null ? "OK" : "ERROR", exception);
            } catch (Exception e) {
                log.warn("[{}] Failed to persist debug message!", configuration.getId(), e);
            }
        }
    }

    private void doProcess(TuyaIntegrationMsg msg) throws Exception {
        byte[] data = mapper.writeValueAsBytes(msg.getJson());
        Map<String, String> metadataMap = msg.getDeviceMetadata();
        List<UplinkData> uplinkDataList = this.convertToUplinkDataList(this.context, data, new UplinkMetaData(UplinkContentType.JSON, metadataMap));
        if (uplinkDataList != null && !uplinkDataList.isEmpty()) {
            for (UplinkData uplinkData : uplinkDataList) {
                UplinkData uplinkDataResult = UplinkData.builder().deviceName(uplinkData.getDeviceName())
                        .deviceType(uplinkData.getDeviceType()).telemetry(uplinkData.getTelemetry())
                        .attributesUpdate(uplinkData.getAttributesUpdate()).customerName(uplinkData.getCustomerName())
                        .build();
                this.processUplinkData(this.context, uplinkDataResult);
            }
        }
    }

    @Override
    public void onDownlinkMsg(IntegrationDownlinkMsg downlink) {
        TbMsg msg = downlink.getTbMsg();
        logDownlink(context, "Downlink: " + msg.getType(), msg);
        if (downlinkConverter != null) {
            Exception exception = null;
            try {
                if (doProcessDownLinkMsg(msg)) {
                    integrationStatistics.incMessagesProcessed();
                }
            } catch (Exception e) {
                exception = e;
            }
            if (exception != null || configuration.isDebugMode()) {
                resultHandler("Downlink", msg.getData(), exception);
            }
        }
    }

    private void resultHandler(String type, String msg, Exception exception) {
        String status = exception == null ? "SUCCESS" : "FAILURE";
        if ("CONNECT".equals(type) && exception != null) {
            try {
                mqConsumer.stop();
            } catch (Exception ignored) {
            }
            this.executor.submit(() -> {
                try {
                    mqConsumer.start();
                } catch (Exception e) {
                    log.debug("[{}] During processing Tuya integration error caught!", configuration.getId(), e);
                }
            });
        }
        persistDebug(context, type, UplinkContentType.JSON, msg, status, exception);

    }

    private MqConsumer createMqConsumer(String accessId, String accessKey) {
        return MqConsumer.builder()
                .serviceUrl(tuyaIntegrationConfiguration.getRegion().getMessagingServerUrl())
                .accessId(accessId)
                .accessKey(accessKey)
                .env(tuyaIntegrationConfiguration.getEnv())
                .messageListener((incomingData) -> {
                    MessageVO vo = JacksonUtil.fromBytes(incomingData.getData(), MessageVO.class);
                    Map<String, String> metadata = new HashMap<>(metadataTemplate.getKvMap());
                    if (vo != null) {
                        String decryptedData = "";
                        try {
                            metadata.put("protocol", vo.getProtocol().toString());
                            metadata.put("pv", vo.getPv());
                            metadata.put("sign", vo.getSign());
                            metadata.put("t", vo.getT().toString());
                            metadata.put("topic", incomingData.getTopicName());
                            decryptedData = TuyaMessageUtil.decrypt(vo.getData(), accessKey.substring(8, 24));
                            JsonNode dataNode = JacksonUtil.fromString(decryptedData, JsonNode.class);
                            TuyaIntegrationMsg msg = new TuyaIntegrationMsg(dataNode, metadata);
                            this.process(msg);
                        } catch (Exception e) {
                            resultHandler("Uplink", decryptedData, e);
                        }
                    }
                })
                .resultHandler((this::resultHandler))
                .build();
    }

    private boolean doProcessDownLinkMsg(TbMsg msg) throws Exception {
        Map<String, String> mdMap = metadataTemplate.getKvMap();
        mdMap.putAll(msg.getMetaData().getData());
        List<DownlinkData> convertedDownlinkData = downlinkConverter.convertDownLink(context.getDownlinkConverterContext(), Collections.singletonList(msg), new IntegrationMetaData(mdMap));
        if (convertedDownlinkData == null || convertedDownlinkData.isEmpty()) {
            throw new RuntimeException("Data should contain commands array or code and value of the command");
        }
        Exception error = null;
        for (DownlinkData downlinkData : convertedDownlinkData) {
            try {
                String deviceId = getDeviceIdentifier(downlinkData.getMetadata());
                ObjectNode commandsNode = JacksonUtil.newObjectNode();
                ArrayNode arrayNode = JacksonUtil.OBJECT_MAPPER.createArrayNode();
                JsonNode data = JacksonUtil.fromBytes(downlinkData.getData());
                if (data instanceof ArrayNode) {
                    arrayNode = (ArrayNode) data;
                } else {
                    if (data.has("code")) {
                        arrayNode.add(data);
                    } else {
                        throw new RuntimeException("Downlink message format is not correct, downlink message should contain code field or commands array!");
                    }
                }
                commandsNode.set("commands", arrayNode);
                accessToken = getToken();
                if (accessToken == null) {
                    throw new RuntimeException("Cannot obtain access token from the server: " + tuyaIntegrationConfiguration.getRegion().getApiServerUrl());
                }
                sendCommands(deviceId, commandsNode);
            } catch (Exception e) {
                error = e;
            }
        }
        if (error != null) {
            throw error;
        }
        return true;
    }

    private TuyaToken getToken() throws Exception {
        return getToken(false);
    }

    private TuyaToken getToken(boolean force) throws Exception {
        boolean hasAccess = hasValidAccessToken();
        if (!force && hasAccess) {
            return accessToken;
        }
        Map<String, Object> queries = new HashMap<>();
        queries.put("grant_type", 1);
        HttpMethod httpMethod = HttpMethod.GET;
        RequestEntity<Object> requestEntity = createRequest(GET_TOKEN_URL_PATH, httpMethod, null, queries);
        ResponseEntity<ObjectNode> responseEntity = httpClient.exchange(requestEntity.getUrl(), httpMethod, requestEntity, ObjectNode.class);
        if (HttpStatus.OK.equals(responseEntity.getStatusCode()) && Objects.requireNonNull(responseEntity.getBody()).get("success").asBoolean()) {
            JsonNode result = responseEntity.getBody().get("result");
            Long expireAt = responseEntity.getBody().get("t").asLong() + result.get("expire_time").asLong() * 1000;
            return TuyaToken.builder()
                    .accessToken(result.get("access_token").asText())
                    .refreshToken(result.get("refresh_token").asText())
                    .uid(result.get("uid").asText())
                    .expireAt(expireAt)
                    .build();
        }
        return null;
    }

    private void sendCommands(String deviceId, ObjectNode commandsNode) throws Exception {
        String path = POST_COMMANDS_URL_PATH.replace("{deviceId}", deviceId);
        HttpMethod httpMethod = HttpMethod.POST;
        RequestEntity<Object> requestEntity = createRequest(path, httpMethod, commandsNode, null);
        ResponseEntity<ObjectNode> responseEntity = httpClient.exchange(requestEntity.getUrl(), httpMethod, requestEntity, ObjectNode.class);
        // If errors appear Tuya returns 200 OK and error in the body of the response.
        if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
            throw new RuntimeException(String.format("No response for device command request! Reason code from Tuya Cloud: %s", responseEntity.getStatusCode().toString()));
        }
        ObjectNode responseBody = responseEntity.getBody();
        if (responseBody != null && responseBody.has("code") && unauthorizedErrorCodes.contains(responseBody.get("code").asInt())) {
            accessToken = getToken(true);
            requestEntity = createRequest(path, httpMethod, commandsNode, null);
            responseEntity = httpClient.exchange(requestEntity.getUrl(), httpMethod, requestEntity, ObjectNode.class);
            responseBody = responseEntity.getBody();
        }
        if (responseBody != null && !responseBody.get("success").asBoolean()) {
            throw new RuntimeException(String.format("Downlink failed! with response from Tuya: %s", responseBody.get("msg")));
        }
    }

    private RequestEntity<Object> createRequest(String path, HttpMethod httpMethod, ObjectNode body, Map<String, Object> queries) throws Exception {
        boolean hasAccess = hasValidAccessToken();
        String ts = String.valueOf(System.currentTimeMillis());
        MultiValueMap<String, String> httpHeaders = createHeaders(ts);
        if (hasAccess) {
            httpHeaders.add("access_token", accessToken.getAccessToken());
        }
        String pathWithQueries = path;
        if (queries != null) {
            pathWithQueries += "?" + queries.entrySet().stream().map(it -> it.getKey() + "=" + it.getValue())
                    .collect(Collectors.joining("&"));
        }
        String strToSign;
        if (body != null) {
            strToSign = tuyaIntegrationConfiguration.getAccessId() + accessToken.getAccessToken() +
                    ts + stringToSign(pathWithQueries, body.toString(), httpMethod);
        } else {
            strToSign = tuyaIntegrationConfiguration.getAccessId() + ts + stringToSign(pathWithQueries, null, httpMethod);
        }
        String signedStr = sign(strToSign, tuyaIntegrationConfiguration.getAccessKey());
        httpHeaders.add("sign", signedStr);
        URI uri = URI.create(tuyaIntegrationConfiguration.getRegion().getApiServerUrl() + pathWithQueries +
                (hasAccess ? getAccessTokenPostfix(httpMethod) : ""));
        return new RequestEntity<>(body == null ? null : body.toString(), httpHeaders, httpMethod, uri);
    }

    private String stringToSign(String path, String body, HttpMethod httpMethod) throws Exception {
        List<String> lines = new ArrayList<>(16);
        lines.add(httpMethod.name());
        String bodyHash = EMPTY_HASH;
        if (body != null && body.length() > 0) {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(body.getBytes(StandardCharsets.UTF_8));
            bodyHash = Hex.encodeHexString(messageDigest.digest());
        }
        lines.add(bodyHash);
        lines.add("");
        lines.add(path);
        return String.join("\n", lines);
    }

    private String sign(String content, String secret) throws Exception {
        byte[] rawHmac = HmacSHA256Util.sign(content, secret.getBytes(StandardCharsets.UTF_8));
        return Hex.encodeHexString(rawHmac).toUpperCase();
    }

    private HttpHeaders createHeaders(String ts) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("client_id", tuyaIntegrationConfiguration.getAccessId());
        httpHeaders.add("t", ts);
        httpHeaders.add("sign_method", "HMAC-SHA256");
        httpHeaders.add("nonce", "");
        httpHeaders.add("Content-Type", "application/json");
        return httpHeaders;
    }

    private String getDeviceIdentifier(Map<String, String> metadata) {
        String deviceId = null;
        if (metadata.containsKey(DataConstants.DEVICE_ID)) {
            deviceId = metadata.get(DataConstants.DEVICE_ID);
        } else if (metadata.containsKey(DataConstants.DEVICE_NAME)) {
            deviceId = metadata.get(DataConstants.DEVICE_NAME);
        }
        if (deviceId == null) {
            throw new RuntimeException("Device identifier not found in metadata! Expected deviceId or deviceName parameters");
        }
        return deviceId;
    }

    private String getAccessTokenPostfix(HttpMethod httpMethod) {
        return (accessToken != null && HttpMethod.GET.equals(httpMethod) ? "/" + accessToken.getRefreshToken() : "");
    }

    private boolean hasValidAccessToken() {
        return accessToken != null && accessToken.getExpireAt() + 20_000 > System.currentTimeMillis();
    }
}
