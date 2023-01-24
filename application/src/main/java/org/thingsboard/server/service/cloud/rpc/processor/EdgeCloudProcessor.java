/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.cloud.rpc.processor;

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
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class EdgeCloudProcessor extends BaseEdgeProcessor {

    private final Lock edgeCreationLock = new ReentrantLock();

    public ListenableFuture<Void> processEdgeConfigurationMsgFromCloud(TenantId tenantId,
                                                                       EdgeConfiguration edgeConfiguration) throws ThingsboardException {
        EdgeId edgeId = new EdgeId(new UUID(edgeConfiguration.getEdgeIdMSB(), edgeConfiguration.getEdgeIdLSB()));
        edgeCreationLock.lock();
        try {
            Edge edge = edgeService.findEdgeById(tenantId, edgeId);
            CustomerId customerId = safeGetCustomerId(edgeConfiguration.getCustomerIdMSB(), edgeConfiguration.getCustomerIdLSB());
            if (edge == null) {
                edge = new Edge();
                edge.setId(edgeId);
                edge.setTenantId(tenantId);
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
            edgeService.saveEdge(edge, false);
        } finally {
            edgeCreationLock.unlock();
        }
        return Futures.immediateFuture(null);
    }
}
