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
package org.thingsboard.server.transport.mqtt.limits;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.transport.mqtt.MqttTransportContext;
import org.thingsboard.server.transport.mqtt.MqttTransportService;

import java.net.InetSocketAddress;

@Slf4j
public class ProxyIpFilter extends ChannelInboundHandlerAdapter {


    private MqttTransportContext context;

    public ProxyIpFilter(MqttTransportContext context) {
        this.context = context;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.trace("[{}] Received msg: {}", ctx.channel().id(), msg);
        if (msg instanceof HAProxyMessage) {
            HAProxyMessage proxyMsg = (HAProxyMessage) msg;
            if (proxyMsg.sourceAddress() != null && proxyMsg.sourcePort() > 0) {
                InetSocketAddress address = new InetSocketAddress(proxyMsg.sourceAddress(), proxyMsg.sourcePort());
                if (!context.checkAddress(address)) {
                    closeChannel(ctx);
                } else {
                    log.trace("[{}] Setting address: {}", ctx.channel().id(), address);
                    ctx.channel().attr(MqttTransportService.ADDRESS).set(address);
                    // We no longer need this channel in the pipeline. Similar to HAProxyMessageDecoder
                    ctx.pipeline().remove(this);
                }
            } else {
                log.trace("Received local health-check connection message: {}", proxyMsg);
                closeChannel(ctx);
            }
        }
    }

    private void closeChannel(ChannelHandlerContext ctx) {
        while (ctx.pipeline().last() != this) {
            ChannelHandler handler = ctx.pipeline().removeLast();
            if (handler instanceof ChannelInboundHandlerAdapter) {
                try {
                    ((ChannelInboundHandlerAdapter) handler).channelUnregistered(ctx);
                } catch (Exception e) {
                    log.error("Failed to unregister channel: [{}]", ctx, e);
                }
            }

        }
        ctx.pipeline().remove(this);
        ctx.close();
    }
}
