/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.rule;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ShortEntityGroupInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.edge.EdgeDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.TimePaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TenantDao;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by igor on 3/12/18.
 */
@Service
@Slf4j
public class BaseRuleChainService extends AbstractEntityService implements RuleChainService {

    @Autowired
    private RuleChainDao ruleChainDao;

    @Autowired
    private RuleNodeDao ruleNodeDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private EdgeDao edgeDao;

    @Override
    public RuleChain saveRuleChain(RuleChain ruleChain) {
        ruleChainValidator.validate(ruleChain, RuleChain::getTenantId);
        RuleChain savedRuleChain = ruleChainDao.save(ruleChain.getTenantId(), ruleChain);
        if (ruleChain.isRoot() && ruleChain.getId() == null) {
            try {
                createRelation(ruleChain.getTenantId(), new EntityRelation(savedRuleChain.getTenantId(), savedRuleChain.getId(),
                        EntityRelation.CONTAINS_TYPE, RelationTypeGroup.RULE_CHAIN));
            } catch (ExecutionException | InterruptedException e) {
                log.warn("[{}] Failed to create tenant to root rule chain relation. from: [{}], to: [{}]",
                        savedRuleChain.getTenantId(), savedRuleChain.getId());
                throw new RuntimeException(e);
            }
        }
        return savedRuleChain;
    }

    @Override
    public boolean setRootRuleChain(TenantId tenantId, RuleChainId ruleChainId) {
        RuleChain ruleChain = ruleChainDao.findById(tenantId, ruleChainId.getId());
        if (!ruleChain.isRoot()) {
            RuleChain previousRootRuleChain = getRootTenantRuleChain(ruleChain.getTenantId());
            try {
                if (previousRootRuleChain == null) {
                    setRootAndSave(tenantId, ruleChain);
                    return true;
                } else if (!previousRootRuleChain.getId().equals(ruleChain.getId())) {
                    deleteRelation(tenantId, new EntityRelation(previousRootRuleChain.getTenantId(), previousRootRuleChain.getId(),
                            EntityRelation.CONTAINS_TYPE, RelationTypeGroup.RULE_CHAIN));
                    previousRootRuleChain.setRoot(false);
                    ruleChainDao.save(tenantId, previousRootRuleChain);
                    setRootAndSave(tenantId, ruleChain);
                    return true;
                }
            } catch (ExecutionException | InterruptedException e) {
                log.warn("[{}] Failed to set root rule chain, ruleChainId: [{}]", ruleChainId);
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    private void setRootAndSave(TenantId tenantId, RuleChain ruleChain) throws ExecutionException, InterruptedException {
        createRelation(tenantId, new EntityRelation(ruleChain.getTenantId(), ruleChain.getId(),
                EntityRelation.CONTAINS_TYPE, RelationTypeGroup.RULE_CHAIN));
        ruleChain.setRoot(true);
        ruleChainDao.save(tenantId, ruleChain);
    }

    @Override
    public RuleChainMetaData saveRuleChainMetaData(TenantId tenantId, RuleChainMetaData ruleChainMetaData) {
        Validator.validateId(ruleChainMetaData.getRuleChainId(), "Incorrect rule chain id.");
        RuleChain ruleChain = findRuleChainById(tenantId, ruleChainMetaData.getRuleChainId());
        if (ruleChain == null) {
            return null;
        }

        List<RuleNode> nodes = ruleChainMetaData.getNodes();
        List<RuleNode> toAddOrUpdate = new ArrayList<>();
        List<RuleNode> toDelete = new ArrayList<>();

        Map<RuleNodeId, Integer> ruleNodeIndexMap = new HashMap<>();
        if (nodes != null) {
            for (RuleNode node : nodes) {
                if (node.getId() != null) {
                    ruleNodeIndexMap.put(node.getId(), nodes.indexOf(node));
                } else {
                    toAddOrUpdate.add(node);
                }
            }
        }

        List<RuleNode> existingRuleNodes = getRuleChainNodes(tenantId, ruleChainMetaData.getRuleChainId());
        for (RuleNode existingNode : existingRuleNodes) {
            deleteEntityRelations(tenantId, existingNode.getId());
            Integer index = ruleNodeIndexMap.get(existingNode.getId());
            if (index != null) {
                toAddOrUpdate.add(ruleChainMetaData.getNodes().get(index));
            } else {
                toDelete.add(existingNode);
            }
        }
        for (RuleNode node : toAddOrUpdate) {
            node.setRuleChainId(ruleChain.getId());
            RuleNode savedNode = ruleNodeDao.save(tenantId, node);
            try {
                createRelation(tenantId, new EntityRelation(ruleChainMetaData.getRuleChainId(), savedNode.getId(),
                        EntityRelation.CONTAINS_TYPE, RelationTypeGroup.RULE_CHAIN));
            } catch (ExecutionException | InterruptedException e) {
                log.warn("[{}] Failed to create rule chain to rule node relation. from: [{}], to: [{}]",
                        ruleChainMetaData.getRuleChainId(), savedNode.getId());
                throw new RuntimeException(e);
            }
            int index = nodes.indexOf(node);
            nodes.set(index, savedNode);
            ruleNodeIndexMap.put(savedNode.getId(), index);
        }
        for (RuleNode node : toDelete) {
            deleteRuleNode(tenantId, node.getId());
        }
        RuleNodeId firstRuleNodeId = null;
        if (ruleChainMetaData.getFirstNodeIndex() != null) {
            firstRuleNodeId = nodes.get(ruleChainMetaData.getFirstNodeIndex()).getId();
        }
        if ((ruleChain.getFirstRuleNodeId() != null && !ruleChain.getFirstRuleNodeId().equals(firstRuleNodeId))
                || (ruleChain.getFirstRuleNodeId() == null && firstRuleNodeId != null)) {
            ruleChain.setFirstRuleNodeId(firstRuleNodeId);
            ruleChainDao.save(tenantId, ruleChain);
        }
        if (ruleChainMetaData.getConnections() != null) {
            for (NodeConnectionInfo nodeConnection : ruleChainMetaData.getConnections()) {
                EntityId from = nodes.get(nodeConnection.getFromIndex()).getId();
                EntityId to = nodes.get(nodeConnection.getToIndex()).getId();
                String type = nodeConnection.getType();
                try {
                    createRelation(tenantId, new EntityRelation(from, to, type, RelationTypeGroup.RULE_NODE));
                } catch (ExecutionException | InterruptedException e) {
                    log.warn("[{}] Failed to create rule node relation. from: [{}], to: [{}]", from, to);
                    throw new RuntimeException(e);
                }
            }
        }
        if (ruleChainMetaData.getRuleChainConnections() != null) {
            for (RuleChainConnectionInfo nodeToRuleChainConnection : ruleChainMetaData.getRuleChainConnections()) {
                EntityId from = nodes.get(nodeToRuleChainConnection.getFromIndex()).getId();
                EntityId to = nodeToRuleChainConnection.getTargetRuleChainId();
                String type = nodeToRuleChainConnection.getType();
                try {
                    createRelation(tenantId, new EntityRelation(from, to, type, RelationTypeGroup.RULE_NODE, nodeToRuleChainConnection.getAdditionalInfo()));
                } catch (ExecutionException | InterruptedException e) {
                    log.warn("[{}] Failed to create rule node to rule chain relation. from: [{}], to: [{}]", from, to);
                    throw new RuntimeException(e);
                }
            }
        }

        return loadRuleChainMetaData(tenantId, ruleChainMetaData.getRuleChainId());
    }

    @Override
    public RuleChainMetaData loadRuleChainMetaData(TenantId tenantId, RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id.");
        RuleChain ruleChain = findRuleChainById(tenantId, ruleChainId);
        if (ruleChain == null) {
            return null;
        }
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChainId);
        List<RuleNode> ruleNodes = getRuleChainNodes(tenantId, ruleChainId);
        Map<RuleNodeId, Integer> ruleNodeIndexMap = new HashMap<>();
        for (RuleNode node : ruleNodes) {
            ruleNodeIndexMap.put(node.getId(), ruleNodes.indexOf(node));
        }
        ruleChainMetaData.setNodes(ruleNodes);
        if (ruleChain.getFirstRuleNodeId() != null) {
            ruleChainMetaData.setFirstNodeIndex(ruleNodeIndexMap.get(ruleChain.getFirstRuleNodeId()));
        }
        for (RuleNode node : ruleNodes) {
            int fromIndex = ruleNodeIndexMap.get(node.getId());
            List<EntityRelation> nodeRelations = getRuleNodeRelations(tenantId, node.getId());
            for (EntityRelation nodeRelation : nodeRelations) {
                String type = nodeRelation.getType();
                if (nodeRelation.getTo().getEntityType() == EntityType.RULE_NODE) {
                    RuleNodeId toNodeId = new RuleNodeId(nodeRelation.getTo().getId());
                    int toIndex = ruleNodeIndexMap.get(toNodeId);
                    ruleChainMetaData.addConnectionInfo(fromIndex, toIndex, type);
                } else if (nodeRelation.getTo().getEntityType() == EntityType.RULE_CHAIN) {
                    RuleChainId targetRuleChainId = new RuleChainId(nodeRelation.getTo().getId());
                    ruleChainMetaData.addRuleChainConnectionInfo(fromIndex, targetRuleChainId, type, nodeRelation.getAdditionalInfo());
                }
            }
        }
        return ruleChainMetaData;
    }

    @Override
    public RuleChain findRuleChainById(TenantId tenantId, RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for search request.");
        return ruleChainDao.findById(tenantId, ruleChainId.getId());
    }

    @Override
    public RuleNode findRuleNodeById(TenantId tenantId, RuleNodeId ruleNodeId) {
        Validator.validateId(ruleNodeId, "Incorrect rule node id for search request.");
        return ruleNodeDao.findById(tenantId, ruleNodeId.getId());
    }

    @Override
    public ListenableFuture<RuleChain> findRuleChainByIdAsync(TenantId tenantId, RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for search request.");
        return ruleChainDao.findByIdAsync(tenantId, ruleChainId.getId());
    }

    @Override
    public ListenableFuture<RuleNode> findRuleNodeByIdAsync(TenantId tenantId, RuleNodeId ruleNodeId) {
        Validator.validateId(ruleNodeId, "Incorrect rule node id for search request.");
        return ruleNodeDao.findByIdAsync(tenantId, ruleNodeId.getId());
    }

    @Override
    public RuleChain getRootTenantRuleChain(TenantId tenantId) {
        return getRootRuleChainByType(tenantId, RuleChainType.SYSTEM);
    }

    private RuleChain getRootRuleChainByType(TenantId tenantId, RuleChainType type) {
        Validator.validateId(tenantId, "Incorrect tenant id for search request.");
        List<EntityRelation> relations = relationService.findByFrom(tenantId, tenantId, RelationTypeGroup.RULE_CHAIN);
        if (relations != null && !relations.isEmpty()) {
            for (EntityRelation relation : relations) {
                RuleChainId ruleChainId = new RuleChainId(relation.getTo().getId());
                RuleChain ruleChainById = findRuleChainById(tenantId, ruleChainId);
                if (type.equals(ruleChainById.getType())) {
                    return ruleChainById;
                }
            }
            return null;
        } else {
            return null;
        }
    }


    @Override
    public List<RuleNode> getRuleChainNodes(TenantId tenantId, RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for search request.");
        List<EntityRelation> relations = getRuleChainToNodeRelations(tenantId, ruleChainId);
        List<RuleNode> ruleNodes = new ArrayList<>();
        for (EntityRelation relation : relations) {
            RuleNode ruleNode = ruleNodeDao.findById(tenantId, relation.getTo().getId());
            if (ruleNode != null) {
                ruleNodes.add(ruleNode);
            } else {
                relationService.deleteRelation(tenantId, relation);
            }
        }
        return ruleNodes;
    }

    @Override
    public List<RuleNode> getReferencingRuleChainNodes(TenantId tenantId, RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for search request.");
        List<EntityRelation> relations = getNodeToRuleChainRelations(tenantId, ruleChainId);
        List<RuleNode> ruleNodes = new ArrayList<>();
        for (EntityRelation relation : relations) {
            RuleNode ruleNode = ruleNodeDao.findById(tenantId, relation.getFrom().getId());
            if (ruleNode != null) {
                ruleNodes.add(ruleNode);
            }
        }
        return ruleNodes;
    }

    @Override
    public List<EntityRelation> getRuleNodeRelations(TenantId tenantId, RuleNodeId ruleNodeId) {
        Validator.validateId(ruleNodeId, "Incorrect rule node id for search request.");
        List<EntityRelation> relations = relationService.findByFrom(tenantId, ruleNodeId, RelationTypeGroup.RULE_NODE);
        List<EntityRelation> validRelations = new ArrayList<>();
        for (EntityRelation relation : relations) {
            boolean valid = true;
            EntityType toType = relation.getTo().getEntityType();
            if (toType == EntityType.RULE_NODE || toType == EntityType.RULE_CHAIN) {
                BaseData entity;
                if (relation.getTo().getEntityType() == EntityType.RULE_NODE) {
                    entity = ruleNodeDao.findById(tenantId, relation.getTo().getId());
                } else {
                    entity = ruleChainDao.findById(tenantId, relation.getTo().getId());
                }
                if (entity == null) {
                    relationService.deleteRelation(tenantId, relation);
                    valid = false;
                }
            }
            if (valid) {
                validRelations.add(relation);
            }
        }
        return validRelations;
    }

    @Override
    public TextPageData<RuleChain> findTenantRuleChainsByType(TenantId tenantId, RuleChainType type, TextPageLink pageLink) {
        Validator.validateId(tenantId, "Incorrect tenant id for search rule chain request.");
        Validator.validatePageLink(pageLink, "Incorrect PageLink object for search rule chain request.");
        List<RuleChain> ruleChains = ruleChainDao.findRuleChainsByTenantIdAndType(tenantId.getId(), type, pageLink);
        return new TextPageData<>(ruleChains, pageLink);
    }

    @Override
    public void deleteRuleChainById(TenantId tenantId, RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for delete request.");
        RuleChain ruleChain = ruleChainDao.findById(tenantId, ruleChainId.getId());
        if (ruleChain != null) {
            if (ruleChain.isRoot()) {
                throw new DataValidationException("Deletion of Root Tenant Rule Chain is prohibited!");
            }
            if (getDefaultRootEdgeRuleChain(tenantId).getId().equals(ruleChainId)) {
                throw new DataValidationException("Can't delete rule chain that is default root for edge groups. Please assign another root rule chain first to the edge group!");
            }
        }
        checkRuleNodesAndDelete(tenantId, ruleChainId);
    }

    @Override
    public void deleteRuleChainsByTenantId(TenantId tenantId) {
        Validator.validateId(tenantId, "Incorrect tenant id for delete rule chains request.");
        tenantRuleChainsRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public RuleChain assignRuleChainToEdgeGroup(TenantId tenantId, RuleChainId ruleChainId, EntityGroupId edgeGroupId) {
        RuleChain ruleChain = findRuleChainById(tenantId, ruleChainId);
        EntityGroup edgeGroup = entityGroupService.findEntityGroupById(tenantId, edgeGroupId);
        if (edgeGroup == null) {
            throw new DataValidationException("Can't assign ruleChain to non-existent edge group!");
        }
        if (ruleChain.addAssignedEdgeGroup(edgeGroup.toShortEntityGroupInfo())) {
            try {
                createRelation(tenantId, new EntityRelation(edgeGroupId, ruleChainId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE_GROUP));
            } catch (ExecutionException | InterruptedException e) {
                log.warn("[{}] Failed to create ruleChain relation. Edge group Id: [{}]", ruleChainId, edgeGroupId);
                throw new RuntimeException(e);
            }
            ruleChain = saveRuleChain(ruleChain);
        }
        return ruleChain;
    }

    @Override
    public RuleChain unassignRuleChainFromEdgeGroup(TenantId tenantId, RuleChainId ruleChainId, EntityGroupId edgeGroupId, boolean remove) {
        RuleChain ruleChain = findRuleChainById(tenantId, ruleChainId);
        EntityGroup edgeGroup = entityGroupService.findEntityGroupById(tenantId, edgeGroupId);
        if (edgeGroup == null) {
            throw new DataValidationException("Can't unassign rule chain from non-existent edge group!");
        }
        ShortEntityGroupInfo shortEntityGroupInfo = edgeGroup.toShortEntityGroupInfo();
//        if (!remove && shortEntityGroupInfo.getRootRuleChainId() != null && shortEntityGroupInfo.getRootRuleChainId().equals(ruleChainId)) {
//            throw new DataValidationException("Can't unassign root rule chain from edge group [" + edgeGroup.getName() + "]. Please assign another root rule chain first!");
//        }
        if (ruleChain.removeAssignedEdgeGroup(shortEntityGroupInfo)) {
            try {
                deleteRelation(tenantId, new EntityRelation(edgeGroupId, ruleChainId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE_GROUP));
            } catch (ExecutionException | InterruptedException e) {
                log.warn("[{}] Failed to delete rule chain relation. Edge group id: [{}]", ruleChainId, edgeGroupId);
                throw new RuntimeException(e);
            }
            return saveRuleChain(ruleChain);
        } else {
            return ruleChain;
        }
    }

    @Override
    public void unassignEdgeGroupRuleChains(TenantId tenantId, EntityGroupId edgeGroupId) {
        log.trace("Executing unassignEdgeGroupRuleChains, edgeGroupId [{}]", edgeGroupId);
        Validator.validateId(edgeGroupId, "Incorrect edgeGroupId " + edgeGroupId);
        EntityGroup edgeGroup = entityGroupService.findEntityGroupById(tenantId, edgeGroupId);
        if (edgeGroup == null) {
            throw new DataValidationException("Can't unassign ruleChains from non-existent edge group!");
        }
        new EdgeGroupRuleChainsUnassigner(tenantId, edgeGroup).removeEntities(tenantId, edgeGroup);
    }

    @Override
    public void updateEdgeGroupRuleChains(TenantId tenantId, EntityGroupId edgeGroupId) {
        log.trace("Executing updateEdgeGroupRuleChains, edgeGroupId [{}]", edgeGroupId);
        Validator.validateId(edgeGroupId, "Incorrect edgeGroupId " + edgeGroupId);
        EntityGroup edgeGroup = entityGroupService.findEntityGroupById(tenantId, edgeGroupId);
        if (edgeGroup == null) {
            throw new DataValidationException("Can't update ruleChains for non-existent edge group!");
        }
        new EdgeGroupRuleChainsUpdater(tenantId, edgeGroup).removeEntities(tenantId, edgeGroup);
    }


    @Override
    public ListenableFuture<TimePageData<RuleChain>> findRuleChainsByTenantIdAndEdgeGroupId(TenantId tenantId, EntityGroupId edgeGroupId, TimePageLink pageLink) {
        log.trace("Executing findRuleChainsByTenantIdAndEdgeGroupId, tenantId [{}], edgeGroupId [{}], pageLink [{}]", tenantId, edgeGroupId, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validateId(edgeGroupId, "Incorrect edgeGroupId " + edgeGroupId);
        Validator.validatePageLink(pageLink, "Incorrect page link " + pageLink);
        ListenableFuture<List<RuleChain>> ruleChains = ruleChainDao.findRuleChainsByTenantIdAndEdgeGroupId(tenantId.getId(), edgeGroupId.getId(), pageLink);

        return Futures.transform(ruleChains, new Function<List<RuleChain>, TimePageData<RuleChain>>() {
            @Nullable
            @Override
            public TimePageData<RuleChain> apply(@Nullable List<RuleChain> ruleChain) {
                return new TimePageData<>(ruleChain, pageLink);
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public RuleChain getDefaultRootEdgeRuleChain(TenantId tenantId) {
        return getRootRuleChainByType(tenantId, RuleChainType.EDGE);
    }

    @Override
    public boolean setDefaultRootEdgeRuleChain(TenantId tenantId, RuleChainId ruleChainId) {
        RuleChain ruleChain = ruleChainDao.findById(tenantId, ruleChainId.getId());
        RuleChain previousDefaultRootEdgeRuleChain = getDefaultRootEdgeRuleChain(ruleChain.getTenantId());
        if (!previousDefaultRootEdgeRuleChain.getId().equals(ruleChain.getId())) {
            try {
                deleteRelation(tenantId, new EntityRelation(previousDefaultRootEdgeRuleChain.getTenantId(), previousDefaultRootEdgeRuleChain.getId(),
                        EntityRelation.CONTAINS_TYPE, RelationTypeGroup.RULE_CHAIN));
                previousDefaultRootEdgeRuleChain.setRoot(false);
                ruleChainDao.save(tenantId, previousDefaultRootEdgeRuleChain);
                createRelation(tenantId, new EntityRelation(ruleChain.getTenantId(), ruleChain.getId(),
                        EntityRelation.CONTAINS_TYPE, RelationTypeGroup.RULE_CHAIN));
                ruleChain.setRoot(true);
                ruleChainDao.save(tenantId, ruleChain);
                return true;
            } catch (ExecutionException | InterruptedException e) {
                log.warn("Failed to set default root edge rule chain, ruleChainId: [{}]", ruleChainId);
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    private void checkRuleNodesAndDelete(TenantId tenantId, RuleChainId ruleChainId) {
        List<EntityRelation> nodeRelations = getRuleChainToNodeRelations(tenantId, ruleChainId);
        for (EntityRelation relation : nodeRelations) {
            deleteRuleNode(tenantId, relation.getTo());
        }
        deleteEntityRelations(tenantId, ruleChainId);
        ruleChainDao.removeById(tenantId, ruleChainId.getId());
    }

    private List<EntityRelation> getRuleChainToNodeRelations(TenantId tenantId, RuleChainId ruleChainId) {
        return relationService.findByFrom(tenantId, ruleChainId, RelationTypeGroup.RULE_CHAIN);
    }

    private List<EntityRelation> getNodeToRuleChainRelations(TenantId tenantId, RuleChainId ruleChainId) {
        return relationService.findByTo(tenantId, ruleChainId, RelationTypeGroup.RULE_NODE);
    }

    private void deleteRuleNode(TenantId tenantId, EntityId entityId) {
        deleteEntityRelations(tenantId, entityId);
        ruleNodeDao.removeById(tenantId, entityId.getId());
    }

    private void createRelation(TenantId tenantId, EntityRelation relation) throws ExecutionException, InterruptedException {
        log.debug("Creating relation: {}", relation);
        relationService.saveRelation(tenantId, relation);
    }

    private void deleteRelation(TenantId tenantId, EntityRelation relation) throws ExecutionException, InterruptedException {
        log.debug("Deleting relation: {}", relation);
        relationService.deleteRelation(tenantId, relation);
    }

    private RuleChain updateAssignedEdgeGroup(TenantId tenantId, RuleChainId ruleChainId, EntityGroup edgeGroup) {
        RuleChain ruleChain = findRuleChainById(tenantId, ruleChainId);
        if (ruleChain.updateAssignedEdgeGroup(edgeGroup.toShortEntityGroupInfo())) {
            return saveRuleChain(ruleChain);
        } else {
            return ruleChain;
        }
    }

    private DataValidator<RuleChain> ruleChainValidator =
            new DataValidator<RuleChain>() {
                @Override
                protected void validateDataImpl(TenantId tenantId, RuleChain ruleChain) {
                    if (StringUtils.isEmpty(ruleChain.getName())) {
                        throw new DataValidationException("Rule chain name should be specified!");
                    }
                    if (ruleChain.getType() == null) {
                        ruleChain.setType(RuleChainType.SYSTEM);
                    }
                    if (ruleChain.getTenantId() == null || ruleChain.getTenantId().isNullUid()) {
                        throw new DataValidationException("Rule chain should be assigned to tenant!");
                    }
                    Tenant tenant = tenantDao.findById(tenantId, ruleChain.getTenantId().getId());
                    if (tenant == null) {
                        throw new DataValidationException("Rule chain is referencing to non-existent tenant!");
                    }
                    if (ruleChain.isRoot() && RuleChainType.SYSTEM.equals(ruleChain.getType())) {
                        RuleChain rootRuleChain = getRootTenantRuleChain(ruleChain.getTenantId());
                        if (rootRuleChain != null && !rootRuleChain.getId().equals(ruleChain.getId())) {
                            throw new DataValidationException("Another root rule chain is present in scope of current tenant!");
                        }
                    }
                    if (ruleChain.isRoot() && RuleChainType.EDGE.equals(ruleChain.getType())) {
                        RuleChain defaultRootEdgeRuleChain = getDefaultRootEdgeRuleChain(ruleChain.getTenantId());
                        if (defaultRootEdgeRuleChain != null && !defaultRootEdgeRuleChain.getId().equals(ruleChain.getId())) {
                            throw new DataValidationException("Another default root edge rule chain is present in scope of current tenant!");
                        }
                    }
                }
            };

    private PaginatedRemover<TenantId, RuleChain> tenantRuleChainsRemover =
            new PaginatedRemover<TenantId, RuleChain>() {

                @Override
                protected List<RuleChain> findEntities(TenantId tenantId, TenantId id, TextPageLink pageLink) {
                    return ruleChainDao.findRuleChainsByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, RuleChain entity) {
                    checkRuleNodesAndDelete(tenantId, entity.getId());
                }
            };

    private class EdgeGroupRuleChainsUnassigner extends TimePaginatedRemover<EntityGroup, RuleChain> {

        private TenantId tenantId;
        private EntityGroup edgeGroup;

        EdgeGroupRuleChainsUnassigner(TenantId tenantId, EntityGroup edgeGroup) {
            this.tenantId = tenantId;
            this.edgeGroup = edgeGroup;
        }

        @Override
        protected List<RuleChain> findEntities(TenantId tenantId, EntityGroup edgeGroup, TimePageLink pageLink) {
            try {
                return ruleChainDao.findRuleChainsByTenantIdAndEdgeGroupId(this.tenantId.getId(), edgeGroup.getId().getId(), pageLink).get();
            } catch (InterruptedException | ExecutionException e) {
                log.warn("Failed to get ruleChains by tenantId [{}] and edgeGroupId [{}].", tenantId.getId(), edgeGroup.getId().getId());
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void removeEntity(TenantId tenantId, RuleChain entity) {
            unassignRuleChainFromEdgeGroup(this.tenantId, new RuleChainId(entity.getUuidId()), this.edgeGroup.getId(), true);
        }
    }

    private class EdgeGroupRuleChainsUpdater extends TimePaginatedRemover<EntityGroup, RuleChain> {

        private TenantId tenantId;
        private EntityGroup edgeGroup;

        EdgeGroupRuleChainsUpdater(TenantId tenantId, EntityGroup edgeGroup) {
            this.tenantId = tenantId;
            this.edgeGroup = edgeGroup;
        }

        @Override
        protected List<RuleChain> findEntities(TenantId tenantId, EntityGroup edgeGroup, TimePageLink pageLink) {
            try {
                return ruleChainDao.findRuleChainsByTenantIdAndEdgeGroupId(this.tenantId.getId(), edgeGroup.getId().getId(), pageLink).get();
            } catch (InterruptedException | ExecutionException e) {
                log.warn("Failed to get ruleChains by tenantId [{}] and edgeGroupId [{}].", tenantId.getId(), edgeGroup.getId().getId());
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void removeEntity(TenantId tenantId, RuleChain entity) {
            updateAssignedEdgeGroup(this.tenantId, new RuleChainId(entity.getUuidId()), this.edgeGroup);
        }

    }
}
