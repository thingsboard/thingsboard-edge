package org.thingsboard.integration.mqtt.messages;

import io.netty.buffer.ByteBuf;
import org.thingsboard.integration.api.data.UplinkContentType;

public class TextMqttIntegrationMsg extends BasicMqttIntegrationMsg {

    public TextMqttIntegrationMsg(String topic, ByteBuf payload) {
        super(topic, payload);
    }

    @Override
    public UplinkContentType getContentType() {
        return UplinkContentType.TEXT;
    }
}
