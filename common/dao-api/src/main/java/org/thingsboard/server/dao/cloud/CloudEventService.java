package org.thingsboard.server.dao.cloud;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeSettings;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.List;

public interface CloudEventService {

    CloudEvent save(CloudEvent cloudEvent);

    PageData<CloudEvent> findCloudEvents(TenantId tenantId, TimePageLink pageLink);

    PageData<CloudEvent> findCloudEventsByEntityIdAndCloudEventActionAndCloudEventType(TenantId tenantId,
                                                                                       EntityId entityId,
                                                                                       CloudEventType cloudEventType,
                                                                                       String cloudEventAction,
                                                                                       TimePageLink pageLink);

    EdgeSettings findEdgeSettings(TenantId tenantId);

    ListenableFuture<List<Void>> saveEdgeSettings(TenantId tenantId, EdgeSettings edgeSettings);

    void deleteCloudEventsByTenantId(TenantId tenantId);

    void cleanupEvents(long ttl);
}