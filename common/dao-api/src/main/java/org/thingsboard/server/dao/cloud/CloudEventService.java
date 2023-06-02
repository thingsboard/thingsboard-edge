/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.dao.cloud;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeSettings;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface CloudEventService {

    ListenableFuture<Void> saveAsync(CloudEvent cloudEvent);

    void saveCloudEvent(TenantId tenantId,
                        CloudEventType cloudEventType,
                        EdgeEventActionType cloudEventAction,
                        EntityId entityId,
                        JsonNode entityBody,
                        Long queueStartTs) throws ExecutionException, InterruptedException;

    ListenableFuture<Void> saveCloudEventAsync(TenantId tenantId,
                                               CloudEventType cloudEventType,
                                               EdgeEventActionType cloudEventAction,
                                               EntityId entityId,
                                               JsonNode entityBody,
                                               Long queueStartTs);

    PageData<CloudEvent> findCloudEvents(TenantId tenantId, Long seqIdOStart, Long seqIdEnd, TimePageLink pageLink);

    EdgeSettings findEdgeSettings(TenantId tenantId);

    ListenableFuture<List<String>> saveEdgeSettings(TenantId tenantId, EdgeSettings edgeSettings);

    void cleanupEvents(long ttl);
}