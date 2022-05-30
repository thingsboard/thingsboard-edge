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
package org.thingsboard.server.service.entitiy.ruleChain;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rule.DefaultRuleChainCreateRequest;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleChainUpdateResult;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultTbRuleChainNotifyService extends AbstractTbEntityService implements TbRuleChainNotifyService {

    @Override
    public RuleChain save(RuleChain ruleChain, EntityGroup entityGroup, SecurityUser user) throws ThingsboardException {
        TenantId tenantId = ruleChain.getTenantId();
        ActionType actionType = ruleChain.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        try {
            RuleChain savedRuleChain = checkNotNull(ruleChainService.saveRuleChain(ruleChain));

            if (RuleChainType.CORE.equals(savedRuleChain.getType())) {
                tbClusterService.broadcastEntityStateChangeEvent(tenantId, savedRuleChain.getId(),
                        actionType.equals(ActionType.ADDED) ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
            }
            boolean isSendMsg = RuleChainType.EDGE.equals(savedRuleChain.getType()) && actionType.equals(ActionType.UPDATED);
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, savedRuleChain.getId(),
                    savedRuleChain, user, actionType, isSendMsg, null);
            return savedRuleChain;
        } catch (Exception e) {
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, emptyId(EntityType.RULE_CHAIN),
                    ruleChain, user, actionType, false, e);
            throw handleException(e);
        }
    }

    @Override
    public void delete(RuleChain ruleChain, SecurityUser user) throws ThingsboardException {
        TenantId tenantId = ruleChain.getTenantId();
        RuleChainId ruleChainId = ruleChain.getId();
        try {
            List<RuleNode> referencingRuleNodes = ruleChainService.getReferencingRuleChainNodes(tenantId, ruleChainId);

            Set<RuleChainId> referencingRuleChainIds = referencingRuleNodes.stream().map(RuleNode::getRuleChainId).collect(Collectors.toSet());

            List<EdgeId> relatedEdgeIds = null;
            if (RuleChainType.EDGE.equals(ruleChain.getType())) {
                relatedEdgeIds = findRelatedEdgeIds(tenantId, ruleChainId);
            }

            ruleChainService.deleteRuleChainById(tenantId, ruleChainId);

            referencingRuleChainIds.remove(ruleChain.getId());

            if (RuleChainType.CORE.equals(ruleChain.getType())) {
                referencingRuleChainIds.forEach(referencingRuleChainId ->
                        tbClusterService.broadcastEntityStateChangeEvent(tenantId, referencingRuleChainId, ComponentLifecycleEvent.UPDATED));

                tbClusterService.broadcastEntityStateChangeEvent(tenantId, ruleChain.getId(), ComponentLifecycleEvent.DELETED);
            }

            notificationEntityService.notifyDeleteRuleChain(tenantId, ruleChain, relatedEdgeIds, user);
        } catch (Exception e) {
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, emptyId(EntityType.RULE_CHAIN),
                    null, user, ActionType.DELETED, false, e, ruleChainId.toString());
            throw handleException(e);
        }
    }

    @Override
    public RuleChain saveDefaultByName(TenantId tenantId, DefaultRuleChainCreateRequest request, SecurityUser user) throws ThingsboardException {
        try {
            RuleChain savedRuleChain = installScripts.createDefaultRuleChain(tenantId, request.getName());
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, savedRuleChain.getId(), ComponentLifecycleEvent.CREATED);
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, savedRuleChain.getId(),
                    savedRuleChain, user, ActionType.ADDED, false, null);
            return savedRuleChain;
        } catch (Exception e) {
            RuleChain ruleChain = new RuleChain();
            ruleChain.setName(request.getName());
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, emptyId(EntityType.RULE_CHAIN),
                    ruleChain, user, ActionType.ADDED, false, e);
            throw handleException(e);
        }
    }

    @Override
    public RuleChain setRootRuleChain(TenantId tenantId, RuleChain ruleChain, SecurityUser user) throws ThingsboardException {
        RuleChain previousRootRuleChain = ruleChainService.getRootTenantRuleChain(tenantId);
        RuleChainId previousRootRuleChainId = previousRootRuleChain.getId();
        RuleChainId ruleChainId = ruleChain.getId();
        try {
            if (ruleChainService.setRootRuleChain(tenantId, ruleChainId)) {
                if (previousRootRuleChain != null) {
                    previousRootRuleChain = ruleChainService.findRuleChainById(tenantId, previousRootRuleChainId);

                    tbClusterService.broadcastEntityStateChangeEvent(tenantId, previousRootRuleChainId,
                            ComponentLifecycleEvent.UPDATED);
                    notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, previousRootRuleChainId,
                            previousRootRuleChain, user, ActionType.UPDATED, false, null);
                }
                ruleChain = ruleChainService.findRuleChainById(tenantId, ruleChainId);

                tbClusterService.broadcastEntityStateChangeEvent(tenantId, ruleChainId,
                        ComponentLifecycleEvent.UPDATED);
                notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, ruleChainId,
                        ruleChain, user, ActionType.UPDATED, false, null);
            }
            return ruleChain;
        } catch (Exception e) {
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, emptyId(EntityType.RULE_CHAIN),
                    ruleChain, user, ActionType.UPDATED, false, e, ruleChainId.toString());
            throw handleException(e);
        }
    }

    @Override
    public RuleChainMetaData saveRuleChainMetaData(TenantId tenantId, RuleChain ruleChain, RuleChainMetaData ruleChainMetaData,
                                                   boolean updateRelated, SecurityUser user) throws ThingsboardException {
        RuleChainId ruleChainId = ruleChain.getId();
        RuleChainId ruleChainMetaDataId = ruleChainMetaData.getRuleChainId();
        try {
            RuleChainUpdateResult result = ruleChainService.saveRuleChainMetaData(tenantId, ruleChainMetaData);
            checkNotNull(result.isSuccess() ? true : null);

            List<RuleChain> updatedRuleChains;
            if (updateRelated && result.isSuccess()) {
                updatedRuleChains = tbRuleChainService.updateRelatedRuleChains(tenantId, ruleChainMetaDataId, result);
            } else {
                updatedRuleChains = Collections.emptyList();
            }

            RuleChainMetaData savedRuleChainMetaData = checkNotNull(ruleChainService.loadRuleChainMetaData(tenantId, ruleChainMetaDataId));

            if (RuleChainType.CORE.equals(ruleChain.getType())) {
                tbClusterService.broadcastEntityStateChangeEvent(tenantId, ruleChainId, ComponentLifecycleEvent.UPDATED);
                updatedRuleChains.forEach(updatedRuleChain -> {
                    tbClusterService.broadcastEntityStateChangeEvent(tenantId, updatedRuleChain.getId(), ComponentLifecycleEvent.UPDATED);
                });
            }

            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, ruleChainId,
                    ruleChain, user, ActionType.UPDATED, false, null, ruleChainMetaData);

            if (RuleChainType.EDGE.equals(ruleChain.getType())) {
                notificationEntityService.notifySendMsgToEdgeService(tenantId, ruleChain, EdgeEventActionType.UPDATED);
            }

            updatedRuleChains.forEach(updatedRuleChain -> {
                if (RuleChainType.EDGE.equals(ruleChain.getType())) {
                    notificationEntityService.notifySendMsgToEdgeService(tenantId, updatedRuleChain, EdgeEventActionType.UPDATED);
                } else {
                    try {
                        RuleChainMetaData updatedRuleChainMetaData = checkNotNull(ruleChainService.loadRuleChainMetaData(tenantId, updatedRuleChain.getId()));
                        notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, updatedRuleChain.getId(),
                                updatedRuleChain, user, ActionType.UPDATED, false, null, updatedRuleChainMetaData);
                    } catch (ThingsboardException e) {
                        e.printStackTrace();
                    }
                }
            });
            return savedRuleChainMetaData;
        } catch (Exception e) {
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, emptyId(EntityType.RULE_CHAIN),
                    null, user, ActionType.ADDED, false, e, ruleChainMetaData);
            throw handleException(e);
        }
    }

    @Override
    public RuleChain assignUnassignRuleChainToEdge(TenantId tenantId, RuleChain ruleChain, Edge edge, ActionType actionType, SecurityUser user) throws ThingsboardException {
        RuleChainId ruleChainId = ruleChain.getId();
        try {
            RuleChain savedRuleChain = checkNotNull(ruleChainService.assignRuleChainToEdge(tenantId, ruleChainId, edge.getId()));
            notificationEntityService.notifyAssignOrUnassignEntityToEdge(tenantId, ruleChainId,
                    null, edge.getId(),
                    savedRuleChain, actionType,
                    user, ruleChainId.toString(), edge.getId().toString(), edge.getName());
            return savedRuleChain;
        } catch (Exception e) {
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, emptyId(EntityType.RULE_CHAIN),
                    null, user, actionType, false, e, ruleChainId.toString(), edge.getId().toString());
            throw handleException(e);
        }
    }

    @Override
    public RuleChain setEdgeTemplateRootRuleChain(TenantId tenantId, RuleChain ruleChain, SecurityUser user) throws ThingsboardException {
        RuleChainId ruleChainId = ruleChain.getId();
        try {
            ruleChainService.setEdgeTemplateRootRuleChain(tenantId, ruleChainId);

            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, ruleChainId,
                    ruleChain, user, ActionType.UPDATED, false, null);
            return ruleChain;
        } catch (Exception e) {
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, emptyId(EntityType.RULE_CHAIN),
                    null, user, ActionType.UPDATED, false, e, ruleChainId.toString());
            throw handleException(e);
        }
    }

    @Override
    public RuleChain setAutoAssignToEdgeRuleChain(TenantId tenantId, RuleChain ruleChain, SecurityUser user) throws ThingsboardException {
        RuleChainId ruleChainId = ruleChain.getId();
        try {
            ruleChainService.setAutoAssignToEdgeRuleChain(tenantId, ruleChainId);

            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, ruleChainId,
                    ruleChain, user, ActionType.UPDATED, false, null);
            return ruleChain;
        } catch (Exception e) {
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, emptyId(EntityType.RULE_CHAIN),
                    null, user, ActionType.UPDATED, false, e, ruleChainId.toString());
            throw handleException(e);
        }
    }

    @Override
    public RuleChain unsetAutoAssignToEdgeRuleChain(TenantId tenantId, RuleChain ruleChain, SecurityUser user) throws ThingsboardException {
        RuleChainId ruleChainId = ruleChain.getId();
        try {
            ruleChainService.unsetAutoAssignToEdgeRuleChain(tenantId, ruleChainId);
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, ruleChainId,
                    ruleChain, user, ActionType.UPDATED, false, null);
            return ruleChain;
        } catch (Exception e) {
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, emptyId(EntityType.RULE_CHAIN),
                    null, user, ActionType.UPDATED, false, e, ruleChainId.toString());
            throw handleException(e);
        }
    }
}
