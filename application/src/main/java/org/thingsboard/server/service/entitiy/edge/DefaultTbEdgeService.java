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
package org.thingsboard.server.service.entitiy.edge;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeNotificationService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@AllArgsConstructor
@TbCoreComponent
@Service
@Slf4j
public class DefaultTbEdgeService extends AbstractTbEntityService implements TbEdgeService {

    private final EdgeNotificationService edgeNotificationService;
    private final RuleChainService ruleChainService;

    @Override
    public Edge save(Edge edge, RuleChain edgeTemplateRootRuleChain, EntityGroup entityGroup, User user) throws Exception {
        ActionType actionType = edge.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = edge.getTenantId();
        try {
            String oldEdgeName = null;
            if (actionType == ActionType.UPDATED) {
                Edge edgeById = edgeService.findEdgeById(tenantId, edge.getId());
                if (edgeById != null) {
                    oldEdgeName = edgeById.getName();
                }
            }
            if (actionType == ActionType.ADDED && edge.getRootRuleChainId() == null) {
                edge.setRootRuleChainId(edgeTemplateRootRuleChain.getId());
            }
            Edge savedEdge = checkNotNull(edgeService.saveEdge(edge));
            EdgeId edgeId = savedEdge.getId();

            if (entityGroup != null && actionType == ActionType.ADDED) {
                entityGroupService.addEntityToEntityGroup(tenantId, entityGroup.getId(), edgeId);
            }

            if (actionType == ActionType.ADDED) {
                ruleChainService.assignRuleChainToEdge(tenantId, edgeTemplateRootRuleChain.getId(), savedEdge.getId());
                edgeNotificationService.setEdgeRootRuleChain(tenantId, savedEdge, edgeTemplateRootRuleChain.getId());
                edgeService.assignDefaultRuleChainsToEdge(tenantId, savedEdge.getId());
                edgeService.assignTenantAdministratorsAndUsersGroupToEdge(tenantId, savedEdge.getId());
            }

            if (oldEdgeName != null && !oldEdgeName.equals(savedEdge.getName())) {
                edgeService.renameDeviceEdgeAllGroup(tenantId, savedEdge, oldEdgeName);
            }

            notificationEntityService.notifyCreateOrUpdateOrDeleteEdge(tenantId, edgeId, savedEdge.getCustomerId(), savedEdge, actionType, user);

            return savedEdge;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.EDGE), edge, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(Edge edge, User user) {
        EdgeId edgeId = edge.getId();
        TenantId tenantId = edge.getTenantId();
        try {
            edgeService.deleteEdge(tenantId, edgeId);
            notificationEntityService.notifyCreateOrUpdateOrDeleteEdge(tenantId, edgeId, edge.getCustomerId(), edge, ActionType.DELETED, user, edgeId.toString());
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.EDGE), ActionType.DELETED,
                    user, e, edgeId.toString());
            throw e;
        }
    }

    @Override
    public Edge setEdgeRootRuleChain(Edge edge, RuleChainId ruleChainId, User user) throws Exception {
        TenantId tenantId = edge.getTenantId();
        EdgeId edgeId = edge.getId();
        try {
            Edge updatedEdge = edgeNotificationService.setEdgeRootRuleChain(tenantId, edge, ruleChainId);
            notificationEntityService.logEntityAction(tenantId, edgeId, edge, null, ActionType.UPDATED, user);
            return updatedEdge;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.EDGE),
                    ActionType.UPDATED, user, e, edgeId.toString());
            throw e;
        }
    }
}
