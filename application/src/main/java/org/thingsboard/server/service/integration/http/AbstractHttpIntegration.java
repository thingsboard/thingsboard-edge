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
package org.thingsboard.server.service.integration.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.msg.session.AdaptorToSessionActorMsg;
import org.thingsboard.server.common.msg.session.BasicAdaptorToSessionActorMsg;
import org.thingsboard.server.common.msg.session.BasicToDeviceActorSessionMsg;
import org.thingsboard.server.service.converter.ThingsboardDataConverter;
import org.thingsboard.server.service.converter.UplinkData;
import org.thingsboard.server.service.converter.UplinkMetaData;
import org.thingsboard.server.service.integration.AbstractIntegration;
import org.thingsboard.server.service.integration.ConverterContext;
import org.thingsboard.server.service.integration.IntegrationContext;
import org.thingsboard.server.service.integration.ThingsboardPlatformIntegration;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * Created by ashvayka on 04.12.17.
 */
@Slf4j
public abstract class AbstractHttpIntegration<T extends HttpIntegrationMsg> extends AbstractIntegration<T> {

    @Override
    public void process(IntegrationContext context, T msg) {
        String status = "OK";
        Exception exception = null;
        try {
            HttpStatus httpStatus = doProcess(context, msg);
            if (!httpStatus.is2xxSuccessful()) {
                status = httpStatus.name();
            }
            msg.getCallback().setResult(new ResponseEntity<>(httpStatus));
        } catch (Exception e) {
            msg.getCallback().setResult(new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR));
            log.warn("Failed to apply data converter function", e);
            exception = e;
            status = "ERROR";
        }
        if (configuration.isDebugMode()) {
            try {
                persistDebug(context, "Uplink", getUplinkContentType(), mapper.writeValueAsString(msg.getMsg()), status, exception);
            } catch (Exception e) {
                log.warn("Failed to persist debug message", e);
            }
        }
    }

    protected abstract HttpStatus doProcess(IntegrationContext context, T msg) throws Exception;

    protected void processUplinkData(IntegrationContext context, UplinkData data) {
        Device device = getOrCreateDevice(context, data);

        if (data.getTelemetry() != null) {
            AdaptorToSessionActorMsg msg = new BasicAdaptorToSessionActorMsg(new IntegrationHttpSessionCtx(), data.getTelemetry());
            context.getSessionMsgProcessor().process(new BasicToDeviceActorSessionMsg(device, msg));
        }

        if (data.getAttributesUpdate() != null) {
            AdaptorToSessionActorMsg msg = new BasicAdaptorToSessionActorMsg(new IntegrationHttpSessionCtx(), data.getAttributesUpdate());
            context.getSessionMsgProcessor().process(new BasicToDeviceActorSessionMsg(device, msg));
        }
    }

    private Device getOrCreateDevice(IntegrationContext context, UplinkData data) {
        Optional<Device> deviceOptional = context.getDeviceService().findDeviceByTenantIdAndName(configuration.getTenantId(), data.getDeviceName());
        Device device;
        if (!deviceOptional.isPresent()) {
            device = new Device();
            device.setName(data.getDeviceName());
            device.setType(data.getDeviceType());
            device.setTenantId(configuration.getTenantId());
            device = context.getDeviceService().saveDevice(device);
        } else {
            device = deviceOptional.get();
        }
        return device;
    }

    private void persistDebug(IntegrationContext context, String type, String messageType, String message, String status, Exception exception) {
        Event event = new Event();
        event.setTenantId(configuration.getTenantId());
        event.setEntityId(configuration.getId());
        event.setType(DataConstants.DEBUG_INTEGRATION);

        ObjectNode node = mapper.createObjectNode()
                .put("server", context.getDiscoveryService().getCurrentServer().getServerAddress().toString())
                .put("type", type)
                .put("messageType", messageType)
                .put("message", message)
                .put("status", status);

        if (exception != null) {
            node = node.put("error", toString(exception));
        }

        event.setBody(node);
        context.getEventService().save(event);
    }

    private String toString(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    protected List<UplinkData> convertToUplinkDataList(IntegrationContext context, byte[] data, UplinkMetaData md) throws Exception {
        return this.converter.convertUplink(context.getConverterContext(), data, md);
    }

}
