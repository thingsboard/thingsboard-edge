package org.thingsboard.server.service.integration.tcpip;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.service.integration.tcpip.configs.BinaryHandlerConfiguration;
import org.thingsboard.server.service.integration.tcpip.configs.HexHandlerConfiguration;
import org.thingsboard.server.service.integration.tcpip.configs.TextHandlerConfiguration;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME, property = "handlerType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextHandlerConfiguration.class, name = "TEXT"),
        @JsonSubTypes.Type(value = HexHandlerConfiguration.class, name = "HEX"),
        @JsonSubTypes.Type(value = BinaryHandlerConfiguration.class, name = "BINARY")
})
public interface HandlerConfiguration {

    int getMaxFrameLength();
    String getHandlerType();

}