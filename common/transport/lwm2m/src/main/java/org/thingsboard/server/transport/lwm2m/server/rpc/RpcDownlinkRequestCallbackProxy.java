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
package org.thingsboard.server.transport.lwm2m.server.rpc;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.request.exception.ClientSleepingException;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.downlink.DownlinkRequestCallback;

import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Slf4j
public abstract class RpcDownlinkRequestCallbackProxy<R, T> implements DownlinkRequestCallback<R, T> {

    private final TransportService transportService;
    private final TransportProtos.ToDeviceRpcRequestMsg request;
    private final DownlinkRequestCallback<R, T> callback;

    protected final LwM2mClient client;

    public RpcDownlinkRequestCallbackProxy(TransportService transportService, LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, DownlinkRequestCallback<R, T> callback) {
        this.transportService = transportService;
        this.client = client;
        this.request = requestMsg;
        this.callback = callback;
    }

    @Override
    public boolean onSent(R request) {
        client.lock();
        try {
            UUID rpcId = new UUID(this.request.getRequestIdMSB(), this.request.getRequestIdLSB());
            if (rpcId.equals(client.getLastSentRpcId())) {
                log.debug("[{}]][{}] Rpc has already sent!", client.getEndpoint(), rpcId);
                return false;
            }
            client.setLastSentRpcId(rpcId);
        } finally {
            client.unlock();
        }
        transportService.process(client.getSession(), this.request, RpcStatus.SENT, TransportServiceCallback.EMPTY);
        return true;
    }

    @Override
    public void onSuccess(R request, T response) {
        transportService.process(client.getSession(), this.request, RpcStatus.DELIVERED, true, TransportServiceCallback.EMPTY);
        sendRpcReplyOnSuccess(response);
        if (callback != null) {
            callback.onSuccess(request, response);
        }
    }

    @Override
    public void onValidationError(String params, String msg) {
        sendRpcReplyOnValidationError(msg);
        if (callback != null) {
            callback.onValidationError(params, msg);
        }
    }

    @Override
    public void onError(String params, Exception e) {
        if (e instanceof TimeoutException || e instanceof org.eclipse.leshan.core.request.exception.TimeoutException) {
            client.setLastSentRpcId(null);
            transportService.process(client.getSession(), this.request, RpcStatus.TIMEOUT, TransportServiceCallback.EMPTY);
        } else if (!(e instanceof ClientSleepingException)) {
            sendRpcReplyOnError(e);
        }
        if (callback != null) {
            callback.onError(params, e);
        }
    }

    protected void reply(LwM2MRpcResponseBody response) {
        TransportProtos.ToDeviceRpcResponseMsg.Builder msg = TransportProtos.ToDeviceRpcResponseMsg.newBuilder().setRequestId(request.getRequestId());
        String responseAsString = JacksonUtil.toString(response);
        if (StringUtils.isEmpty(response.getError())) {
            msg.setPayload(responseAsString);
        } else {
            msg.setError(responseAsString);
        }
        transportService.process(client.getSession(), msg.build(), null);
    }

    abstract protected void sendRpcReplyOnSuccess(T response);

    protected void sendRpcReplyOnValidationError(String msg) {
        reply(LwM2MRpcResponseBody.builder().result(ResponseCode.BAD_REQUEST.getName()).error(msg).build());
    }

    protected void sendRpcReplyOnError(Exception e) {
        String error = e.getMessage();
        if (error == null) {
            error = e.toString();
        }
        reply(LwM2MRpcResponseBody.builder().result(ResponseCode.INTERNAL_SERVER_ERROR.getName()).error(error).build());
    }

}
