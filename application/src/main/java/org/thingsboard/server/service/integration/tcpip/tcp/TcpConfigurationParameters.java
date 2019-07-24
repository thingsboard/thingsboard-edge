package org.thingsboard.server.service.integration.tcpip.tcp;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.server.service.integration.tcpip.HandlerConfiguration;

@Data
class TcpConfigurationParameters {
    private int port;
    private int soBacklogOption;
    private int soRcvBuf;
    private int soSndBuf;
    private boolean soKeepaliveOption;
    private boolean soReuseAddr;
    private boolean tcpNoDelay;
    private String charsetName;
    private JsonNode metadata;
    private HandlerConfiguration handlerConfiguration;
}
