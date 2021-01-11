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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EventId;
import org.thingsboard.server.common.data.id.TenantId;

import static org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo.getJson;
import static org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo.setJson;

/**
 * @author Andrew Shvayka
 */
@Data
@Slf4j
public class Event extends BaseData<EventId> {

    private TenantId tenantId;
    private String type;
    private String uid;
    private EntityId entityId;
    private transient JsonNode body;
    @JsonIgnore
    private byte[] bodyBytes;

    public Event() {
        super();
    }

    public Event(EventId id) {
        super(id);
    }

    public Event(Event event) {
        super(event);
        this.setBody(event.getBody());
    }

    public JsonNode getBody() {
        return getJson(() -> body, () -> bodyBytes);
    }

    public void setBody(JsonNode body) {
        setJson(body, json -> this.body = json, bytes -> this.bodyBytes = bytes);
    }
}
