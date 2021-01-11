/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.integration.opcua;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.ToString;

import java.util.Map;

/**
 * Created by Valerii Sosliuk on 3/17/2018.
 */
@Data
@ToString(exclude = "payload")
public class OpcUaIntegrationMsg {

    private Map<String,String> deviceMetadata;
    private JsonNode json;
    private byte[] payload;

    OpcUaIntegrationMsg(JsonNode json, Map<String,String> deviceMetadata) {
        this.json = json;
        this.payload = json.toString().getBytes();
        this.deviceMetadata = deviceMetadata;
    }

    JsonNode toJson() {
        return json;
    }

    byte[] getPayload() {
        return payload;
    }
}
