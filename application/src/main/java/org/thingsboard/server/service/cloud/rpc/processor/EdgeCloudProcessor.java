/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@TbCoreComponent
public class EdgeCloudProcessor extends BaseEdgeProcessor {

    private final Lock edgeCreationLock = new ReentrantLock();

    public ListenableFuture<Void> processEdgeConfigurationMsgFromCloud(TenantId tenantId, EdgeConfiguration edgeConfiguration) {
        EdgeId edgeId = new EdgeId(new UUID(edgeConfiguration.getEdgeIdMSB(), edgeConfiguration.getEdgeIdLSB()));
        edgeCreationLock.lock();
        try {
            cloudSynchronizationManager.getSync().set(true);
            Edge edge = edgeCtx.getEdgeService().findEdgeById(tenantId, edgeId);
            if (edge == null) {
                edge = new Edge();
                edge.setId(edgeId);
                edge.setTenantId(tenantId);
                edge.setCreatedTime(Uuids.unixTimestamp(edgeId.getId()));
            }
            CustomerId customerId = safeGetCustomerId(edgeConfiguration.getCustomerIdMSB(), edgeConfiguration.getCustomerIdLSB());
            edge.setCustomerId(customerId);
            edge.setName(edgeConfiguration.getName());
            edge.setType(edgeConfiguration.getType());
            edge.setRoutingKey(edgeConfiguration.getRoutingKey());
            edge.setSecret(edgeConfiguration.getSecret());
            edge.setAdditionalInfo(JacksonUtil.toJsonNode(edgeConfiguration.getAdditionalInfo()));
            edgeCtx.getEdgeService().saveEdge(edge, false);
        } finally {
            cloudSynchronizationManager.getSync().remove();
            edgeCreationLock.unlock();
        }
        return Futures.immediateFuture(null);
    }

}
