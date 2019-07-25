/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.integration.ophardt.basic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.http.HttpIntegrationMsg;
import org.thingsboard.integration.ophardt.data.ConverterResult;
import org.thingsboard.integration.ophardt.converter.OphardtConverter;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.gen.transport.PostAttributeMsg;
import org.thingsboard.server.gen.transport.PostTelemetryMsg;

import java.util.Map;
import java.util.UUID;

@Slf4j
public class OphardtIntegration extends AbstractIntegration<HttpIntegrationMsg> {

    private OphardtConverter converter;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        converter = new OphardtConverter();
    }

    @Override
    public void process(HttpIntegrationMsg msg) {
        String status;
        Exception exception = null;
        try {
            status = doProcess(msg);
        } catch (Exception e) {
            msg.getCallback().setResult(new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR));
            log.debug("Failed to apply data converter function: {}", e.getMessage(), e);
            exception = e;
            status = "ERROR";
        }
        if (!status.equals("OK")) {
            integrationStatistics.incErrorsOccurred();
        }
        if (configuration.isDebugMode()) {
            try {
                persistDebug(context, "Uplink", getUplinkContentType(), mapper.writeValueAsString(msg.getMsg()), status, exception);
            } catch (Exception e) {
                log.warn("Failed to persist debug message", e);
            }
        }
    }

    private String doProcess(HttpIntegrationMsg msg) {
        DeferredResult<ResponseEntity> callback = msg.getCallback();

        JsonNode rawData = msg.getMsg();
        UplinkData.UplinkDataBuilder builder = UplinkData.builder();
        Long mostSigBits;
        try {
            mostSigBits = rawData.get("submitterMessageReceiveTime").asLong();
            builder.deviceName(rawData.get("submitterUUID").asText());
            builder.deviceType(rawData.get("submitterDeviceType").asText());
            builder.telemetry(PostTelemetryMsg.newBuilder().getDefaultInstanceForType());
            builder.attributesUpdate(PostAttributeMsg.newBuilder().getDefaultInstanceForType());
        } catch (Exception e) {
            log.warn("Could not read one of the submitter fields!");
            callback.setResult(fromStatus("Could not read one of the submitter fields!", HttpStatus.BAD_REQUEST));
            return HttpStatus.BAD_REQUEST.name();
        }

        processUplinkData(context, builder.build());
        String uuidString = generateUUID(mostSigBits).toString();
        context.saveRawDataEvent(rawData.get("submitterUUID").asText(), DataConstants.RAW_DATA, uuidString, createEventBody(uuidString, rawData), new IntegrationDebugEventCallback());

        ConverterResult result;
        try {
            result = converter.convert(context, rawData, uuidString);
        } catch (Exception e) {
            log.warn("Could not convert request message to TB data!");
            callback.setResult(fromStatus("Could not convert request message to TB data!", HttpStatus.BAD_REQUEST));
            return HttpStatus.BAD_REQUEST.name();
        }

        UplinkData uplinkData = result.getUplinkData();
        if (uplinkData != null) {
            processUplinkData(context, uplinkData);
            log.info("[{}] Processing uplink data", uplinkData);
            processEntityViewCreation(context, uplinkData, uplinkData.getDeviceName() + "_View", uplinkData.getDeviceType());
            for (Map.Entry<UUID, JsonNode> entry : result.getEventsMap().entrySet()) {
                context.saveEventUidInCache(uplinkData.getDeviceName(), DataConstants.RAW_DATA, entry.getKey().toString());
                context.saveRawDataEvent(uplinkData.getDeviceName(), DataConstants.RAW_DATA, entry.getKey().toString(), createEventBody(entry.getKey().toString(), entry.getValue()), new IntegrationDebugEventCallback());
            }
        }
        callback.setResult(fromStatus(uuidString, HttpStatus.OK));
        return HttpStatus.OK.name();
    }

    private UUID generateUUID(Long mostSigBits) {
        return new UUID(mostSigBits, UUID.randomUUID().getLeastSignificantBits());
    }

    private ObjectNode createEventBody(String uuidString, JsonNode eventNode) {
        ObjectNode node = null;
        try {
            node = mapper.createObjectNode()
                    .put("messageType", getUplinkContentType())
                    .put("message", mapper.writeValueAsString(eventNode))
                    .put("uuid", uuidString);
        } catch (JsonProcessingException e) {
            log.error("Could not write node [{}] to string", eventNode, e);
        }
        return node;
    }

    private ResponseEntity fromStatus(String body, HttpStatus status) {
        return new ResponseEntity<>(body, status);
    }

    private static class IntegrationDebugEventCallback implements IntegrationCallback<Void> {
        @Override
        public void onSuccess(Void msg) {
            if (log.isDebugEnabled()) {
                log.debug("Event has been saved successfully!");
            }
        }

        @Override
        public void onError(Throwable e) {
            log.error("Failed to save the debug event!", e);
        }
    }
}
