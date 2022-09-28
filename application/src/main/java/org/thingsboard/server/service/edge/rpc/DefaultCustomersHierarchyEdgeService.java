package org.thingsboard.server.service.edge.rpc;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@TbCoreComponent
public class DefaultCustomersHierarchyEdgeService implements CustomersHierarchyEdgeService {

    private final CustomerHierarchyNode root =
            new CustomerHierarchyNode(new CustomerId(CustomerId.NULL_UUID), new ArrayList<>(), new ArrayList<>());

    @Autowired
    private CustomerService customerService;

    @Autowired
    private EdgeService edgeService;

    @Autowired
    private TenantService tenantService;

    @PostConstruct
    private void init() {
        PageLink pageLink = new PageLink(100, 0);
        PageData<TenantId> tenantsIds;
        do {
            tenantsIds = tenantService.findTenantsIds(pageLink);
            for (TenantId tenantId : tenantsIds.getData()) {
                PageLink pageLink1 = new PageLink(100, 0);
                PageData<Edge> edgesByTenantId;
                do {
                    edgesByTenantId = edgeService.findEdgesByTenantId(tenantId, pageLink1);
                    for (Edge edge : edgesByTenantId.getData()) {
                        List<CustomerId> customersHierarchy = getCustomersHierarchy(edge.getTenantId(), edge.getCustomerId());
                        addEdgeToCustomersHierarchy(customersHierarchy, edge.getId(), root);
                    }
                    pageLink1 = pageLink1.nextPageLink();
                } while (edgesByTenantId.hasNext());
            }
            pageLink = pageLink.nextPageLink();
        } while (tenantsIds.hasNext());

    }

    private List<CustomerId> getCustomersHierarchy(TenantId tenantId, CustomerId customerId) {
        List<CustomerId> result = new ArrayList<>();
        result.add(customerId);
        if (!customerId.isNullUid()) {
            Customer customerById = customerService.findCustomerById(tenantId, customerId);
            if (customerById != null) {
                CustomerId parentId = customerById.getParentCustomerId();
                if (parentId != null) {
                    result.addAll(getCustomersHierarchy(tenantId, parentId));
                }
            }
        }
        return result;
    }

    private void addEdgeToCustomersHierarchy(List<CustomerId> customerIds, EdgeId edgeId, CustomerHierarchyNode node) {
        if (customerIds.size() == 1) {
            node.getEdgeIds().add(edgeId);
            return;
        }
        customerIds.remove(0);
        CustomerId customerId = customerIds.get(0);
        CustomerHierarchyNode childNode = null;
        for (CustomerHierarchyNode tmpNode : node.getChildren()) {
            if (tmpNode.getCustomerId().equals(customerId)) {
                childNode = tmpNode;
                break;
            }
        }
        if (childNode == null) {
            childNode = new CustomerHierarchyNode(customerId, new ArrayList<>(), new ArrayList<>());
            node.getChildren().add(childNode);
        }
        addEdgeToCustomersHierarchy(customerIds, edgeId, childNode);
    }

    @Override
    public List<EdgeId> findEdgesByCustomerId(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing findEdgesByCustomerId [{}]", customerId);
        return findEdgesByCustomerIdInt(customerId, root);
    }

    private List<EdgeId> findEdgesByCustomerIdInt(CustomerId customerId, CustomerHierarchyNode node) {
        if (node.getCustomerId().equals(customerId)) {
            log.trace("Found customer id in hierarchy [{}]", node);
            return node.getEdgeIds();
        } else {
            for (CustomerHierarchyNode child : node.getChildren()) {
                log.trace("Processing hierarchy node [{}]", node);
                List<EdgeId> result = findEdgesByCustomerIdInt(customerId, child);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }
    }

    @Override
    public void processEdgeChangeOwner(TenantId tenantId, EdgeId edgeId, EntityId previousOwnerId) {
        log.trace("Executing processEdgeChangeOwner [{}]", edgeId);
        if (previousOwnerId == null || EntityType.TENANT.equals(previousOwnerId.getEntityType())) {
            previousOwnerId = new CustomerId(EntityId.NULL_UUID);
        }
        CustomerId previousCustomerId = new CustomerId(previousOwnerId.getId());
        List<EdgeId> edgeIds = findEdgesByCustomerIdInt(previousCustomerId, root);
        if (edgeIds != null) {
            edgeIds.remove(edgeId);
        }
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge != null) {
            List<CustomerId> customersHierarchy = getCustomersHierarchy(edge.getTenantId(), edge.getCustomerId());
            addEdgeToCustomersHierarchy(customersHierarchy, edge.getId(), root);
        }
    }

    @Data
    private static class CustomerHierarchyNode {
        private final CustomerId customerId;
        private final List<CustomerHierarchyNode> children;
        private final List<EdgeId> edgeIds;
    }
}
