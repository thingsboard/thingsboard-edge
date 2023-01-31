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
package org.thingsboard.integration.http.thingpark;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.BaseEncoding;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.UnknownHttpStatusCodeException;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.integration.http.AbstractHttpIntegration;
import org.thingsboard.server.common.msg.TbMsg;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by ashvayka on 02.12.17.
 */
@Slf4j
@SuppressWarnings("deprecation")
public class ThingParkIntegration extends AbstractHttpIntegration<ThingParkIntegrationMsg> {

    private static final ThreadLocal<SimpleDateFormat> ISO8601 = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
    private static final String DEFAULT_DOWNLINK_URL = "https://api.thingpark.com/thingpark/lrc/rest/downlink";
    private static final String DEV_EUI = "DevEUI";
    private static final String F_PORT = "FPort";
    private static final String PAYLOAD = "Payload";
    private static final String F_CNT_DN = "FCntDn";
    private static final String CONFIRMED = "Confirmed";
    private static final String FLUSH_DOWNLINK_QUEUE = "FlushDownlinkQueue";

    private boolean securityEnabled = false;
    private String securityAsId;
    private String securityAsKey;
    private long maxTimeDiffInSeconds;
    private AsyncRestTemplate httpClient;
    private String downlinkUrl;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        JsonNode json = configuration.getConfiguration();
        securityEnabled = json.has("enableSecurity") && json.get("enableSecurity").asBoolean();
        if (securityEnabled) {
            securityAsId = json.get("asId").asText();
            securityAsKey = json.get("asKey").asText();
            maxTimeDiffInSeconds = json.get("maxTimeDiffInSeconds").asLong();
        }
        if (downlinkConverter != null) {
            downlinkUrl = json.has("downlinkUrl") ? json.get("downlinkUrl").asText() : DEFAULT_DOWNLINK_URL;
            Netty4ClientHttpRequestFactory nettyFactory = new Netty4ClientHttpRequestFactory(context.getEventLoopGroup());
            nettyFactory.setSslContext(SslContextBuilder.forClient().build());
            httpClient = new AsyncRestTemplate(nettyFactory);
        }
    }

    @Override
    protected ResponseEntity doProcess(ThingParkIntegrationMsg msg) throws Exception {
        if (checkSecurity(msg)) {
            List<UplinkData> uplinkDataList = convertToUplinkDataList(context, msg);
            if (uplinkDataList != null) {
                for (UplinkData data : uplinkDataList) {
                    processUplinkData(context, data);
                    log.trace("[{}] Processing uplink data", data);
                }
            }
            return fromStatus(HttpStatus.OK);
        } else {
            return fromStatus(HttpStatus.FORBIDDEN);
        }
    }

    @Override
    protected String getTypeUplink(ThingParkIntegrationMsg msg) {
        return "Uplink";
    }

    @Override
    public void onDownlinkMsg(IntegrationDownlinkMsg downlink) {
        TbMsg msg = downlink.getTbMsg();
        logDownlink(context, "Downlink: " + msg.getType(), msg);
        if (downlinkConverter != null) {
            processDownLinkMsg(context, msg);
        }
    }

    private void processDownLinkMsg(IntegrationContext context, TbMsg msg) {
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        String status;
        Exception exception;
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
                        throw new RuntimeException("DevEUI is missing in the downlink metadata!");
                    }
                    if (!metadata.containsKey(F_PORT)) {
                        throw new RuntimeException("FPort is missing in the downlink metadata!");
                    }
                    String payload = new String(downlink.getData(), StandardCharsets.UTF_8);
                    String params = DEV_EUI + "=" + metadata.get(DEV_EUI) + "&" + F_PORT + "=" + metadata.get(F_PORT) + "&" + PAYLOAD + "=" + payload;
                    if (metadata.containsKey(F_CNT_DN)) {
                        params += "&" + F_CNT_DN + "=" + metadata.containsKey(F_CNT_DN);
                    }
                    if (metadata.containsKey(CONFIRMED)) {
                        params += "&" + CONFIRMED + "=" + metadata.containsKey(CONFIRMED);
                    }
                    if (metadata.containsKey(FLUSH_DOWNLINK_QUEUE)) {
                        params += "&" + FLUSH_DOWNLINK_QUEUE + "=" + metadata.containsKey(FLUSH_DOWNLINK_QUEUE);
                    }
                    if (securityEnabled) {
                        params += "&AS_ID=" + securityAsId;
                        params += "&Time=" + ISO8601.get().format(new Date());
                        String token = getToken(params + securityAsKey);
                        params += "&Token=" + token;
                    }
                    ListenableFuture<ResponseEntity<String>> future = httpClient.postForEntity(downlinkUrl + "?" + params, new HttpEntity<>(""), String.class);
                    future.addCallback(new ListenableFutureCallback<>() {
                        @Override
                        public void onFailure(Throwable throwable) {
                            if (throwable instanceof UnknownHttpStatusCodeException) {
                                UnknownHttpStatusCodeException exception = (UnknownHttpStatusCodeException) throwable;
                                reportDownlinkError(context, msg, "ERROR", new Exception(exception.getResponseBodyAsString()));
                            } else {
                                reportDownlinkError(context, msg, "ERROR", new Exception(throwable));
                            }
                        }

                        @Override
                        public void onSuccess(ResponseEntity<String> voidResponseEntity) {
                            if (voidResponseEntity.getStatusCode().is2xxSuccessful()) {
                                reportDownlinkOk(context, downlink);
                            } else {
                                reportDownlinkError(context, msg, voidResponseEntity.getBody(), new RuntimeException());
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.warn("Failed to process downLink message", e);
            exception = e;
            status = "ERROR";
            reportDownlinkError(context, msg, status, exception);
        }
    }

    private boolean checkSecurity(ThingParkIntegrationMsg msg) throws Exception {
        if (securityEnabled) {
            ThingParkRequestParameters params = msg.getParams();
            log.trace("Validating request using following parameters: {}", params);

            if (!securityAsId.equalsIgnoreCase(params.getAsId())) {
                log.trace("Expected AS ID: {}, actual: {}", securityAsId, params.getAsId());
                return false;
            }

            long currentTs = System.currentTimeMillis();
            long requestTs = ISO8601.get().parse(params.getTime()).getTime();
            if (Math.abs(currentTs - requestTs) > TimeUnit.SECONDS.toMillis(maxTimeDiffInSeconds)) {
                log.trace("Request timestamp {} is out of sync with current server timestamp: {}", requestTs, currentTs);
                return false;
            }

            String queryParameters = "LrnDevEui=" + params.getLrnDevEui()
                    + "&LrnFPort=" + params.getLrnFPort()
                    + "&LrnInfos=" + params.getLrnInfos()
                    + "&AS_ID=" + params.getAsId()
                    + "&Time=" + params.getTime();


            JsonNode uplinkMsg = msg.getMsg().get("DevEUI_uplink");

            String bodyElements = uplinkMsg.get("CustomerID").asText() + uplinkMsg.get("DevEUI").asText()
                    + uplinkMsg.get("FPort").asText() + uplinkMsg.get("FCntUp").asText()
                    + uplinkMsg.get("payload_hex").asText();

            String tokenSrc = bodyElements + queryParameters + securityAsKey;

            log.trace("Validating request using following raw token: {}", tokenSrc);

            String token = getToken(tokenSrc);

            if (!token.equals(params.getToken())) {
                log.trace("Expected token: {}, actual: {}", token, params.getToken());
                return false;
            }
        }

        return true;
    }

    private String getToken(String tokenSrc) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(tokenSrc.getBytes(StandardCharsets.UTF_8));
        byte[] digest = md.digest();

        return BaseEncoding.base16().lowerCase().encode(digest);
    }

    private List<UplinkData> convertToUplinkDataList(IntegrationContext context, ThingParkIntegrationMsg msg) throws Exception {
        byte[] data = msg.getMsgInBytes();
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        ThingParkRequestParameters params = msg.getParams();
        mdMap.put("AS_ID", params.getAsId());
        mdMap.put("LrnDevEui", params.getLrnDevEui());
        mdMap.put("LrnFPort", params.getLrnFPort());
        return convertToUplinkDataList(context, data, new UplinkMetaData(getDefaultUplinkContentType(), mdMap));
    }

}
