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
package org.thingsboard.integration.tcpip.tcp;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.tcpip.AbstractIpIntegration;
import org.thingsboard.integration.tcpip.HandlerConfiguration;
import org.thingsboard.integration.tcpip.configs.BinaryHandlerConfiguration;
import org.thingsboard.integration.tcpip.configs.TextHandlerConfiguration;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Slf4j
public class BasicTcpIntegration extends AbstractIpIntegration {

    private TcpConfigurationParameters tcpConfigurationParameters;

    private static final String SYSTEM_LINE_SEPARATOR = "SYSTEM_LINE_SEPARATOR";
    private static final String LITTLE_ENDIAN_BYTE_ORDER = "LITTLE_ENDIAN";

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        try {
            tcpConfigurationParameters = mapper.readValue(mapper.writeValueAsString(configuration.getConfiguration().get("clientConfiguration")), TcpConfigurationParameters.class);
            setUplinkContentTypeFromHandlerType(tcpConfigurationParameters.getHandlerConfiguration().getHandlerType());
            bossGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();
            startServer();
            log.info("TCP Server of [{}] started, BIND_PORT: [{}]", configuration.getName().toUpperCase(), tcpConfigurationParameters.getPort());
        } catch (Exception e) {
            log.error("[{}] Integration exception while initialization TCP server: {}", configuration.getName().toUpperCase(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void bind() throws Exception {
        ServerBootstrap server = new ServerBootstrap().group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, tcpConfigurationParameters.getSoBacklogOption())
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_RCVBUF, tcpConfigurationParameters.getSoRcvBuf() * 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, tcpConfigurationParameters.isSoKeepaliveOption())
                .childOption(ChannelOption.TCP_NODELAY, tcpConfigurationParameters.isTcpNoDelay())
                .childOption(ChannelOption.SO_SNDBUF, tcpConfigurationParameters.getSoSndBuf() * 1024)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(createChannelHandlerInitializer(tcpConfigurationParameters.getHandlerConfiguration()));
        serverChannel = server.bind(tcpConfigurationParameters.getPort()).sync().channel();
        if (bindFuture != null) {
            bindFuture.cancel(true);
        }
    }

    @Override
    protected void doValidateConfiguration(JsonNode configuration, boolean allowLocalNetworkHosts) {
        TcpConfigurationParameters tcpConfiguration;
        try {
            String stringTcpConfiguration = mapper.writeValueAsString(configuration.get("clientConfiguration"));
            tcpConfiguration = mapper.readValue(stringTcpConfiguration, TcpConfigurationParameters.class);
            HandlerConfiguration handlerConfiguration = tcpConfiguration.getHandlerConfiguration();
            if (handlerConfiguration == null) {
                throw new IllegalArgumentException("Handler Configuration is empty");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid TCP Integration Configuration structure! " + e.getMessage());
        }
        if (!allowLocalNetworkHosts) {
            throw new IllegalArgumentException("Usage of local network host for TCP Server connection is not allowed!");
        }
    }

    private ChannelInitializer<SocketChannel> createChannelHandlerInitializer(HandlerConfiguration handlerConfig) {
        switch (handlerConfig.getHandlerType()) {
            case TEXT_PAYLOAD:
                return new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        TextHandlerConfiguration textHandlerConfig = (TextHandlerConfiguration) handlerConfig;
                        ByteBuf[] delimiters = SYSTEM_LINE_SEPARATOR.equals(textHandlerConfig.getMessageSeparator()) ? Delimiters.lineDelimiter() : Delimiters.nulDelimiter();
                        DelimiterBasedFrameDecoder framer = new DelimiterBasedFrameDecoder(
                                textHandlerConfig.getMaxFrameLength(),
                                textHandlerConfig.isStripDelimiter(),
                                delimiters);
                        socketChannel.pipeline()
                                .addLast("framer", framer)
                                .addLast("tcpTextDecoder", new BasicTcpIntegration.AbstractTcpMsgDecoder<ByteBuf, byte[]>(BasicTcpIntegration.this::toByteArray){})
                                .addLast("tcpStringHandler", new AbstractChannelHandler<byte[]>(msg -> msg, Objects::isNull) {});
                    }
                };
            case JSON_PAYLOAD:
                return new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        try {
                            socketChannel.pipeline()
                                    .addLast("datagramToJsonDecoder", new JsonObjectDecoder())
                                    .addLast("tcpJsonDecoder", new BasicTcpIntegration.AbstractTcpMsgDecoder<ByteBuf, byte[]>(BasicTcpIntegration.this::toByteArray){})
                                    .addLast("tcpJsonHandler", new AbstractChannelHandler<byte[]>(msg -> msg, Objects::isNull) {});
                        } catch (Exception e) {
                            log.error("Init Channel Exception: {}", e.getMessage(), e);
                            throw new RuntimeException(e);
                        }
                    }
                };

            case BINARY_PAYLOAD:
                return new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        BinaryHandlerConfiguration binaryHandlerConfig = (BinaryHandlerConfiguration) handlerConfig;
                        ByteOrder byteOrder = LITTLE_ENDIAN_BYTE_ORDER.equals(binaryHandlerConfig.getByteOrder()) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
                        LengthFieldBasedFrameDecoder framer = new LengthFieldBasedFrameDecoder(
                                byteOrder,
                                binaryHandlerConfig.getMaxFrameLength(),
                                binaryHandlerConfig.getLengthFieldOffset(),
                                binaryHandlerConfig.getLengthFieldLength(),
                                binaryHandlerConfig.getLengthAdjustment(),
                                binaryHandlerConfig.getInitialBytesToStrip(),
                                binaryHandlerConfig.isFailFast()
                        );
                        socketChannel.pipeline()
                                .addLast("tcpByteDecoder", framer)
                                .addLast("tcpByteDecoderOverride", new BasicTcpIntegration.AbstractTcpMsgDecoder<ByteBuf, byte[]>(BasicTcpIntegration.this::toByteArray){})
                                .addLast("tcpByteHandler", new AbstractChannelHandler<byte[]>(msg -> msg, Objects::isNull) {});
                    }
                };
            default:
                throw new RuntimeException("Unknown handler configuration type");
        }
    }

    @Override
    protected void sendDownlink(ChannelHandlerContext deviceCtx, String entityName) {
        if (!deviceCtx.isRemoved()) {
            for (DownlinkData downlinkData : devicesDownlinkData.get(entityName)) {
                deviceCtx.write(Unpooled.wrappedBuffer(downlinkData.getData()));
                deviceCtx.write(Unpooled.wrappedBuffer("\n".getBytes()));
            }
            deviceCtx.flush();
            devicesDownlinkData.remove(entityName);
        } else {
            connectedDevicesContexts.remove(entityName);
        }
    }

    @AllArgsConstructor
    private abstract class AbstractTcpMsgDecoder<T, R> extends MessageToMessageDecoder<T> {

        private Function<T, R> transformer;

        @Override
        protected void decode(ChannelHandlerContext ctx, T msg, List<Object> out) throws Exception {
            try {
                out.add(new RawIpIntegrationMsg<R>(ctx.pipeline().channel().remoteAddress(), transformer.apply(msg)));
            } catch (Exception e) {
                log.error("[{}] Exception during of decoding message", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
    }
}
