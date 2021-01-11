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
package org.thingsboard.integration.http.loriot.credentials;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Data
public class LoriotBasicCredentials implements LoriotCredentials {

    private String email;
    private String password;

    @JsonIgnore
    private String session;

    @Override
    public void setInterceptor(RestTemplate restTemplate, String baseUrl) {
        restTemplate.getInterceptors().add((request, body, execution) -> {
            HttpRequest wrapper = new HttpRequestWrapper(request);
            wrapper.getHeaders().set(AUTH_HEADER_PARAM, "Session " + session);
            ClientHttpResponse response = execution.execute(wrapper, body);
            if (response.getStatusCode() == HttpStatus.FORBIDDEN) {
                    restTemplate.getInterceptors().remove(this);
                    refreshSession(restTemplate, baseUrl);
                    wrapper.getHeaders().set(AUTH_HEADER_PARAM, "Session " + session);
                    return execution.execute(wrapper, body);
            }
            return response;
        });
    }

    private void refreshSession(RestTemplate restTemplate, String baseUrl) {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("user", email);
        loginRequest.put("pwd", password);
        ResponseEntity<JsonNode> sessionInfo = restTemplate.postForEntity(baseUrl + "1/pub/login", loginRequest, JsonNode.class);
        if (sessionInfo.getStatusCode().equals(HttpStatus.OK)) {
            session = sessionInfo.getBody().get("session").asText();
        } else {
            throw new RuntimeException(sessionInfo.getBody().asText());
        }
    }
}
