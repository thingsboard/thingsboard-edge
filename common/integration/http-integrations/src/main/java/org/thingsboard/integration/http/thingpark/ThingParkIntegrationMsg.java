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
package org.thingsboard.integration.http.thingpark;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.integration.api.controller.HttpIntegrationMsg;

import java.util.Map;

/**
 * Created by ashvayka on 18.12.17.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ThingParkIntegrationMsg extends HttpIntegrationMsg {

    private final ThingParkRequestParameters params;

    public ThingParkIntegrationMsg(Map<String, String> requestHeaders, JsonNode msg, ThingParkRequestParameters params,
                                   DeferredResult<ResponseEntity> callback) {
        super(requestHeaders, msg, callback);
        this.params = params;
    }

}
