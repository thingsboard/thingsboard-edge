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
package org.thingsboard.server.dao.sql.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.RuleChainEntity;
import org.thingsboard.server.dao.rule.RuleChainDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaRuleChainDao extends JpaAbstractSearchTextDao<RuleChainEntity, RuleChain> implements RuleChainDao {

    @Autowired
    private RuleChainRepository ruleChainRepository;

    @Override
    protected Class<RuleChainEntity> getEntityClass() {
        return RuleChainEntity.class;
    }

    @Override
    protected JpaRepository<RuleChainEntity, UUID> getRepository() {
        return ruleChainRepository;
    }

    @Override
    public PageData<RuleChain> findRuleChainsByTenantId(UUID tenantId, PageLink pageLink) {
        log.debug("Try to find rule chains by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        return DaoUtil.toPageData(ruleChainRepository
                .findByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<RuleChain> findRuleChainsByTenantIdAndType(UUID tenantId, RuleChainType type, PageLink pageLink) {
        log.debug("Try to find rule chains by tenantId [{}], type [{}] and pageLink [{}]", tenantId, type, pageLink);
        return DaoUtil.toPageData(ruleChainRepository
                .findByTenantIdAndType(
                        tenantId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public RuleChain findRootRuleChainByTenantIdAndType(UUID tenantId, RuleChainType type) {
        log.debug("Try to find root rule chain by tenantId [{}] and type [{}]", tenantId, type);
        return DaoUtil.getData(ruleChainRepository.findByTenantIdAndTypeAndRootIsTrue(tenantId, type));
    }

    @Override
    public PageData<RuleChain> findRuleChainsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, PageLink pageLink) {
        log.debug("Try to find rule chains by tenantId [{}], edgeId [{}] and pageLink [{}]", tenantId, edgeId, pageLink);
        return DaoUtil.toPageData(ruleChainRepository
                .findByTenantIdAndEdgeId(
                        tenantId,
                        edgeId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<RuleChain> findAutoAssignToEdgeRuleChainsByTenantId(UUID tenantId, PageLink pageLink) {
        log.debug("Try to find auto assign to edge rule chains by tenantId [{}]", tenantId);
        return DaoUtil.toPageData(ruleChainRepository
                .findAutoAssignByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Collection<RuleChain> findByTenantIdAndTypeAndName(TenantId tenantId, RuleChainType type, String name) {
        return DaoUtil.convertDataList(ruleChainRepository.findByTenantIdAndTypeAndName(tenantId.getId(), type, name));
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return ruleChainRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public RuleChain findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(ruleChainRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public PageData<RuleChain> findByTenantId(UUID tenantId, PageLink pageLink) {
        return findRuleChainsByTenantId(tenantId, pageLink);
    }

    @Override
    public RuleChainId getExternalIdByInternal(RuleChainId internalId) {
        return Optional.ofNullable(ruleChainRepository.getExternalIdById(internalId.getId()))
                .map(RuleChainId::new).orElse(null);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.RULE_CHAIN;
    }

}
