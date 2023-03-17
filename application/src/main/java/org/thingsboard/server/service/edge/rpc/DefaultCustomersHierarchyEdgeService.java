/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.service.edge.rpc;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
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
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@TbCoreComponent
public class DefaultCustomersHierarchyEdgeService implements CustomersHierarchyEdgeService {

    private static final CustomerId NULL_CUSTOMER_ID = new CustomerId(CustomerId.NULL_UUID);

    private final CustomerHierarchyNode root =
            new CustomerHierarchyNode(NULL_CUSTOMER_ID, new ArrayList<>(), new ArrayList<>());

    @Autowired
    private CustomerService customerService;

    @Autowired
    private EdgeService edgeService;

    @Autowired
    private TenantService tenantService;

    private ScheduledExecutorService scheduledExecutor;

    @PostConstruct
    private void init() {
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("edge-customers-hierarchy"));
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void onApplicationEvent(ApplicationReadyEvent ignored) {
        scheduledExecutor.schedule(this::fetchCustomersHierarchy, 1, TimeUnit.SECONDS);
    }

    private void fetchCustomersHierarchy() {
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
                        List<CustomerId> customersHierarchyIds = getCustomersHierarchyIds(edge.getTenantId(), edge.getCustomerId());
                        addEdgeToCustomersHierarchy(customersHierarchyIds, edge.getId(), root);
                    }
                    pageLink1 = pageLink1.nextPageLink();
                } while (edgesByTenantId.hasNext());
            }
            pageLink = pageLink.nextPageLink();
        } while (tenantsIds.hasNext());
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdownNow();
        }
    }

    private List<CustomerId> getCustomersHierarchyIds(TenantId tenantId, CustomerId customerId) {
        List<Customer> customersHierarchy = getCustomersHierarchy(tenantId, customerId);
        return customersHierarchy.stream().map(Customer::getId).collect(Collectors.toList());
    }

    @Override
    public List<Customer> getCustomersHierarchy(TenantId tenantId, CustomerId customerId) {
        List<Customer> result = new ArrayList<>();
        if (customerId != null && !customerId.isNullUid()) {
            Customer customerById = customerService.findCustomerById(tenantId, customerId);
            result.add(customerById);
            if (customerById != null) {
                CustomerId parentId = customerById.getParentCustomerId();
                if (parentId != null) {
                    result.addAll(getCustomersHierarchy(tenantId, parentId));
                }
            }
        }
        Collections.reverse(result);
        return result;
    }

    private void addEdgeToCustomersHierarchy(List<CustomerId> customerIds, EdgeId edgeId, CustomerHierarchyNode node) {
        if (customerIds.isEmpty()) {
            node.getEdgeIds().add(edgeId);
            return;
        }
        CustomerId customerId = customerIds.remove(0);
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
    public List<EdgeId> findAllEdgesInHierarchyByCustomerId(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing findEdgesByCustomerId [{}]", customerId);
        return findAllEdgesInHierarchyByCustomerId(customerId, root, false);
    }

    private List<EdgeId> findAllEdgesInHierarchyByCustomerId(CustomerId customerId, CustomerHierarchyNode node, boolean include) {
        List<EdgeId> result = new ArrayList<>();
        if (node.getCustomerId().equals(customerId)) {
            log.trace("Found customer id in hierarchy [{}]", node);
            include = true;
        }
        if (include) {
            result.addAll(node.getEdgeIds());
        }
        for (CustomerHierarchyNode child : node.getChildren()) {
            log.trace("Processing hierarchy node [{}]", node);
            result.addAll(findAllEdgesInHierarchyByCustomerId(customerId, child, include));
        }
        return result;
    }

    @Override
    public void processEdgeChangeOwner(TenantId tenantId, EdgeId edgeId, EntityId previousOwnerId) {
        log.trace("Executing processEdgeChangeOwner [{}]", edgeId);
        if (previousOwnerId == null || EntityType.TENANT.equals(previousOwnerId.getEntityType())) {
            previousOwnerId = NULL_CUSTOMER_ID;
        }
        CustomerId previousCustomerId = new CustomerId(previousOwnerId.getId());
        CustomerHierarchyNode node = findNodeByCustomerId(previousCustomerId, root);
        if (node != null) {
            node.getEdgeIds().remove(edgeId);
        }
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge != null) {
            List<CustomerId> customersHierarchyIds = getCustomersHierarchyIds(edge.getTenantId(), edge.getCustomerId());
            addEdgeToCustomersHierarchy(customersHierarchyIds, edge.getId(), root);
        }
    }

    private CustomerHierarchyNode findNodeByCustomerId(CustomerId customerId, CustomerHierarchyNode node) {
        if (node.getCustomerId().equals(customerId)) {
            log.trace("Found customer id in hierarchy [{}]", node);
            return node;
        } else {
            for (CustomerHierarchyNode child : node.getChildren()) {
                log.trace("Processing hierarchy node [{}]", node);
                CustomerHierarchyNode result = findNodeByCustomerId(customerId, child);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }
    }

    @Data
    private static class CustomerHierarchyNode {
        private final CustomerId customerId;
        private final List<CustomerHierarchyNode> children;
        private final List<EdgeId> edgeIds;
    }
}
