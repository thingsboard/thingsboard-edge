/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
        configuration.put("soReuseAddr", SO_REUSE_ADDR);
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
