package org.thingsboard.server.service.integration.tcpip.configs;

import lombok.Data;
import org.thingsboard.server.service.integration.tcpip.HandlerConfiguration;

@Data
public class HexHandlerConfiguration implements HandlerConfiguration {

    private int maxFrameLength;

    @Override
    public String getHandlerType() {
        return "HEX";
    }
}
