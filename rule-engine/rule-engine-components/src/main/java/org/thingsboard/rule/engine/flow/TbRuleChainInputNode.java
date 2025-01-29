/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.rule.engine.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RuleNode(
        type = ComponentType.FLOW,
        name = "rule chain",
        configClazz = TbRuleChainInputNodeConfiguration.class,
        version = 1,
        nodeDescription = "Transfers the message to another rule chain",
        nodeDetails = "The incoming message is forwarded to the input node of target rule chain. " +
                "If 'Forward message to the originator's default rule chain' is enabled, " +
                "then target rule chain might be resolved dynamically based on incoming message originator. " +
                "In this case rule chain specified in the configuration will be used as fallback rule chain.<br><br>" +
                "Output connections: <i>Any connection(s) produced by output node(s) in the target rule chain.</i>",
        configDirective = "tbFlowNodeRuleChainInputConfig",
        relationTypes = {},
        ruleChainNode = true,
        customRelations = true
)
public class TbRuleChainInputNode implements TbNode {

    private RuleChainId ruleChainId;
    private boolean forwardMsgToDefaultRuleChain;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        TbRuleChainInputNodeConfiguration config = TbNodeUtils.convert(configuration, TbRuleChainInputNodeConfiguration.class);
        if (config.getRuleChainId() == null) {
            throw new TbNodeException("Rule chain must be set!", true);
        }
        UUID ruleChainUUID;
        try {
            ruleChainUUID = UUID.fromString(config.getRuleChainId());
        } catch (Exception e) {
            throw new TbNodeException("Failed to parse rule chain id: " + config.getRuleChainId(), true);
        }
        ruleChainId = new RuleChainId(ruleChainUUID);
        ctx.checkTenantEntity(ruleChainId);
        forwardMsgToDefaultRuleChain = config.isForwardMsgToDefaultRuleChain();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        RuleChainId targetRuleChainId = forwardMsgToDefaultRuleChain ?
                getOriginatorDefaultRuleChainId(ctx, msg).orElse(ruleChainId) : ruleChainId;
        ctx.input(msg, targetRuleChainId);
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        switch (fromVersion) {
            case 0 -> {
                if (!oldConfiguration.has("forwardMsgToDefaultRuleChain")) {
                    hasChanges = true;
                    ((ObjectNode) oldConfiguration).put("forwardMsgToDefaultRuleChain", false);
                }
            }
            default -> {
            }
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

    private Optional<RuleChainId> getOriginatorDefaultRuleChainId(TbContext ctx, TbMsg msg) {
        return Optional.ofNullable(
                switch (msg.getOriginator().getEntityType()) {
                    case DEVICE ->
                            ctx.getDeviceProfileCache().get(ctx.getTenantId(), (DeviceId) msg.getOriginator()).getDefaultRuleChainId();
                    case ASSET ->
                            ctx.getAssetProfileCache().get(ctx.getTenantId(), (AssetId) msg.getOriginator()).getDefaultRuleChainId();
                    default -> null;
                });
    }
}
