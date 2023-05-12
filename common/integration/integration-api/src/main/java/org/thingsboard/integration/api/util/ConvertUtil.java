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
package org.thingsboard.integration.api.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.util.Base64Utils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.UplinkContentType;

import java.nio.charset.StandardCharsets;

public class ConvertUtil {

    public static String toDebugMessage(UplinkContentType messageType, byte[] message) {
        return toDebugMessage(messageType.name(), message);
    }

    public static String toDebugMessage(String messageType, byte[] message) {
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
    public static void putJson(ObjectNode root, byte[] payload) {
        putJson(root, payload, "JSON");
    }

    public static void putJson(ObjectNode root, byte[] payload, String onFailMessageType) {
        try {
            JsonNode payloadJson = JacksonUtil.fromBytes(payload);
            root.set("payload", payloadJson);
        } catch (IllegalArgumentException e) {
            root.put("payload", toDebugMessage(onFailMessageType, payload));
        }
    }

}
