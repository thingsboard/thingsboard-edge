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
package org.thingsboard.server.transport.coap.callback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.client.TbCoapClientState;
import org.thingsboard.server.transport.coap.client.TbCoapContentFormatUtil;
import org.thingsboard.server.transport.coap.client.TbCoapObservationState;

import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractSyncSessionCallback implements SessionMsgListener {

    protected final TbCoapClientState state;
    protected final CoapExchange exchange;
    protected final Request request;

    @Override
    public void onGetAttributesResponse(TransportProtos.GetAttributeResponseMsg getAttributesResponse) {
        logUnsupportedCommandMessage(getAttributesResponse);
    }

    @Override
    public void onAttributeUpdate(UUID sessionId, TransportProtos.AttributeUpdateNotificationMsg attributeUpdateNotification) {
        logUnsupportedCommandMessage(attributeUpdateNotification);
    }

    @Override
    public void onRemoteSessionCloseCommand(UUID sessionId, TransportProtos.SessionCloseNotificationProto sessionCloseNotification) {

    }

    @Override
    public void onDeviceDeleted(DeviceId deviceId) {

    }

    @Override
    public void onToDeviceRpcRequest(UUID sessionId, TransportProtos.ToDeviceRpcRequestMsg toDeviceRequest) {
        logUnsupportedCommandMessage(toDeviceRequest);
    }

    @Override
    public void onToServerRpcResponse(TransportProtos.ToServerRpcResponseMsg toServerResponse) {
        logUnsupportedCommandMessage(toServerResponse);
    }

    private void logUnsupportedCommandMessage(Object update) {
        log.trace("[{}] Ignore unsupported update: {}", state.getDeviceId(), update);
    }

    public static boolean isConRequest(TbCoapObservationState state) {
        if (state != null) {
            return state.getExchange().advanced().getRequest().isConfirmable();
        } else {
            return false;
        }
    }

    protected void respond(Response response) {
        response.getOptions().setContentFormat(TbCoapContentFormatUtil.getContentFormat(exchange.getRequestOptions().getContentFormat(), state.getContentFormat()));
        exchange.respond(response);
    }

}
