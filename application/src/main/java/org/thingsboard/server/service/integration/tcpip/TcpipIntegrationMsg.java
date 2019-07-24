package org.thingsboard.server.service.integration.tcpip;

import lombok.Data;

@Data
public class TcpipIntegrationMsg {
    private final byte[] msg;
}
