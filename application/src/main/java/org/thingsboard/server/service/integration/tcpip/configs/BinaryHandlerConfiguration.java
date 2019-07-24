package org.thingsboard.server.service.integration.tcpip.configs;

import lombok.Data;
import org.thingsboard.server.service.integration.tcpip.HandlerConfiguration;

@Data
public class BinaryHandlerConfiguration implements HandlerConfiguration {

    private String byteOrder;
    private int maxFrameLength;
    private int lengthFieldOffset;
    private int lengthFieldLength;
    private int lengthAdjustment;
    private int initialBytesToStrip;
    private boolean failFast;

    @Override
    public String getHandlerType() {
        return "BINARY";
    }
}
