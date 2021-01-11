/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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

import lombok.Data;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.client.RestTemplate;

@Data
public class LoriotTokenCredentials implements LoriotCredentials {

    private String token;

    @Override
    public void setInterceptor(RestTemplate restTemplate, String baseUrl) {
        restTemplate.getInterceptors().add((request, body, execution) -> {
            HttpRequest wrapper = new HttpRequestWrapper(request);
            wrapper.getHeaders().setBearerAuth(token);
            return execution.execute(wrapper, body);
        });
    }
}
