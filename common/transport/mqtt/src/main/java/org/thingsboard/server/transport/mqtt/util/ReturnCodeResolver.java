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
package org.thingsboard.server.transport.mqtt.util;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttVersion;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReturnCodeResolver {

    public static MqttConnectReturnCode getConnectionReturnCode(MqttVersion mqttVersion, ReturnCode returnCode) {
        if (!MqttVersion.MQTT_5.equals(mqttVersion) && !ReturnCode.SUCCESS.equals(returnCode)) {
            switch (returnCode) {
                case BAD_USERNAME_OR_PASSWORD:
                case NOT_AUTHORIZED_5:
                    return MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED;
                case SERVER_UNAVAILABLE_5:
                    return MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE;
                case CLIENT_IDENTIFIER_NOT_VALID:
                    return MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED;
                default:
                    log.warn("Unknown return code for conversion: {}", returnCode.name());
            }
        }
        return MqttConnectReturnCode.valueOf(returnCode.byteValue());
    }

    public static int getSubscriptionReturnCode(MqttVersion mqttVersion, ReturnCode returnCode) {
        if (!MqttVersion.MQTT_5.equals(mqttVersion) && !ReturnCode.SUCCESS.equals(returnCode)) {
            switch (returnCode) {
                case UNSPECIFIED_ERROR:
                case TOPIC_FILTER_INVALID:
                case IMPLEMENTATION_SPECIFIC:
                case NOT_AUTHORIZED_5:
                case PACKET_IDENTIFIER_IN_USE:
                case QUOTA_EXCEEDED:
                case SHARED_SUBSCRIPTION_NOT_SUPPORTED:
                case SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED:
                case WILDCARD_SUBSCRIPTION_NOT_SUPPORTED:
                    return MqttQoS.FAILURE.value();
            }
        }
        return returnCode.byteValue();
    }
}
