/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.client.tools;

/**
 * @author Valerii Sosliuk
 * This class is intended for manual MQTT SSL Testing
 */

import com.google.common.io.Resources;
import org.eclipse.paho.client.mqttv3.*;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;

public class MqttSslClient {


    private static final String MQTT_URL = "ssl://localhost:1883";

    private static final String clientId = "MQTT_SSL_JAVA_CLIENT";
    private static final String accessToken = "C1_TEST_TOKEN";
    private static final String keyStoreFile = "mqttclient.jks";
    private static final String JKS="JKS";
    private static final String TLS="TLS";
    private static final String CLIENT_KEYSTORE_PASSWORD = "client_ks_password";
    private static final String CLIENT_KEY_PASSWORD = "client_key_password";

    public static void main(String[] args) {

        try {

            URL ksUrl = Resources.getResource(keyStoreFile);
            File ksFile = new File(ksUrl.toURI());
            URL tsUrl = Resources.getResource(keyStoreFile);
            File tsFile = new File(tsUrl.toURI());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

            KeyStore trustStore = KeyStore.getInstance(JKS);
            trustStore.load(new FileInputStream(tsFile), CLIENT_KEYSTORE_PASSWORD.toCharArray());
            tmf.init(trustStore);
            KeyStore ks = KeyStore.getInstance(JKS);

            ks.load(new FileInputStream(ksFile), CLIENT_KEYSTORE_PASSWORD.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, CLIENT_KEY_PASSWORD.toCharArray());

            KeyManager[] km = kmf.getKeyManagers();
            TrustManager[] tm = tmf.getTrustManagers();
            SSLContext sslContext = SSLContext.getInstance(TLS);
            sslContext.init(km, tm, null);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setSocketFactory(sslContext.getSocketFactory());
            MqttAsyncClient client = new MqttAsyncClient(MQTT_URL, clientId);
            client.connect(options);
            Thread.sleep(3000);
            MqttMessage message = new MqttMessage();
            message.setPayload("{\"key1\":\"value1\", \"key2\":true, \"key3\": 3.0, \"key4\": 4}".getBytes());
            client.publish("v1/devices/me/telemetry", message);
            client.disconnect();
            System.out.println("Disconnected");
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}