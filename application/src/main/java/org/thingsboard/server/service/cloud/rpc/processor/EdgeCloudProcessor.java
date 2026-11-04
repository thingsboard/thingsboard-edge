// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.cloud.rpc.processor;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
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

    public ListenableFuture<Void> processEdgeConfigurationMsgFromCloud(TenantId tenantId,
                                                                       EdgeConfiguration edgeConfiguration) throws ThingsboardException {
        EdgeId edgeId = new EdgeId(new UUID(edgeConfiguration.getEdgeIdMSB(), edgeConfiguration.getEdgeIdLSB()));
        edgeCreationLock.lock();
        try {
            cloudSynchronizationManager.getSync().set(true);
            Edge edge = edgeCtx.getEdgeService().findEdgeById(tenantId, edgeId);
            CustomerId customerId = safeGetCustomerId(edgeConfiguration.getCustomerIdMSB(), edgeConfiguration.getCustomerIdLSB());
            if (edge == null) {
                edge = new Edge();
                edge.setId(edgeId);
                edge.setTenantId(tenantId);
                edge.setCreatedTime(Uuids.unixTimestamp(edgeId.getId()));
            } else {
                changeOwnerIfRequired(tenantId, customerId, edgeId);
            }
            edge.setCustomerId(customerId);
            edge.setName(edgeConfiguration.getName());
            edge.setType(edgeConfiguration.getType());
            edge.setRoutingKey(edgeConfiguration.getRoutingKey());
            edge.setSecret(edgeConfiguration.getSecret());
            edge.setEdgeLicenseKey(edgeConfiguration.getEdgeLicenseKey());
            edge.setCloudEndpoint(edgeConfiguration.getCloudEndpoint());
            edge.setAdditionalInfo(JacksonUtil.toJsonNode(edgeConfiguration.getAdditionalInfo()));
            edgeCtx.getEdgeService().saveEdge(edge, false);
        } finally {
            cloudSynchronizationManager.getSync().remove();
            edgeCreationLock.unlock();
        }
        return Futures.immediateFuture(null);
    }

}
