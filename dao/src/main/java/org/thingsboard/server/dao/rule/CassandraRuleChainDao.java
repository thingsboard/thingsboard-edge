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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.nosql.RuleChainEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_CHAIN_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_CHAIN_BY_TENANT_BY_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_CHAIN_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_CHAIN_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_CHAIN_TYPE_PROPERTY;

@Component
@Slf4j
@NoSqlDao
public class CassandraRuleChainDao extends CassandraAbstractSearchTextDao<RuleChainEntity, RuleChain> implements RuleChainDao {

    @Autowired
    private RelationDao relationDao;

    @Override
    protected Class<RuleChainEntity> getColumnFamilyClass() {
        return RuleChainEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return RULE_CHAIN_COLUMN_FAMILY_NAME;
    }

    @Override
    public List<RuleChain> findRuleChainsByTenantId(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find rule chains by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<RuleChainEntity> ruleChainEntities = findPageWithTextSearch(new TenantId(tenantId), RULE_CHAIN_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Collections.singletonList(eq(RULE_CHAIN_TENANT_ID_PROPERTY, tenantId)),
                pageLink);

        log.trace("Found rule chains [{}] by tenantId [{}] and pageLink [{}]", ruleChainEntities, tenantId, pageLink);
        return DaoUtil.convertDataList(ruleChainEntities);
    }

    @Override
    public List<RuleChain> findRuleChainsByTenantIdAndType(UUID tenantId, RuleChainType type, TextPageLink pageLink) {
        log.debug("Try to find rule chains by tenantId [{}], type [{}] and pageLink [{}]", tenantId, type, pageLink);
        List<RuleChainEntity> ruleChainEntities = findPageWithTextSearch(new TenantId(tenantId), RULE_CHAIN_BY_TENANT_BY_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(RULE_CHAIN_TYPE_PROPERTY, type),
                        eq(RULE_CHAIN_TENANT_ID_PROPERTY, tenantId)),
                pageLink);
        log.trace("Found rule chains [{}] by tenantId [{}] and pageLink [{}]", ruleChainEntities, tenantId, pageLink);
        return DaoUtil.convertDataList(ruleChainEntities);
    }

    @Override
    public ListenableFuture<List<RuleChain>> findRuleChainsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, TimePageLink pageLink) {
        log.debug("Try to find rule chains by tenantId [{}], edgeId [{}] and pageLink [{}]", tenantId, edgeId, pageLink);
        ListenableFuture<List<EntityRelation>> relations = relationDao.findRelations(new TenantId(tenantId), new EdgeId(edgeId), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE, EntityType.RULE_CHAIN, pageLink);
        return Futures.transformAsync(relations, input -> {
            List<ListenableFuture<RuleChain>> ruleChainFutures = new ArrayList<>(input.size());
            for (EntityRelation relation : input) {
                ruleChainFutures.add(findByIdAsync(new TenantId(tenantId), relation.getTo().getId()));
            }
            return Futures.successfulAsList(ruleChainFutures);
        }, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<List<RuleChain>> findAutoAssignToEdgeRuleChainsByTenantId(UUID tenantId) {
        log.debug("Try to find auto assign to edge rule chains by tenantId [{}]", tenantId);
        ListenableFuture<List<EntityRelation>> relations =
                relationDao.findAllByFromAndType(new TenantId(tenantId), new TenantId(tenantId), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE_AUTO_ASSIGN_RULE_CHAIN);
        return Futures.transformAsync(relations, input -> {
            List<ListenableFuture<RuleChain>> ruleChainFutures = new ArrayList<>(input.size());
            for (EntityRelation relation : input) {
                ruleChainFutures.add(findByIdAsync(new TenantId(tenantId), relation.getTo().getId()));
            }
            return Futures.successfulAsList(ruleChainFutures);
        }, MoreExecutors.directExecutor());
    }

}
