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

import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.DefaultRuleChainCreateRequest;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.service.entitiy.SimpleTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;

public interface TbRuleChainNotifyService extends SimpleTbEntityService<RuleChain> {

    RuleChain saveDefaultByName(TenantId tenantId, DefaultRuleChainCreateRequest request, SecurityUser user) throws ThingsboardException;

    RuleChain setRootRuleChain(TenantId tenantId, RuleChain ruleChain, SecurityUser user) throws ThingsboardException;

    RuleChainMetaData saveRuleChainMetaData(TenantId tenantId, RuleChain ruleChain, RuleChainMetaData ruleChainMetaData,
                                            boolean updateRelated, SecurityUser user) throws ThingsboardException;

    RuleChain assignRuleChainToEdge(TenantId tenantId, RuleChain ruleChain, Edge edge,
                                    SecurityUser user) throws ThingsboardException;
    RuleChain unassignRuleChainToEdge(TenantId tenantId, RuleChain ruleChain, Edge edge,
                                            SecurityUser user) throws ThingsboardException;

    RuleChain setEdgeTemplateRootRuleChain(TenantId tenantId, RuleChain ruleChain, SecurityUser user) throws ThingsboardException;

    RuleChain setAutoAssignToEdgeRuleChain(TenantId tenantId, RuleChain ruleChain, SecurityUser user) throws ThingsboardException;

    RuleChain unsetAutoAssignToEdgeRuleChain(TenantId tenantId, RuleChain ruleChain, SecurityUser user) throws ThingsboardException;
}
