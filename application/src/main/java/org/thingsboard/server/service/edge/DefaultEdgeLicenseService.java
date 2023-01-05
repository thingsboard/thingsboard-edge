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
package org.thingsboard.server.service.edge;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.queue.util.TbCoreComponent;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.thingsboard.server.common.data.StringUtils.isNotEmpty;

@Service
@TbCoreComponent
@Slf4j
public class DefaultEdgeLicenseService implements EdgeLicenseService {

    private static final int CONNECT_TIMEOUT = 30000; // Default connect timeout in ms (30 seconds)
    private static final int READ_TIMEOUT = 30000; // Default read timeout in ms (30 seconds)

    private RestTemplate restTemplate;

    private static final String EDGE_LICENSE_SERVER_ENDPOINT = "https://license.thingsboard.io";

    @Value("${edges.enabled:false}")
    private boolean edgesEnabled;

    @PostConstruct
    public void init() {
        if (edgesEnabled) {
            this.restTemplate = initRestTemplate();
        }
    }

    @Override
    public ResponseEntity<JsonNode> checkInstance(JsonNode request) {
        return this.restTemplate.postForEntity(EDGE_LICENSE_SERVER_ENDPOINT + "/api/license/checkInstance", request, JsonNode.class);
    }

    @Override
    public ResponseEntity<JsonNode> activateInstance(String edgeLicenseSecret, String releaseDate) {
        Map<String, String> params = new HashMap<>();
        params.put("licenseSecret", edgeLicenseSecret);
        params.put("releaseDate", releaseDate);
        return this.restTemplate.postForEntity(EDGE_LICENSE_SERVER_ENDPOINT + "/api/license/activateInstance?licenseSecret={licenseSecret}&releaseDate={releaseDate}", null, JsonNode.class, params);
    }

    private RestTemplate initRestTemplate() {
        boolean jdkHttpClientEnabled = isNotEmpty(System.getProperty("tb.proxy.jdk")) && System.getProperty("tb.proxy.jdk").equalsIgnoreCase("true");
        boolean systemProxyEnabled = isNotEmpty(System.getProperty("tb.proxy.system")) && System.getProperty("tb.proxy.system").equalsIgnoreCase("true");
        boolean proxyEnabled = isNotEmpty(System.getProperty("tb.proxy.host")) && isNotEmpty(System.getProperty("tb.proxy.port"));
        CloseableHttpClient httpClient;
        if (jdkHttpClientEnabled) {
            log.warn("Going to use plain JDK Http Client!");
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            if (proxyEnabled) {
                log.warn("Going to use Proxy Server: [{}:{}]", System.getProperty("tb.proxy.host"), System.getProperty("tb.proxy.port"));
                factory.setProxy(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(System.getProperty("tb.proxy.host"), Integer.parseInt(System.getProperty("tb.proxy.port")))));
            }
            factory.setConnectTimeout(CONNECT_TIMEOUT);
            factory.setReadTimeout(READ_TIMEOUT);
            return new RestTemplate(factory);
        } else if (systemProxyEnabled) {
            log.warn("Going to use System Proxy Server!");
            httpClient = HttpClients.createSystem();
            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
            factory.setHttpClient(httpClient);
            factory.setConnectTimeout(CONNECT_TIMEOUT);
            factory.setReadTimeout(READ_TIMEOUT);
            return new RestTemplate(factory);
        } else if (proxyEnabled) {
            log.warn("Going to use Proxy Server: [{}:{}]", System.getProperty("tb.proxy.host"), System.getProperty("tb.proxy.port"));
            httpClient = HttpClients.custom()
                    .setSSLHostnameVerifier(new DefaultHostnameVerifier())
                    .setProxy(new HttpHost(System.getProperty("tb.proxy.host"), Integer.parseInt(System.getProperty("tb.proxy.port")), "https")).build();
            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
            factory.setHttpClient(httpClient);
            factory.setConnectTimeout(CONNECT_TIMEOUT);
            factory.setReadTimeout(READ_TIMEOUT);
            return new RestTemplate(factory);
        } else {
            httpClient = HttpClients.custom().setSSLHostnameVerifier(new DefaultHostnameVerifier()).build();
            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
            factory.setHttpClient(httpClient);
            factory.setConnectTimeout(CONNECT_TIMEOUT);
            factory.setReadTimeout(READ_TIMEOUT);
            return new RestTemplate(factory);
        }
    }
}


