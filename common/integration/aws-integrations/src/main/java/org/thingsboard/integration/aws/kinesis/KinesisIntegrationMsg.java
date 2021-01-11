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
package org.thingsboard.integration.aws.kinesis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;

import java.nio.ByteBuffer;

@Data
public class KinesisIntegrationMsg {

    private static ObjectMapper mapper = new ObjectMapper();

    private final String shardId;
    private final String sequenceNumber;
    private final String partitionKey;
    private final byte[] payload;

    public KinesisIntegrationMsg(String shardId, String sequenceNumber, ByteBuffer payload, String partitionKey) {
        this.shardId = shardId;
        this.sequenceNumber = sequenceNumber;
        this.payload = new byte[payload.remaining()];
        payload.get(this.payload);
        this.partitionKey = partitionKey;
    }

    public JsonNode toJson() {
        ObjectNode json = mapper.createObjectNode();
        json.put("shardId", shardId);
        json.put("sequenceNumber", sequenceNumber);
        json.put("partitionKey", partitionKey);
        JsonNode payloadJson = null;
        try {
            payloadJson = mapper.readTree(payload);
        } catch (Exception e) {}
        if (payloadJson != null) {
            json.set("payload", payloadJson);
        } else {
            json.put("payload", payload);
        }
        return json;
    }
}
