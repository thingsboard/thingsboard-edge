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
package org.thingsboard.integration.api.converter;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.integration.api.IntegrationCallback;

/**
 * Created by ashvayka on 05.12.17.
 */
public interface ConverterContext {

    /**
     * Returns current server address that is used mostly for logging.
     *
     * @return server address
     */
    String getServiceId();

    /**
     * Saves event to ThingsBoard based on provided type and body on behalf of the converter
     */
    void saveEvent(String type, JsonNode body, IntegrationCallback<Void> callback);

}
