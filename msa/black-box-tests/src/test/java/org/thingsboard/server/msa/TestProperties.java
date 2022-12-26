/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.msa;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class TestProperties {
    private static final String HTTPS_URL = "https://localhost";

    private static final String WSS_URL = "wss://localhost";

    private static final ContainerTestSuite instance = ContainerTestSuite.getInstance();

    private static Properties properties;

    public static String getBaseUrl() {
        if (instance.isActive()) {
            return HTTPS_URL;
        }
        return getProperties().getProperty("tb.baseUrl");
    }

    public static String getWebSocketUrl() {
        if (instance.isActive()) {
            return WSS_URL;
        }
        return getProperties().getProperty("tb.wsUrl");
    }

    public static String getRemoteHttpUrl(){
        if (instance.isActive()) {
            String host = instance.getTestContainer().getServiceHost("tb-pe-http-integration", 8082);
            Integer port = instance.getTestContainer().getServicePort("tb-pe-http-integration", 8082);
            return "http://" + host + ":" + port;
        }
        return getProperties().getProperty("remote.httpUrl");
    }

    public static String getMqttBrokerUrl(){
        if (instance.isActive()) {
            String host = instance.getTestContainer().getServiceHost("broker", 1883);
            Integer port = instance.getTestContainer().getServicePort("broker", 1883);
            return "tcp://" + host + ":" + port;
        }
        return getProperties().getProperty("mqtt.broker");
    }

    public static String getRemoteCoapHost(){
        if (instance.isActive()) {
            return "localhost";
        }
        return getProperties().getProperty("remote.coap.host");
    }

    public static int getRemoteCoapPort(){
        if (instance.isActive()) {
            return 15683;
        }
        return Integer.parseInt(getProperties().getProperty("remote.coap.port"));
    }

    private static Properties getProperties() {
        if (properties == null) {
            try (InputStream input = TestProperties.class.getClassLoader().getResourceAsStream("config.properties")) {
                properties = new Properties();
                properties.load(input);
            } catch (IOException ex) {
                log.error("Exception while reading test properties " + ex.getMessage());
            }
        }
        return properties;
    }

}
