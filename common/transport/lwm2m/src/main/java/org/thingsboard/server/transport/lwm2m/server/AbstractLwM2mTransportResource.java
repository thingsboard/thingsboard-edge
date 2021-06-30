/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.lwm2m.server;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.leshan.core.californium.LwM2mCoapResource;
import org.thingsboard.server.common.transport.TransportServiceCallback;

@Slf4j
public abstract class AbstractLwM2mTransportResource extends LwM2mCoapResource {

    public AbstractLwM2mTransportResource(String name) {
        super(name);
    }

    @Override
    public void handleGET(CoapExchange exchange) {
        processHandleGet(exchange);
    }

    @Override
    public void handlePOST(CoapExchange exchange) {
        processHandlePost(exchange);
    }

    protected abstract void processHandleGet(CoapExchange exchange);

    protected abstract void processHandlePost(CoapExchange exchange);

    public static class CoapOkCallback implements TransportServiceCallback<Void> {
        private final CoapExchange exchange;
        private final CoAP.ResponseCode onSuccessResponse;
        private final CoAP.ResponseCode onFailureResponse;

        public CoapOkCallback(CoapExchange exchange, CoAP.ResponseCode onSuccessResponse, CoAP.ResponseCode onFailureResponse) {
            this.exchange = exchange;
            this.onSuccessResponse = onSuccessResponse;
            this.onFailureResponse = onFailureResponse;
        }

        @Override
        public void onSuccess(Void msg) {
            Response response = new Response(onSuccessResponse);
            response.setAcknowledged(isConRequest());
            exchange.respond(response);
        }

        @Override
        public void onError(Throwable e) {
            exchange.respond(onFailureResponse);
        }

        private boolean isConRequest() {
            return exchange.advanced().getRequest().isConfirmable();
        }
    }

    public static class CoapNoOpCallback implements TransportServiceCallback<Void> {
        private final CoapExchange exchange;

        CoapNoOpCallback(CoapExchange exchange) {
            this.exchange = exchange;
        }

        @Override
        public void onSuccess(Void msg) {
        }

        @Override
        public void onError(Throwable e) {
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }


}
