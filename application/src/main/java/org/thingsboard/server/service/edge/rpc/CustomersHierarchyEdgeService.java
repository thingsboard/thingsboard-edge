package org.thingsboard.server.service.edge.rpc;

import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;

public interface CustomersHierarchyEdgeService {

    List<EdgeId> findEdgesByCustomerId(TenantId tenantId, CustomerId customerId);

    void processEdgeChangeOwner(TenantId tenantId, EdgeId edgeId, EntityId previousOwnerId);

}
