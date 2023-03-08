/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.integration.azure;

import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;

import java.io.IOException;
import java.util.Map;

@Data
public class AzureServiceBusIntegrationMsg {

    private static ObjectMapper mapper = new ObjectMapper();

    private final ServiceBusReceivedMessageContext context;

    public AzureServiceBusIntegrationMsg(ServiceBusReceivedMessageContext context) {
        this.context = context;
    }

    public byte[] getPayload() {
        return this.context.getMessage().getBody().toBytes();
    }

    public Map<String, Object> getSystemProperties() {
        return this.context.getMessage().getApplicationProperties();
    }

    public JsonNode toJson() {
        ObjectNode json = mapper.createObjectNode();
        Map<String, Object> properties = this.context.getMessage().getApplicationProperties();
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
            payloadJson = mapper.readTree(this.context.getMessage().getBody().toBytes());
        } catch (IOException e) {
        }
        if (payloadJson != null) {
            json.set("payload", payloadJson);
        } else {
            json.put("payload", this.context.getMessage().getBody().toBytes());
        }
        return json;
    }

}
