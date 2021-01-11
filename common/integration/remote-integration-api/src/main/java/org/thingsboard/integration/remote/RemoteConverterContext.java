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
package org.thingsboard.integration.remote;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.converter.ConverterContext;
import org.thingsboard.integration.storage.EventStorage;
import org.thingsboard.server.gen.integration.TbEventProto;
import org.thingsboard.server.gen.integration.TbEventSource;
import org.thingsboard.server.gen.integration.UplinkMsg;

@Data
@Slf4j
public class RemoteConverterContext implements ConverterContext {

    private final EventStorage eventStorage;
    private final boolean isUplink;
    private final ObjectMapper mapper;
    private final String clientId;
    private final int port;

    @Override
    public String getServiceId() {
        return "[" + clientId + ":" + port + "]";
    }

    @Override
    public void saveEvent(String type, JsonNode body, IntegrationCallback<Void> callback) {
        TbEventSource source;
        if (isUplink) {
            source = TbEventSource.UPLINK_CONVERTER;
        } else {
            source = TbEventSource.DOWNLINK_CONVERTER;
        }
        String eventData = "";
        try {
            eventData = mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            log.warn("[{}] Failed to convert event body!", body, e);
        }
        eventStorage.write(UplinkMsg.newBuilder()
                .addEventsData(TbEventProto.newBuilder()
                        .setSource(source)
                        .setType(type)
                        .setData(eventData)
                        .build()
                ).build(), callback);
    }
}
