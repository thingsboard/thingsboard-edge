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
package org.thingsboard.integration.api.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Base64Utils;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.converter.Converter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * Created by ashvayka on 18.12.17.
 */
@Slf4j
public abstract class AbstractDataConverter implements TBDataConverter {

    protected final ObjectMapper mapper = new ObjectMapper();
    protected Converter configuration;

    @Override
    public void init(Converter configuration) {
        this.configuration = configuration;
    }

    @Override
    public String getName() {
        return configuration != null ? configuration.getName() : null;
    }

    protected String toString(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private String convertToString(String messageType, byte[] message) {
        if (message == null) {
            return null;
        }
        switch (messageType) {
            case "JSON":
            case "TEXT":
                return new String(message, StandardCharsets.UTF_8);
            case "BINARY":
                return Base64Utils.encodeToString(message);
            default:
                throw new RuntimeException("Message type: " + messageType + " is not supported!");
        }
    }

    protected void persistDebug(ConverterContext context, String type, String inMessageType, byte[] inMessage,
                                String outMessageType, byte[] outMessage, String metadata, Exception exception) {
        ObjectNode node = mapper.createObjectNode()
                .put("server", context.getServiceId())
                .put("type", type)
                .put("inMessageType", inMessageType)
                .put("in", convertToString(inMessageType, inMessage))
                .put("outMessageType", outMessageType)
                .put("out", convertToString(outMessageType, outMessage))
                .put("metadata", metadata);

        if (exception != null) {
            node = node.put("error", toString(exception));
        }
        context.saveEvent(DataConstants.DEBUG_CONVERTER, node, new DebugEventCallback());
    }

    private static class DebugEventCallback implements IntegrationCallback<Void> {

        @Override
        public void onSuccess(Void msg) {
            if (log.isDebugEnabled()) {
                log.debug("Event has been saved successfully!");
            }
        }

        @Override
        public void onError(Throwable e) {
            log.error("Failed to save the debug event!", e);
        }
    }
}
