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

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleNodeState;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.exception.DataValidationException;

@Service
@Slf4j
public class BaseRuleNodeStateService extends AbstractEntityService implements RuleNodeStateService {

    @Autowired
    private RuleNodeStateDao ruleNodeStateDao;

    @Override
    public PageData<RuleNodeState> findByRuleNodeId(TenantId tenantId, RuleNodeId ruleNodeId, PageLink pageLink) {
        if (tenantId == null) {
            throw new DataValidationException("Tenant id should be specified!.");
        }
        if (ruleNodeId == null) {
            throw new DataValidationException("RuleNode id should be specified!.");
        }
        return ruleNodeStateDao.findByRuleNodeId(ruleNodeId.getId(), pageLink);
    }

    @Override
    public RuleNodeState findByRuleNodeIdAndEntityId(TenantId tenantId, RuleNodeId ruleNodeId, EntityId entityId) {
        if (tenantId == null) {
            throw new DataValidationException("Tenant id should be specified!.");
        }
        if (ruleNodeId == null) {
            throw new DataValidationException("RuleNode id should be specified!.");
        }
        if (entityId == null) {
            throw new DataValidationException("Entity id should be specified!.");
        }
        return ruleNodeStateDao.findByRuleNodeIdAndEntityId(ruleNodeId.getId(), entityId.getId());
    }

    @Override
    public RuleNodeState save(TenantId tenantId, RuleNodeState ruleNodeState) {
        if (tenantId == null) {
            throw new DataValidationException("Tenant id should be specified!.");
        }
        return saveOrUpdate(tenantId, ruleNodeState, false);
    }

    @Override
    public void removeByRuleNodeId(TenantId tenantId, RuleNodeId ruleNodeId) {
        if (tenantId == null) {
            throw new DataValidationException("Tenant id should be specified!.");
        }
        if (ruleNodeId == null) {
            throw new DataValidationException("Rule node id should be specified!.");
        }
        ruleNodeStateDao.removeByRuleNodeId(ruleNodeId.getId());
    }

    @Override
    public void removeByRuleNodeIdAndEntityId(TenantId tenantId, RuleNodeId ruleNodeId, EntityId entityId) {
        if (tenantId == null) {
            throw new DataValidationException("Tenant id should be specified!.");
        }
        if (ruleNodeId == null) {
            throw new DataValidationException("Rule node id should be specified!.");
        }
        if (entityId == null) {
            throw new DataValidationException("Entity id should be specified!.");
        }
        ruleNodeStateDao.removeByRuleNodeIdAndEntityId(ruleNodeId.getId(), entityId.getId());
    }

    public RuleNodeState saveOrUpdate(TenantId tenantId, RuleNodeState ruleNodeState, boolean update) {
        try {
            if (update) {
                RuleNodeState old = ruleNodeStateDao.findByRuleNodeIdAndEntityId(ruleNodeState.getRuleNodeId().getId(), ruleNodeState.getEntityId().getId());
                if (old != null && !old.getId().equals(ruleNodeState.getId())) {
                    ruleNodeState.setId(old.getId());
                    ruleNodeState.setCreatedTime(old.getCreatedTime());
                }
            }
            return ruleNodeStateDao.save(tenantId, ruleNodeState);
        } catch (Exception t) {
            ConstraintViolationException e = DaoUtil.extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("rule_node_state_unq_key")) {
                if (!update) {
                    return saveOrUpdate(tenantId, ruleNodeState, true);
                } else {
                    throw new DataValidationException("Rule node state for such rule node id and entity id already exists!");
                }
            } else {
                throw t;
            }
        }
    }
}
