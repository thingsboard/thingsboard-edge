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
package org.thingsboard.server.dao.edge;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeInfo;
import org.thingsboard.server.common.data.edge.EdgeSearchQuery;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.Optional;

public interface EdgeService extends EntityDaoService {

    Edge findEdgeById(TenantId tenantId, EdgeId edgeId);

    EdgeInfo findEdgeInfoById(TenantId tenantId, EdgeId edgeId);

    ListenableFuture<Edge> findEdgeByIdAsync(TenantId tenantId, EdgeId edgeId);

    Edge findEdgeByTenantIdAndName(TenantId tenantId, String name);

    Optional<Edge> findEdgeByRoutingKey(TenantId tenantId, String routingKey);

    Edge saveEdge(Edge edge);

    void deleteEdge(TenantId tenantId, EdgeId edgeId);

    PageData<Edge> findEdgesByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<Edge> findEdgesByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink);

    ListenableFuture<List<Edge>> findEdgesByTenantIdAndIdsAsync(TenantId tenantId, List<EdgeId> edgeIds);

    void deleteEdgesByTenantId(TenantId tenantId);

    PageData<Edge> findEdgesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<Edge> findEdgesByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink);

    ListenableFuture<List<Edge>> findEdgesByTenantIdCustomerIdAndIdsAsync(TenantId tenantId, CustomerId customerId, List<EdgeId> edgeIds);

    void deleteEdgesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId);

    ListenableFuture<List<Edge>> findEdgesByQuery(TenantId tenantId, EdgeSearchQuery query);

    ListenableFuture<List<EntitySubtype>> findEdgeTypesByTenantId(TenantId tenantId);

    void assignDefaultRuleChainsToEdge(TenantId tenantId, EdgeId edgeId);

    void assignTenantAdministratorsAndUsersGroupToEdge(TenantId tenantId, EdgeId edgeId);

    void assignCustomerAdministratorsAndUsersGroupToEdge(TenantId tenantId, EdgeId edgeId, CustomerId customerId, CustomerId parentCustomerId);

    PageData<Edge> findEdgesByTenantIdAndEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink);

    PageData<EdgeId> findEdgeIdsByTenantIdAndEntityIds(TenantId tenantId, List<EntityId> entityIds, EntityType entityType, PageLink pageLink);

    PageData<EdgeId> findEdgeIdsByTenantIdAndEntityGroupIds(TenantId tenantId, List<EntityGroupId> entityGroupId, EntityType groupType, PageLink pageLink);

    List<EdgeId> findAllRelatedEdgeIds(TenantId tenantId, EntityId entityId);

    PageData<EdgeId> findRelatedEdgeIdsByEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink);

    PageData<EdgeId> findRelatedEdgeIdsByEntityId(TenantId tenantId, EntityId entityId, EntityType groupType, PageLink pageLink);

    PageData<Edge> findEdgesByEntityGroupId(EntityGroupId groupId, PageLink pageLink);

    PageData<Edge> findEdgesByEntityGroupIds(List<EntityGroupId> groupIds, PageLink pageLink);

    PageData<Edge> findEdgesByEntityGroupIdsAndType(List<EntityGroupId> groupIds, String type, PageLink pageLink);

    void renameDeviceEdgeAllGroup(TenantId tenantId, Edge edge, String oldEdgeName);

    String findMissingToRelatedRuleChains(TenantId tenantId, EdgeId edgeId, String tbRuleChainInputNodeClassName);

    String findEdgeMissingAttributes(TenantId tenantId, EdgeId edgeId, List<IntegrationId> integrationIds) throws Exception;

    String findAllRelatedEdgesMissingAttributes(TenantId tenantId, IntegrationId integrationId) throws Exception;

    PageData<EdgeInfo> findEdgeInfosByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<EdgeInfo> findEdgeInfosByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink);

    PageData<EdgeInfo> findTenantEdgeInfosByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<EdgeInfo> findTenantEdgeInfosByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink);

    PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink);

    PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerIdIncludingSubCustomers(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerIdAndTypeIncludingSubCustomers(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink);
}
