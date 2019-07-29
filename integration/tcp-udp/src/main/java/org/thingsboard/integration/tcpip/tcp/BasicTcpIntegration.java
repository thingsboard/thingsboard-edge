/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.tcpip.EventLoopGroupService;
import org.thingsboard.integration.tcpip.configs.BinaryHandlerConfiguration;
import org.thingsboard.integration.tcpip.configs.HexHandlerConfiguration;
import org.thingsboard.integration.tcpip.configs.TextHandlerConfiguration;
import org.thingsboard.integration.tcpip.AbstractTcpIpIntegration;
import org.thingsboard.integration.tcpip.HandlerConfiguration;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class BasicTcpIntegration extends AbstractTcpIpIntegration {

    private TcpConfigurationParameters tcpConfigurationParameters;

    private Channel serverChannel;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        try {
            tcpConfigurationParameters = mapper.readValue(mapper.writeValueAsString(configuration.getConfiguration()), TcpConfigurationParameters.class);
            EventLoopGroup bossGroup = EventLoopGroupService.BOSS_LOOP_GROUP;
            EventLoopGroup workerGroup = EventLoopGroupService.WORKER_LOOP_GROUP;
            ServerBootstrap server = new ServerBootstrap().group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, tcpConfigurationParameters.getSoBacklogOption())
                    .option(ChannelOption.SO_REUSEADDR, tcpConfigurationParameters.isSoReuseAddr())
                    .option(ChannelOption.SO_RCVBUF, tcpConfigurationParameters.getSoRcvBuf()) //65535
                    .childOption(ChannelOption.SO_KEEPALIVE, tcpConfigurationParameters.isSoKeepaliveOption())
                    .childOption(ChannelOption.TCP_NODELAY, tcpConfigurationParameters.isTcpNoDelay())
                    .childOption(ChannelOption.SO_SNDBUF, tcpConfigurationParameters.getSoSndBuf()) //65535
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(createChannelHandlerInitializer(tcpConfigurationParameters.getHandlerConfiguration()));
            if (serverChannel != null && serverChannel.isOpen()) {
                serverChannel.close();
            }
            serverChannel = server.bind(tcpConfigurationParameters.getPort()).sync().channel();
            log.info("TCP Server of '{}' started, BIND_PORT: {}", configuration.getName().toUpperCase(), tcpConfigurationParameters.getPort());
        } catch (Exception e) {
            log.error("[{}] Exception while initialization TCP server {}", tcpConfigurationParameters.getPort(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
        try {
            if (serverChannel != null) {
                serverChannel.close().sync();
            }
        } catch (Exception e) {
            log.error("Exception while closing of TCP channel", e);
            throw new RuntimeException(e);
        }
        log.info("TCP Server of '{}' on {} BIND_PORT stopped!", configuration.getName().toUpperCase(), tcpConfigurationParameters.getPort());
    }

    @Override
    protected void doValidateConfiguration(JsonNode configuration, boolean allowLocalNetworkHosts) {
        TcpConfigurationParameters tcpConfiguration;
        try {
            String stringTcpConfiguration = mapper.writeValueAsString(configuration);
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
            case "TEXT":
                return new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        TextHandlerConfiguration textHandlerConfig = (TextHandlerConfiguration) handlerConfig;
                        socketChannel.pipeline()
                                .addLast("delimiterBasedFrameDecoder", new DelimiterBasedFrameDecoder(
                                        textHandlerConfig.getMaxFrameLength(),
                                        textHandlerConfig.isStripDelimiter(),
                                        "systemLineSeparator".equals(textHandlerConfig.getMessageSeparator()) ? Delimiters.lineDelimiter() : Delimiters.nulDelimiter()))
                                .addLast("tcpStringChannelInboundHandler", new AbstractChannelHandler<ByteBuf>(BasicTcpIntegration.this::toByteArray, Objects::isNull) {
                                });
                    }
                };
            case "HEX":
                return new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        HexHandlerConfiguration hexHandlerConfig = (HexHandlerConfiguration) handlerConfig;
                        socketChannel.pipeline()
                                .addLast("tcpHexStringFrameDecoder", new TcpHexStringFrameDecoder(
                                        hexHandlerConfig.getMaxFrameLength(),0,0
                                ))
                                .addLast("tcpHexStringChannelInboundHandler", new AbstractChannelHandler<ObjectNode>(objectNode -> objectNode.toString().getBytes(), BasicTcpIntegration.this::isEmptyObjectNode) {
                                });
                    }
                };
            case "BINARY":
                return new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        BinaryHandlerConfiguration binaryHandlerConfig = (BinaryHandlerConfiguration) handlerConfig;
                        socketChannel.pipeline()
                                .addLast("tcpByteFrameDecoder", new TcpByteFrameDecoder(
                                        "littleEndian".equals(binaryHandlerConfig.getByteOrder()) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN,
                                        binaryHandlerConfig.getMaxFrameLength(),
                                        binaryHandlerConfig.getLengthFieldOffset(),
                                        binaryHandlerConfig.getLengthFieldLength(),
                                        binaryHandlerConfig.getLengthAdjustment(),
                                        binaryHandlerConfig.getInitialBytesToStrip(),
                                        binaryHandlerConfig.isFailFast()
                                ))
                                .addLast("tcpByteChannelInboundHandler", new AbstractChannelHandler<byte[]>(byteArray -> byteArray, BasicTcpIntegration.this::isEmptyByteArray) {
                                });
                    }
                };
            default:
                throw new RuntimeException("Unknown handler configuration type");
        }
    }

    private class TcpHexStringFrameDecoder extends AbstractTcpFrameDecoder {

        private int maxFrameLength;

        TcpHexStringFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength) {
            super(ByteOrder.BIG_ENDIAN, maxFrameLength, lengthFieldOffset, lengthFieldLength,
                    0, 0, false);
            this.maxFrameLength = maxFrameLength;
        }

        @Override
        protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
            Object decode = super.decode(ctx, in);
            return decode != null ? getJsonHexReport((byte[]) decode) : null;
        }

        @Override
        protected long getUnadjustedFrameLength(ByteBuf buf, int offset, int length, ByteOrder order) {
            return maxFrameLength;
        }
    }

    private class TcpByteFrameDecoder extends AbstractTcpFrameDecoder {

        private int maxFrameLength;

        TcpByteFrameDecoder(ByteOrder byteOrder, int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
                            int lengthAdjustment, int initialBytesToStrip, boolean failFast) {
            super(byteOrder, maxFrameLength, lengthFieldOffset, lengthFieldLength,
                    lengthAdjustment, initialBytesToStrip, failFast);
            this.maxFrameLength = maxFrameLength;
        }

        @Override
        protected long getUnadjustedFrameLength(ByteBuf buf, int offset, int length, ByteOrder order) {
            return length == 0 ? maxFrameLength : Long.parseLong(buf.toString(offset, length, Charset.forName(tcpConfigurationParameters.getCharsetName())));
        }
    }

    private abstract class AbstractTcpFrameDecoder extends LengthFieldBasedFrameDecoder {

        AbstractTcpFrameDecoder(ByteOrder byteOrder, int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
                                int lengthAdjustment, int initialBytesToStrip, boolean failFast) {
            super(byteOrder, maxFrameLength, lengthFieldOffset, lengthFieldLength,
                    lengthAdjustment, initialBytesToStrip, failFast);
        }

        @Override
        protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
            try {
                Optional<ByteBuf> frameOpt = Optional.ofNullable((ByteBuf) super.decode(ctx, in));
                if (frameOpt.isPresent()) {
                    ByteBuf frame = frameOpt.get();
                    return toByteArray(frame);
                }
                return null;
            } catch (Exception e) {
                log.error("Exception during decoding inbound message!", e);
                throw new Exception(e);
            }
        }
    }
}
