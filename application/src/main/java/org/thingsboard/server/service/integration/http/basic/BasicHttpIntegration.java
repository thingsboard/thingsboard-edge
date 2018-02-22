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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.thingsboard.server.service.converter.UplinkData;
import org.thingsboard.server.service.converter.UplinkMetaData;
import org.thingsboard.server.service.integration.IntegrationContext;
import org.thingsboard.server.service.integration.TbIntegrationInitParams;
import org.thingsboard.server.service.integration.http.AbstractHttpIntegration;
import org.thingsboard.server.service.integration.http.HttpIntegrationMsg;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
public class BasicHttpIntegration extends AbstractHttpIntegration<HttpIntegrationMsg> {

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
    protected HttpStatus doProcess(IntegrationContext context, HttpIntegrationMsg msg) throws Exception {
        if (checkSecurity(msg)) {
            byte[] data = mapper.writeValueAsBytes(msg.getMsg());
            Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
            msg.getRequestHeaders().forEach(
                    (header, value) -> {
                        mdMap.put("header:" + header, value);
                    }
            );
            List<UplinkData> uplinkDataList = convertToUplinkDataList(context, data, new UplinkMetaData(getUplinkContentType(), mdMap));
            if (uplinkDataList != null) {
                for (UplinkData uplinkData : uplinkDataList) {
                    processUplinkData(context, uplinkData);
                    log.info("[{}] Processing uplink data", uplinkData);
                }
            }
            return HttpStatus.OK;
        } else {
            return HttpStatus.FORBIDDEN;
        }
    }

    private boolean checkSecurity(HttpIntegrationMsg msg) throws Exception {
        if (securityEnabled) {
            Map<String, String> requestHeaders = msg.getRequestHeaders();
            log.trace("Validating request using the following request headers: {}", requestHeaders);
            for (Map.Entry<String,String> headerFilter : headersFilter.entrySet()) {
                String value = requestHeaders.get(headerFilter.getKey().toLowerCase());
                if (value == null || !value.equals(headerFilter.getValue())) {
                    return false;
                }
            }
        }
        return true;
    }

}
