package org.thingsboard.server.service.integration.tcpip.configs;

import lombok.Data;
import org.thingsboard.server.service.integration.tcpip.HandlerConfiguration;

@Data
public class TextHandlerConfiguration implements HandlerConfiguration {

    private int maxFrameLength;
    private boolean stripDelimiter;
    private String messageSeparator;

    @Override
    public String getHandlerType() {
        return "TEXT";
    }
}
