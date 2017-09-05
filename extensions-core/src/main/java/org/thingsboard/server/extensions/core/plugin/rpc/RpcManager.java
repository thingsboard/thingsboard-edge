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
package org.thingsboard.server.extensions.core.plugin.rpc;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.msg.*;
import org.thingsboard.server.extensions.core.plugin.rpc.handlers.RpcRestMsgHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class RpcManager {

    @Setter
    private RpcRestMsgHandler restHandler;

    private Map<UUID, LocalRequestMetaData> localRpcRequests = new HashMap<>();

    public void process(PluginContext ctx, LocalRequestMetaData requestMd) {
        ToDeviceRpcRequest request = requestMd.getRequest();
        log.trace("[{}] Processing local rpc call for device [{}]", request.getId(), request.getDeviceId());
        ctx.sendRpcRequest(request);
        localRpcRequests.put(request.getId(), requestMd);
        ctx.scheduleTimeoutMsg(new TimeoutUUIDMsg(request.getId(), request.getExpirationTime() - System.currentTimeMillis()));
    }

    public void process(PluginContext ctx, FromDeviceRpcResponse response) {
        UUID requestId = response.getId();
        LocalRequestMetaData md = localRpcRequests.remove(requestId);
        if (md != null) {
            log.trace("[{}] Processing local rpc response from device [{}]", requestId, md.getRequest().getDeviceId());
            restHandler.reply(ctx, md.getResponseWriter(), response);
        } else {
            log.trace("[{}] Unknown or stale rpc response received [{}]", requestId, response);
        }
    }

    public void process(PluginContext ctx, TimeoutMsg msg) {
        if (msg instanceof TimeoutUUIDMsg) {
            UUID requestId = ((TimeoutUUIDMsg) msg).getId();
            FromDeviceRpcResponse timeoutReponse = new FromDeviceRpcResponse(requestId, null, RpcError.TIMEOUT);
            LocalRequestMetaData md = localRpcRequests.remove(requestId);
            if (md != null) {
                log.trace("[{}] Processing rpc timeout for local device [{}]", requestId, md.getRequest().getDeviceId());
                restHandler.reply(ctx, md.getResponseWriter(), timeoutReponse);
            }
        }
    }
}
