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
package org.thingsboard.server.service.integration.thingpark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.BaseEncoding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.service.converter.ThingsboardDataConverter;
import org.thingsboard.server.service.converter.UplinkData;
import org.thingsboard.server.service.converter.UplinkMetaData;
import org.thingsboard.server.service.integration.IntegrationContext;
import org.thingsboard.server.service.integration.http.AbstractHttpIntegration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by ashvayka on 02.12.17.
 */
@Slf4j
public class ThingParkIntegration extends AbstractHttpIntegration<ThingParkIntegrationMsg> {

    private static final ThreadLocal<SimpleDateFormat> ISO8601 = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));

    private final ObjectMapper mapper = new ObjectMapper();
    private boolean securityEnabled = false;
    private String securityAsId;
    private String securityAsKey;
    private long maxTimeDiffInSeconds;

    @Override
    public void init(IntegrationContext context, Integration dto, ThingsboardDataConverter converter) throws Exception {
        super.init(context, dto, converter);
        JsonNode json = configuration.getConfiguration();
        securityEnabled = json.has("enableSecurity") && json.get("enableSecurity").asBoolean();
        if (securityEnabled) {
            securityAsId = json.get("asId").asText();
            securityAsKey = json.get("asKey").asText();
            maxTimeDiffInSeconds = json.get("maxTimeDiffInSeconds").asLong();
        }
    }

    @Override
    protected HttpStatus doProcess(IntegrationContext context, ThingParkIntegrationMsg msg) throws Exception {
        if (checkSecurity(msg)) {
            List<UplinkData> uplinkDataList = convertToUplinkDataList(context, msg);
            if (uplinkDataList != null) {
                for (UplinkData data : uplinkDataList) {
                    processUplinkData(context, data);
                    log.info("[{}] Processing uplink data", data);
                }
            }
            return HttpStatus.OK;
        } else {
            return HttpStatus.FORBIDDEN;
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

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(tokenSrc.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();

            String token = BaseEncoding.base16().lowerCase().encode(digest);

            if (!token.equals(params.getToken())) {
                log.trace("Expected token: {}, actual: {}", token, params.getToken());
                return false;
            }

        }

        return true;
    }

    private List<UplinkData> convertToUplinkDataList(IntegrationContext context, ThingParkIntegrationMsg msg) throws Exception {
        byte[] data = mapper.writeValueAsBytes(msg.getMsg());
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        ThingParkRequestParameters params = msg.getParams();
        mdMap.put("AS_ID", params.getAsId());
        mdMap.put("LrnDevEui", params.getLrnDevEui());
        mdMap.put("LrnFPort", params.getLrnFPort());
        return convertToUplinkDataList(context, data, new UplinkMetaData(getUplinkContentType(), mdMap));
    }

}
