/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.service.tcpip.tcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.service.tcpip.AbstractTcpipIntegrationTest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractTcpHandlerIntegrationTest extends AbstractTcpipIntegrationTest {

    private static final int SO_BACKLOG = 128;
    private static final int SO_RCV_BUF = 65535;
    private static final int SO_SND_BUF  = 65535;
    private static final boolean SO_KEEPALIVE = true;
    private static final boolean SO_REUSE_ADDR = true;
    private static final boolean TCP_NO_DELAY = true;
    private static final String CHARSET_NAME = "UTF-8";

    protected final SimpleTcpClient client = new SimpleTcpClient();

    protected final int port;

    @Override
    protected JsonNode createIntegrationConfiguration() {
        ObjectNode configuration = mapper.createObjectNode();
        configuration.put("port", port);
        configuration.put("soBacklogOption", SO_BACKLOG);
        configuration.put("soRcvBuf", SO_RCV_BUF);
        configuration.put("soSndBuf", SO_SND_BUF);
        configuration.put("soKeepaliveOption", SO_KEEPALIVE);
        configuration.put("tcpNoDelay", TCP_NO_DELAY);
        configuration.put("charsetName", CHARSET_NAME);
        configuration.set("metadata", mapper.createObjectNode());
        configuration.set("handlerConfiguration", createHandlerConfiguration());
        return configuration;
    }

    protected abstract ObjectNode createHandlerConfiguration();

    protected class SimpleTcpClient {

        private SocketChannel socket;
        private InetAddress inetAddress;

        SimpleTcpClient() {
            try {
                this.socket = SocketChannel.open();
            } catch (IOException e) {
                log.error("Exception during init Socket: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
            try {
                this.inetAddress = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                log.error("Unknown host Exception: {}", e.getMessage(),  e);
                throw new RuntimeException(e);
            }
        }

        public void connect(int port) {
            try {
                socket.connect(new InetSocketAddress(inetAddress.getHostAddress(), port));
            } catch (IOException e) {
                log.error("Exception during connection: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }

        public void sendData(byte[] data) {
            try {
                ByteBuffer buf = ByteBuffer.allocate(data.length + 1);
                buf.clear();
                buf.put(data);
                buf.flip();
                while (buf.hasRemaining()) {
                    socket.write(buf);
                }
            } catch (IOException e) {
                log.error("Input/Output Exception", e);
            }
        }

        public boolean isConnected() {
            return socket.isConnected();
        }
    }

}
