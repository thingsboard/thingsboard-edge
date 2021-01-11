/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.integration.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.ByteBuf;
import lombok.Data;

import java.io.IOException;

/**
 * Created by ashvayka on 04.12.17.
 */
@Data
public class BasicMqttIntegrationMsg implements MqttIntegrationMsg {

    private static ObjectMapper mapper = new ObjectMapper();

    private final String topic;
    private final byte[] payload;

    public BasicMqttIntegrationMsg(String topic, ByteBuf payload) {
        this.topic = topic;
        this.payload = new byte[payload.readableBytes()];
        payload.readBytes(this.payload);
    }

    @Override
    public JsonNode toJson() {
        ObjectNode json = mapper.createObjectNode().put("topic", topic);
        JsonNode payloadJson = null;
        try {
            payloadJson = mapper.readTree(payload);
        } catch (IOException e) {
        }
        if (payloadJson != null) {
            json.set("payload", payloadJson);
        } else {
            json.put("payload", payload);
        }
        return json;
    }
}
