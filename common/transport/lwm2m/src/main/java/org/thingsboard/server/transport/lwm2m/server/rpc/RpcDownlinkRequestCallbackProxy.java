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
package org.thingsboard.server.transport.lwm2m.server.rpc;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.codec.LwM2mValueConverter;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.downlink.DownlinkRequestCallback;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

public abstract class RpcDownlinkRequestCallbackProxy<R, T> implements DownlinkRequestCallback<R, T> {

    private final TransportService transportService;
    private final TransportProtos.ToDeviceRpcRequestMsg request;
    private final DownlinkRequestCallback<R, T> callback;

    protected final LwM2mClient client;
    protected final LwM2mValueConverter converter;

    public RpcDownlinkRequestCallbackProxy(TransportService transportService, LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, DownlinkRequestCallback<R, T> callback) {
        this.transportService = transportService;
        this.client = client;
        this.request = requestMsg;
        this.callback = callback;
        this.converter = LwM2mValueConverterImpl.getInstance();
    }

    @Override
    public void onSuccess(R request, T response) {
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
        sendRpcReplyOnError(e);
        if (callback != null) {
            callback.onError(params, e);
        }
    }

    protected void reply(LwM2MRpcResponseBody response) {
        TransportProtos.ToDeviceRpcResponseMsg msg = TransportProtos.ToDeviceRpcResponseMsg.newBuilder()
                .setPayload(JacksonUtil.toString(response))
                .setRequestId(request.getRequestId())
                .build();
        transportService.process(client.getSession(), msg, null);
    }

    abstract protected void sendRpcReplyOnSuccess(T response);

    protected void sendRpcReplyOnValidationError(String msg) {
        reply(LwM2MRpcResponseBody.builder().result(ResponseCode.BAD_REQUEST.getName()).error(msg).build());
    }

    protected void sendRpcReplyOnError(Exception e) {
        reply(LwM2MRpcResponseBody.builder().result(ResponseCode.INTERNAL_SERVER_ERROR.getName()).error(e.getMessage()).build());
    }

}
