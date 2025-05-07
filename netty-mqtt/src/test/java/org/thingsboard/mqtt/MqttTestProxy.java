/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.mqtt;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.function.Predicate;

@Slf4j
public class MqttTestProxy {

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;

    private Channel clientToProxyChannel;
    private Channel proxyToBrokerChannel;

    private final int assignedPort;

    private boolean stopped;

    private final Predicate<MqttMessage> brokerToClientInterceptor;

    private MqttTestProxy(Builder builder) {
        log.info("Starting MQTT proxy...");

        brokerToClientInterceptor = builder.brokerToClientInterceptor != null ? builder.brokerToClientInterceptor : msg -> true;
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(1);

        ServerBootstrap proxyBootstrap = new ServerBootstrap();
        proxyBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        clientToProxyChannel = channel;
                        clientToProxyChannel.config().setAutoRead(false); // do not accept data before we connected to a broker

                        connectToBroker(builder.brokerHost, builder.brokerPort).addListener(future -> {
                            if (future.isSuccess()) {
                                clientToProxyChannel.pipeline().addLast("mqttDecoder", new MqttDecoder());
                                clientToProxyChannel.pipeline().addLast("mqttToBroker", new MqttRelayHandler(proxyToBrokerChannel, null));
                                clientToProxyChannel.pipeline().addLast("mqttEncoder", MqttEncoder.INSTANCE);

                                clientToProxyChannel.config().setAutoRead(true); // start accepting data for a client
                            } else {
                                log.error("Failed to connect to broker", future.cause());
                                clientToProxyChannel.close();
                            }
                        });
                    }
                });

        try {
            Channel proxyChannel = proxyBootstrap.bind(builder.localPort).sync().channel();
            assignedPort = ((InetSocketAddress) proxyChannel.localAddress()).getPort();
        } catch (Exception e) {
            log.error("Failed to start MQTT proxy", e);
            throw new RuntimeException("Failed to start MQTT proxy", e);
        }

        log.info("MQTT proxy started on port {}", assignedPort);
    }

    private ChannelFuture connectToBroker(String brokerHost, int brokerPort) {
        Bootstrap proxyToBrokerBootstrap = new Bootstrap();
        proxyToBrokerBootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        proxyToBrokerChannel = channel;
                        proxyToBrokerChannel.pipeline().addLast(new MqttDecoder());
                        proxyToBrokerChannel.pipeline().addLast("mqttToClient", new MqttRelayHandler(clientToProxyChannel, brokerToClientInterceptor));
                        proxyToBrokerChannel.pipeline().addLast(MqttEncoder.INSTANCE);
                    }
                });
        return proxyToBrokerBootstrap.connect(brokerHost, brokerPort);
    }

    private static class MqttRelayHandler extends SimpleChannelInboundHandler<MqttMessage> {

        private final Channel targetChannel;
        private final Predicate<MqttMessage> interceptor;

        private MqttRelayHandler(Channel targetChannel, Predicate<MqttMessage> interceptor) {
            this.targetChannel = targetChannel;
            this.interceptor = interceptor;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) {
            log.debug("Received message: {}", msg.fixedHeader().messageType());
            if (interceptor == null || interceptor.test(msg)) {
                if (targetChannel.isActive()) {
                    targetChannel.writeAndFlush(ReferenceCountUtil.retain(msg));
                }
            } else {
                log.info("Dropping message: {}", msg.fixedHeader().messageType());
            }
        }

    }

    public void stop() {
        if (stopped) {
            log.info("MQTT proxy was already stopped");
            return;
        }

        stopped = true;

        log.info("Stopping MQTT proxy...");

        if (clientToProxyChannel != null) {
            clientToProxyChannel.close();
        }
        if (proxyToBrokerChannel != null) {
            proxyToBrokerChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }

        log.info("MQTT proxy stopped");
    }

    public int getPort() {
        return assignedPort;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private int localPort;
        private String brokerHost;
        private int brokerPort;
        private Predicate<MqttMessage> brokerToClientInterceptor;

        public Builder localPort(int localPort) {
            this.localPort = localPort;
            return this;
        }

        public Builder brokerHost(String brokerHost) {
            this.brokerHost = brokerHost;
            return this;
        }

        public Builder brokerPort(int brokerPort) {
            this.brokerPort = brokerPort;
            return this;
        }

        public Builder brokerToClientInterceptor(Predicate<MqttMessage> interceptor) {
            this.brokerToClientInterceptor = interceptor;
            return this;
        }

        public MqttTestProxy build() {
            return new MqttTestProxy(this);
        }

    }
}
