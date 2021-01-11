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
package org.thingsboard.integration.azure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.eventhubs.EventData;
import lombok.Data;

import java.io.IOException;
import java.util.Map;

@Data
public class AzureEventHubIntegrationMsg {

    private static ObjectMapper mapper = new ObjectMapper();

    private final EventData eventData;

    public AzureEventHubIntegrationMsg(EventData eventData) {
        this.eventData = eventData;
    }

    public byte[] getPayload() {
        return this.eventData.getBytes();
    }

    public Map<String, Object> getSystemProperties() {
        return this.eventData.getSystemProperties();
    }

    public JsonNode toJson() {
        ObjectNode json = mapper.createObjectNode();
        EventData.SystemProperties properties = this.eventData.getSystemProperties();
        ObjectNode sysPropsJson = mapper.createObjectNode();
        properties.forEach(
                (key, val) -> {
                    if (val != null) {
                        sysPropsJson.put(key, val.toString());
                    }
                }
        );
        json.set("systemProperties", sysPropsJson);
        JsonNode payloadJson = null;
        try {
            payloadJson = mapper.readTree(this.eventData.getBytes());
        } catch (IOException e) {
        }
        if (payloadJson != null) {
            json.set("payload", payloadJson);
        } else {
            json.put("payload", this.eventData.getBytes());
        }
        return json;
    }

}
