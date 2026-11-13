// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.cloud;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.concurrent.ExecutionException;

public interface CloudEventService {

    void saveCloudEvent(TenantId tenantId,
                        CloudEventType cloudEventType,
                        EdgeEventActionType cloudEventAction,
                        EntityId entityId,
                        JsonNode entityBody) throws ExecutionException, InterruptedException;

    ListenableFuture<Void> saveCloudEventAsync(TenantId tenantId,
                                               CloudEventType cloudEventType,
                                               EdgeEventActionType cloudEventAction,
                                               EntityId entityId,
                                               JsonNode entityBody);

    ListenableFuture<Void> saveAsync(CloudEvent cloudEvent);

    ListenableFuture<Void> saveTsKvAsync(CloudEvent cloudEvent);

    PageData<CloudEvent> findCloudEvents(TenantId tenantId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink);

    PageData<CloudEvent> findTsKvCloudEvents(TenantId tenantId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink);

}
