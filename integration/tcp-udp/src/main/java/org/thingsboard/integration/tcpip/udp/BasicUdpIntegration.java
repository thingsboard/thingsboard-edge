/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.integration.tcpip.udp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.tcpip.AbstractIpIntegration;
import org.thingsboard.integration.tcpip.HandlerConfiguration;
import org.thingsboard.integration.tcpip.configs.TextHandlerConfiguration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Function;

@Slf4j
public class BasicUdpIntegration extends AbstractIpIntegration {

    private UdpConfigurationParameters udpConfigurationParameters;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        if (!this.configuration.isEnabled()) {
            return;
        }
        try {
            udpConfigurationParameters = mapper.readValue(mapper.writeValueAsString(configuration.getConfiguration().get("clientConfiguration")), UdpConfigurationParameters.class);
            workerGroup = new NioEventLoopGroup();
            startServer();
            log.info("UDP Server of [{}] started, BIND_PORT: [{}]", configuration.getName().toUpperCase(), udpConfigurationParameters.getPort());
        } catch (Exception e) {
            log.error("[{}] Integration exception while initialization UDP server: {}", configuration.getName().toUpperCase(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void bind() throws Exception {
        Bootstrap server = new Bootstrap().group(workerGroup);
        server.channel(NioDatagramChannel.class)
                .handler(createChannelHandlerInitializer(udpConfigurationParameters.getHandlerConfiguration()))
                .option(ChannelOption.SO_BROADCAST, udpConfigurationParameters.isSoBroadcast())
                .option(ChannelOption.SO_RCVBUF, udpConfigurationParameters.getSoRcvBuf() * 1024);
        serverChannel = server.bind(udpConfigurationParameters.getPort()).sync().channel();
        if (bindFuture != null) {
            bindFuture.cancel(true);
        }
    }

    @Override
    protected void doValidateConfiguration(JsonNode configuration, boolean allowLocalNetworkHosts) {
        UdpConfigurationParameters udpConfiguration;
        try {
            String stringUdpConfiguration = mapper.writeValueAsString(configuration.get("clientConfiguration"));
            udpConfiguration = mapper.readValue(stringUdpConfiguration, UdpConfigurationParameters.class);
            HandlerConfiguration handlerConfiguration = udpConfiguration.getHandlerConfiguration();
            if (handlerConfiguration == null) {
                throw new IllegalArgumentException("Handler Configuration is empty");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid UDP Integration Configuration structure! " + e.getMessage());
        }
        if (!allowLocalNetworkHosts) {
            throw new IllegalArgumentException("Usage of local network host for UDP Server connection is not allowed!");
        }
    }

    private ChannelInitializer<NioDatagramChannel> createChannelHandlerInitializer(HandlerConfiguration handlerConfig) {
        switch (handlerConfig.getHandlerType()) {
            case TEXT_PAYLOAD:
                return new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(final NioDatagramChannel channel) {
                        try {
                            TextHandlerConfiguration textHandlerConfiguration = (TextHandlerConfiguration) handlerConfig;
                            channel.pipeline()
                                    .addLast("datagramToStringDecoder", new AbstractUdpMsgDecoder<DatagramPacket, String>(msg -> msg.content().toString(Charset.forName(textHandlerConfiguration.getCharsetName()))) {
                                    })
                                    .addLast("udpStringHandler", new AbstractChannelHandler<String>(BasicUdpIntegration.this::writeValueAsBytes, StringUtils::isEmpty) {
                                    });
                        } catch (Exception e) {
                            log.error("Init Channel Exception: {}", e.getMessage(), e);
                            throw new RuntimeException(e);
                        }
                    }
                };
            case JSON_PAYLOAD:
                return new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(final NioDatagramChannel channel) throws Exception {
                        try {
                            channel.pipeline()
                                    .addLast("datagramToJsonDecoder", new AbstractUdpMsgDecoder<DatagramPacket, ObjectNode>(msg -> {
                                        try {
                                            return mapper.reader().readTree(new ByteArrayInputStream(toByteArray(msg.content()))).deepCopy();
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    }){})
                                    .addLast("udpJsonHandler", new AbstractChannelHandler<ObjectNode>(objectNode -> {
                                        try {
                                            return mapper.writer().writeValueAsBytes(objectNode);
                                        } catch (JsonProcessingException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }, objectNode -> objectNode == null || objectNode.isNull()) {});
                        } catch (Exception e) {
                            log.error("Init Channel Exception: {}", e.getMessage(), e);
                            throw new RuntimeException(e);
                        }
                    }
                };
            case BINARY_PAYLOAD:
                return new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(final NioDatagramChannel channel) {
                        try {
                            channel.pipeline()
                                    .addLast("datagramToByteDecoder", new AbstractUdpMsgDecoder<DatagramPacket, byte[]>(msg -> toByteArray(msg.content())) {
                                    })
                                    .addLast("udpByteHandler", new AbstractChannelHandler<byte[]>(byteArray -> byteArray, BasicUdpIntegration.this::isEmptyByteArray) {
                                    });
                        } catch (Exception e) {
                            log.error("Init Channel Exception: {}", e.getMessage(), e);
                            throw new RuntimeException(e);
                        }
                    }
                };
            case HEX_PAYLOAD:
                return new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(final NioDatagramChannel channel) throws Exception {
                        try {
                            channel.pipeline()
                                    .addLast("datagramToHexStringDecoder", new AbstractUdpMsgDecoder<DatagramPacket, ObjectNode>(msg -> getJsonHexReport(toByteArray(msg.content()))) {
                                    })
                                    .addLast("udpHexStringHandler", new AbstractChannelHandler<ObjectNode>(objectNode -> objectNode.toString().getBytes(), BasicUdpIntegration.this::isEmptyObjectNode) {
                                    });
                        } catch (Exception e) {
                            log.error("Init Channel Exception: {}", e.getMessage(), e);
                            throw new RuntimeException(e);
                        }
                    }
                };
            default:
                throw new RuntimeException("Unknown Channel Initializer");
        }
    }

    @AllArgsConstructor
    private abstract class AbstractUdpMsgDecoder<T, R> extends MessageToMessageDecoder<T> {

        private Function<T, R> transformer;

        @Override
        protected void decode(ChannelHandlerContext ctx, T msg, List<Object> out) throws Exception {
            try {
                out.add(transformer.apply(msg));
            } catch (Exception e) {
                log.error("[{}] Exception during of decoding message", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
    }

}
