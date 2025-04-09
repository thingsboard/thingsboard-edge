/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.edge.rpc.processor.customer;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class CustomerEdgeProcessor extends BaseEdgeProcessor {

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        CustomerId customerId = new CustomerId(edgeEvent.getEntityId());
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                Customer customer = edgeCtx.getCustomerService().findCustomerById(edgeEvent.getTenantId(), customerId);
                if (customer != null) {
                    EntityGroupId entityGroupId = edgeEvent.getEntityGroupId() != null ? new EntityGroupId(edgeEvent.getEntityGroupId()) : null;
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    CustomerUpdateMsg customerUpdateMsg = EdgeMsgConstructorUtils.constructCustomerUpdatedMsg(msgType, customer, entityGroupId);
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addCustomerUpdateMsg(customerUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                // case CHANGE_OWNER: TODO: @voba implement
                CustomerUpdateMsg customerUpdateMsg = EdgeMsgConstructorUtils.constructCustomerDeleteMsg(customerId);
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addCustomerUpdateMsg(customerUpdateMsg)
                        .build();
            }
        }
        return null;
    }

    @Override
    public ListenableFuture<Void> processEntityNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        UUID uuid = new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB());
        CustomerId customerId = new CustomerId(EntityIdFactory.getByEdgeEventTypeAndUuid(type, uuid).getId());
        switch (actionType) {
            case ADDED:
                Customer customerById = edgeCtx.getCustomerService().findCustomerById(tenantId, customerId);
                if (customerById != null && customerById.isPublic()) {
                    EntityId ownerId = customerById.getOwnerId();
                    if (EntityType.TENANT.equals(ownerId.getEntityType())) {
                        List<ListenableFuture<Void>> futures = new ArrayList<>();
                        PageDataIterable<Edge> edges = new PageDataIterable<>(link -> edgeCtx.getEdgeService().findEdgesByTenantId(tenantId, link), 1024);
                        for (Edge edge : edges) {
                            futures.add(saveEdgeEvent(tenantId, edge.getId(), type, actionType, customerId, null));
                        }
                        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
                    } else {
                        List<EdgeId> edgesByCustomerId = edgeCtx.getCustomersHierarchyEdgeService().findAllEdgesInHierarchyByCustomerId(tenantId, new CustomerId(ownerId.getId()));
                        List<ListenableFuture<Void>> futures = new ArrayList<>();
                        if (edgesByCustomerId != null) {
                            for (EdgeId edgeId : edgesByCustomerId) {
                                futures.add(saveEdgeEvent(tenantId, edgeId, type, actionType, customerId, null));
                            }
                        }
                        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
                    }
                }
                return Futures.immediateFuture(null);
            case UPDATED:
                List<EdgeId> edgesByCustomerId = edgeCtx.getCustomersHierarchyEdgeService().findAllEdgesInHierarchyByCustomerId(tenantId, customerId);
                List<ListenableFuture<Void>> futures = new ArrayList<>();
                if (edgesByCustomerId != null) {
                    for (EdgeId edgeId : edgesByCustomerId) {
                        futures.add(saveEdgeEvent(tenantId, edgeId, type, actionType, customerId, null));
                    }
                }
                return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
            case DELETED:
                return processActionForAllEdges(tenantId, type, actionType, customerId, null, null);
            // case CHANGE_OWNER:
            default:
                return Futures.immediateFuture(null);
        }
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.CUSTOMER;
    }

}
