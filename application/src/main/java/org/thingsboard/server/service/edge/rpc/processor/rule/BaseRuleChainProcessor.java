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
package org.thingsboard.server.service.edge.rpc.processor.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.function.Function;

@Slf4j
public class BaseRuleChainProcessor extends BaseEdgeProcessor {

    @Autowired
    private DataValidator<RuleChain> ruleChainValidator;

    protected Pair<Boolean, Boolean> saveOrUpdateRuleChain(TenantId tenantId, RuleChainId ruleChainId, RuleChainUpdateMsg ruleChainUpdateMsg, RuleChainType ruleChainType) {
        boolean created = false;
        RuleChain ruleChainFromDb = edgeCtx.getRuleChainService().findRuleChainById(tenantId, ruleChainId);
        if (ruleChainFromDb == null) {
            created = true;
        }

        RuleChain ruleChain = JacksonUtil.fromString(ruleChainUpdateMsg.getEntity(), RuleChain.class, true);
        if (ruleChain == null) {
            throw new RuntimeException("[{" + tenantId + "}] ruleChainUpdateMsg {" + ruleChainUpdateMsg + "} cannot be converted to rule chain");
        }
        boolean isRoot = ruleChain.isRoot();
        if (RuleChainType.CORE.equals(ruleChainType)) {
            ruleChain.setRoot(false);
        } else {
            ruleChain.setRoot(ruleChainFromDb == null ? false : ruleChainFromDb.isRoot());
        }
        ruleChain.setType(ruleChainType);

        ruleChainValidator.validate(ruleChain, RuleChain::getTenantId);
        if (created) {
            ruleChain.setId(ruleChainId);
        }
        edgeCtx.getRuleChainService().saveRuleChain(ruleChain);
        return Pair.of(created, isRoot);
    }

    protected void saveOrUpdateRuleChainMetadata(TenantId tenantId, RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg) {
        RuleChainMetaData ruleChainMetadata = JacksonUtil.fromString(ruleChainMetadataUpdateMsg.getEntity(), RuleChainMetaData.class, true);
        if (ruleChainMetadata == null) {
            throw new RuntimeException("[{" + tenantId + "}] ruleChainMetadataUpdateMsg {" + ruleChainMetadataUpdateMsg + "} cannot be converted to rule chain metadata");
        }
        if (!ruleChainMetadata.getNodes().isEmpty()) {
            ruleChainMetadata.setVersion(null);
            for (RuleNode ruleNode : ruleChainMetadata.getNodes()) {
                ruleNode.setRuleChainId(null);
                ruleNode.setId(null);
            }
            edgeCtx.getRuleChainService().saveRuleChainMetaData(tenantId, ruleChainMetadata, Function.identity(), true);
        }
    }
}
