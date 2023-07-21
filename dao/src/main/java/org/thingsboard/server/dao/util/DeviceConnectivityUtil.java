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
package org.thingsboard.server.dao.util;

import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentials;

public class DeviceConnectivityUtil {

    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    public static final String MQTT = "mqtt";
    public static final String LINUX = "linux";
    public static final String WINDOWS = "windows";
    public static final String DOCKER = "docker";
    public static final String MQTTS = "mqtts";
    public static final String COAP = "coap";
    public static final String COAPS = "coaps";
    public static final String MQTT_SSL_PEM_FILE_NAME = "tb-server-chain.pem";
    public static final String CHECK_DOCUMENTATION = "Check documentation";
    public static final String JSON_EXAMPLE_PAYLOAD = "\"{temperature:25}\"";

    public static String getCurlCommand(String protocol, String host, String port, DeviceCredentials deviceCredentials) {
        return String.format("curl -v -X POST %s://%s%s/api/v1/%s/telemetry --header Content-Type:application/json --data " + JSON_EXAMPLE_PAYLOAD,
                protocol, host, port, deviceCredentials.getCredentialsId());
    }

    public static String getMosquittoPubPublishCommand(String protocol, String host, String port, String deviceTelemetryTopic, DeviceCredentials deviceCredentials) {
        StringBuilder command = new StringBuilder("mosquitto_pub -d -q 1");
        if (MQTTS.equals(protocol)) {
            command.append(" --cafile pathToFile/" + MQTT_SSL_PEM_FILE_NAME);
        }
        command.append(" -h ").append(host).append(port == null ? "" : " -p " + port);
        command.append(" -t ").append(deviceTelemetryTopic);

        switch (deviceCredentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                command.append(" -u ").append(deviceCredentials.getCredentialsId());
                break;
            case MQTT_BASIC:
                BasicMqttCredentials credentials = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(),
                        BasicMqttCredentials.class);
                if (credentials != null) {
                    if (credentials.getClientId() != null) {
                        command.append(" -i ").append(credentials.getClientId());
                    }
                    if (credentials.getUserName() != null) {
                        command.append(" -u ").append(credentials.getUserName());
                    }
                    if (credentials.getPassword() != null) {
                        command.append(" -P ").append(credentials.getPassword());
                    }
                } else {
                    return null;
                }
                break;
            default:
                return null;
        }
        command.append(" -m " + JSON_EXAMPLE_PAYLOAD);
        return command.toString();
    }

    public static String getDockerMosquittoClientsPublishCommand(String protocol, String host, String port, String deviceTelemetryTopic, DeviceCredentials deviceCredentials) {
        StringBuilder command = new StringBuilder("docker run");
        if (MQTTS.equals(protocol)) {
            command.append(" --volume pathToFile/" + MQTT_SSL_PEM_FILE_NAME + ":/tmp/" + MQTT_SSL_PEM_FILE_NAME);
        }
        command.append(" -it --rm thingsboard/mosquitto-clients pub");
        if (MQTTS.equals(protocol)) {
            command.append(" --cafile tmp/" + MQTT_SSL_PEM_FILE_NAME);
        }
        command.append(" -h ").append(host).append(port == null ? "" : " -p " + port);
        command.append(" -t ").append(deviceTelemetryTopic);

        switch (deviceCredentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                command.append(" -u ").append(deviceCredentials.getCredentialsId());
                break;
            case MQTT_BASIC:
                BasicMqttCredentials credentials = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(),
                        BasicMqttCredentials.class);
                if (credentials != null) {
                    if (credentials.getClientId() != null) {
                        command.append(" -i ").append(credentials.getClientId());
                    }
                    if (credentials.getUserName() != null) {
                        command.append(" -u ").append(credentials.getUserName());
                    }
                    if (credentials.getPassword() != null) {
                        command.append(" -P ").append(credentials.getPassword());
                    }
                } else {
                    return null;
                }
                break;
            default:
                return null;
        }
        command.append(" -m " + JSON_EXAMPLE_PAYLOAD);
        return command.toString();
    }

    public static String getCoapClientCommand(String protocol, String host, String port, DeviceCredentials deviceCredentials) {
        switch (deviceCredentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                String client = COAPS.equals(protocol) ? "coap-client-openssl" : "coap-client";
                return String.format("%s -m POST %s://%s%s/api/v1/%s/telemetry -t json -e %s",
                        client, protocol, host, port, deviceCredentials.getCredentialsId(), JSON_EXAMPLE_PAYLOAD);
            default:
                return null;
        }
    }
}
