package org.thingsboard.integration.mqtt;

import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.integration.api.data.UplinkContentType;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = BasicMqttIntegrationMsgTest.class)
public class BasicMqttIntegrationMsgTest {

    @Test
    public void testBasicMqttIntegrationMsgContentType() {
        byte[] jsonPayload = "{\"testKey\": \"testValue\"}".getBytes();
        BasicMqttIntegrationMsg message = createMessageWithPayload(jsonPayload);
        Assert.assertEquals(UplinkContentType.JSON, message.getContentType());

        byte[] textPayload = "{\"testKey\":\"testValue\"".getBytes();
        message = createMessageWithPayload(textPayload);
        Assert.assertEquals(UplinkContentType.TEXT, message.getContentType());

        byte[] binaryPayload = {0x01, 0x02, 0x03};
        message = createMessageWithPayload(binaryPayload);
        Assert.assertEquals(UplinkContentType.BINARY, message.getContentType());

        byte[] binaryPayloadButExpectedToGetTextType = {0x64, 0x65, 0x66};
        message = createMessageWithPayload(binaryPayloadButExpectedToGetTextType);
        Assert.assertEquals(UplinkContentType.TEXT, message.getContentType());
    }

    private static BasicMqttIntegrationMsg createMessageWithPayload(byte[] payload) {
        return new BasicMqttIntegrationMsg("test", Unpooled.wrappedBuffer(payload));
    }
}
