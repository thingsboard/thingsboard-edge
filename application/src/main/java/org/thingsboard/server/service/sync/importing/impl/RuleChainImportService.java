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
package org.thingsboard.server.service.sync.importing.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.exporting.data.RuleChainExportData;
import org.thingsboard.server.utils.ThrowingRunnable;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class RuleChainImportService extends BaseEntityImportService<RuleChainId, RuleChain, RuleChainExportData> {

    private final RuleChainService ruleChainService;

    @Override
    protected void setOwner(TenantId tenantId, RuleChain ruleChain, NewIdProvider idProvider) {
        ruleChain.setTenantId(tenantId);
    }

    @Override
    protected RuleChain prepareAndSave(TenantId tenantId, RuleChain ruleChain, RuleChainExportData exportData, NewIdProvider idProvider) {
        RuleChainMetaData metaData = exportData.getMetaData();
        Optional.ofNullable(metaData.getNodes()).orElse(Collections.emptyList())
                .forEach(ruleNode -> {
                    ruleNode.setId(null);
                    ruleNode.setRuleChainId(null);
                    JsonNode ruleNodeConfig = ruleNode.getConfiguration();
                    Optional.ofNullable(ruleNodeConfig)
                            .flatMap(config -> Optional.ofNullable(config.get("ruleChainId")).filter(JsonNode::isTextual))
                            .map(JsonNode::asText).map(UUID::fromString)
                            .ifPresent(otherRuleChainUuid -> {
                                ((ObjectNode) ruleNodeConfig).set("ruleChainId", new TextNode(
                                        idProvider.get(rc -> new RuleChainId(otherRuleChainUuid)).toString()
                                ));
                                ruleNode.setConfiguration(ruleNodeConfig);
                            });
                });
        Optional.ofNullable(metaData.getRuleChainConnections()).orElse(Collections.emptyList())
                .forEach(ruleChainConnectionInfo -> {
                    ruleChainConnectionInfo.setTargetRuleChainId(idProvider.get(rc -> ruleChainConnectionInfo.getTargetRuleChainId()));
                });
        ruleChain.setFirstRuleNodeId(null);

        if (ruleChain.getId() != null) {
            // FIXME [viacheslav]: maybe no need to delete
            ruleChainService.deleteRuleNodes(tenantId, ruleChain.getId());
        }
        ruleChain = ruleChainService.saveRuleChain(ruleChain);
        exportData.getMetaData().setRuleChainId(ruleChain.getId());
        ruleChainService.saveRuleChainMetaData(tenantId, exportData.getMetaData());
        return ruleChainService.findRuleChainById(tenantId, ruleChain.getId());
    }

    @Override
    protected ThrowingRunnable getCallback(SecurityUser user, RuleChain savedRuleChain, RuleChain oldRuleChain) {
        return super.getCallback(user, savedRuleChain, oldRuleChain).andThen(() -> {
            if (savedRuleChain.getType() == RuleChainType.CORE) {
                clusterService.broadcastEntityStateChangeEvent(user.getTenantId(), savedRuleChain.getId(),
                        oldRuleChain == null ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
            }
            if (savedRuleChain.getType() == RuleChainType.EDGE && oldRuleChain != null) {
                entityActionService.sendEntityNotificationMsgToEdgeService(user.getTenantId(), savedRuleChain.getId(), EdgeEventActionType.UPDATED);
            }
        });
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.RULE_CHAIN;
    }

}
