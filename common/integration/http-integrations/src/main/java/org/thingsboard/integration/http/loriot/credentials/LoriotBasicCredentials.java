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
package org.thingsboard.integration.http.loriot.credentials;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Data
public class LoriotBasicCredentials implements LoriotCredentials, ClientHttpRequestInterceptor {

    private String username;
    private String password;

    @JsonIgnore
    private String session;

    @JsonIgnore
    private RestTemplate restTemplate;

    @JsonIgnore
    private String baseUrl;

    @Override
    public void setInterceptor(RestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        refreshSession(restTemplate, baseUrl);
    }

    private void refreshSession(RestTemplate restTemplate, String baseUrl) {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("user", username);
        loginRequest.put("pwd", password);
        ResponseEntity<JsonNode> sessionInfo = restTemplate.postForEntity(baseUrl + "1/pub/login", loginRequest, JsonNode.class);
        if (sessionInfo.getStatusCode().equals(HttpStatus.OK)) {
            session = sessionInfo.getBody().get("session").asText();
            restTemplate.getInterceptors().add(this);
        } else {
            throw new RuntimeException(sessionInfo.getBody().get("error").asText());
        }
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        HttpRequest wrapper = new HttpRequestWrapper(request);
        wrapper.getHeaders().set(AUTH_HEADER_PARAM, "Session " + session);
        ClientHttpResponse response = execution.execute(wrapper, body);
        if (response.getStatusCode() == HttpStatus.FORBIDDEN) {
            synchronized (this) {
                restTemplate.getInterceptors().remove(this);
                refreshSession(restTemplate, baseUrl);
                wrapper.getHeaders().set(AUTH_HEADER_PARAM, "Session " + session);
                return execution.execute(wrapper, body);
            }
        }
        return response;
    }
}
