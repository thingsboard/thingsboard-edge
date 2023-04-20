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

import org.eclipse.leshan.core.request.LwM2mRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.downlink.DownlinkRequestCallback;

import java.util.Optional;

public abstract class RpcLwM2MDownlinkCallback<R extends LwM2mRequest<T>, T extends LwM2mResponse> extends RpcDownlinkRequestCallbackProxy<R, T> {

    public RpcLwM2MDownlinkCallback(TransportService transportService, LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, DownlinkRequestCallback<R, T> callback) {
        super(transportService, client, requestMsg, callback);
    }

    @Override
    protected void sendRpcReplyOnSuccess(T response) {
        LwM2MRpcResponseBody.LwM2MRpcResponseBodyBuilder builder = LwM2MRpcResponseBody.builder().result(response.getCode().getName());
        if (response.isSuccess()) {
            Optional<String> responseValue = serializeSuccessfulResponse(response);
            if (responseValue.isPresent() && StringUtils.isNotEmpty(responseValue.get())) {
                builder.value(responseValue.get());
            }
        } else {
            if (StringUtils.isNotEmpty(response.getErrorMessage())) {
                builder.error(response.getErrorMessage());
            }
        }
        reply(builder.build());
    }

    protected abstract Optional<String> serializeSuccessfulResponse(T response);
}
