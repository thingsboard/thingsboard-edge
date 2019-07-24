package org.thingsboard.server.service.integration.tcpip.udp;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.server.service.integration.tcpip.HandlerConfiguration;

@Data
class UdpConfigurationParameters {
    private int port;
    private int soRcvBuf;
    private boolean soBroadcast;
    private String charsetName;
    private JsonNode metadata;
    private HandlerConfiguration handlerConfiguration;
}
