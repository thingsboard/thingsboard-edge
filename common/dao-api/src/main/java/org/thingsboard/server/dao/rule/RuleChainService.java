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
package org.thingsboard.server.dao.rule;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainData;
import org.thingsboard.server.common.data.rule.RuleChainImportResult;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleChainUpdateResult;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Created by igor on 3/12/18.
 */
public interface RuleChainService extends EntityDaoService {

    RuleChain saveRuleChain(RuleChain ruleChain);

    boolean setRootRuleChain(TenantId tenantId, RuleChainId ruleChainId);

    RuleChainUpdateResult saveRuleChainMetaData(TenantId tenantId, RuleChainMetaData ruleChainMetaData, Function<RuleNode, RuleNode> ruleNodeUpdater);

    RuleChainMetaData loadRuleChainMetaData(TenantId tenantId, RuleChainId ruleChainId);

    RuleChain findRuleChainById(TenantId tenantId, RuleChainId ruleChainId);

    RuleNode findRuleNodeById(TenantId tenantId, RuleNodeId ruleNodeId);

    ListenableFuture<RuleChain> findRuleChainByIdAsync(TenantId tenantId, RuleChainId ruleChainId);

    ListenableFuture<RuleNode> findRuleNodeByIdAsync(TenantId tenantId, RuleNodeId ruleNodeId);

    RuleChain getRootTenantRuleChain(TenantId tenantId);

    List<RuleNode> getRuleChainNodes(TenantId tenantId, RuleChainId ruleChainId);

    List<RuleNode> getReferencingRuleChainNodes(TenantId tenantId, RuleChainId ruleChainId);

    List<EntityRelation> getRuleNodeRelations(TenantId tenantId, RuleNodeId ruleNodeId);

    PageData<RuleChain> findTenantRuleChainsByType(TenantId tenantId, RuleChainType type, PageLink pageLink);

    Collection<RuleChain> findTenantRuleChainsByTypeAndName(TenantId tenantId, RuleChainType type, String name);

    void deleteRuleChainById(TenantId tenantId, RuleChainId ruleChainId);

    void deleteRuleChainsByTenantId(TenantId tenantId);

    RuleChainData exportTenantRuleChains(TenantId tenantId, PageLink pageLink) throws ThingsboardException;

    List<RuleChainImportResult> importTenantRuleChains(TenantId tenantId, RuleChainData ruleChainData, boolean overwrite, Function<RuleNode, RuleNode> ruleNodeUpdater);

    RuleChain assignRuleChainToEdge(TenantId tenantId, RuleChainId ruleChainId, EdgeId edgeId);

    RuleChain unassignRuleChainFromEdge(TenantId tenantId, RuleChainId ruleChainId, EdgeId edgeId, boolean remove);

    PageData<RuleChain> findRuleChainsByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink);

    RuleChain getEdgeTemplateRootRuleChain(TenantId tenantId);

    boolean setEdgeTemplateRootRuleChain(TenantId tenantId, RuleChainId ruleChainId);

    boolean setAutoAssignToEdgeRuleChain(TenantId tenantId, RuleChainId ruleChainId);

    boolean unsetAutoAssignToEdgeRuleChain(TenantId tenantId, RuleChainId ruleChainId);

    PageData<RuleChain> findAutoAssignToEdgeRuleChainsByTenantId(TenantId tenantId, PageLink pageLink);

    List<RuleNode> findRuleNodesByTenantIdAndType(TenantId tenantId, String name, String toString);

    List<RuleNode> findRuleNodesByTenantIdAndType(TenantId tenantId, String type);

    PageData<RuleNode> findAllRuleNodesByType(String type, PageLink pageLink);

    PageData<RuleNode> findAllRuleNodesByTypeAndVersionLessThan(String type, int version, PageLink pageLink);

    RuleNode saveRuleNode(TenantId tenantId, RuleNode ruleNode);

    void deleteRuleNodes(TenantId tenantId, RuleChainId ruleChainId);

}
