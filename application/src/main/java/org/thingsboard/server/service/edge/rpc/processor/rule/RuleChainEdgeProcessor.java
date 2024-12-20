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
package org.thingsboard.server.service.edge.rpc.processor.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import static org.thingsboard.server.dao.edge.EdgeServiceImpl.EDGE_IS_ROOT_BODY_KEY;

@Slf4j
@Component
@TbCoreComponent
public class RuleChainEdgeProcessor extends BaseEdgeProcessor {

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent) {
        RuleChainId ruleChainId = new RuleChainId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED, ASSIGNED_TO_EDGE -> {
                RuleChain ruleChain = edgeCtx.getRuleChainService().findRuleChainById(edgeEvent.getTenantId(), ruleChainId);
                if (ruleChain != null) {
                    boolean isRoot = false;
                    if (edgeEvent.getBody() != null && edgeEvent.getBody().get(EDGE_IS_ROOT_BODY_KEY) != null) {
                        try {
                            isRoot = Boolean.parseBoolean(edgeEvent.getBody().get(EDGE_IS_ROOT_BODY_KEY).asText());
                        } catch (Exception ignored) {
                        }
                    }
                    if (!isRoot) {
                        Edge edge = edgeCtx.getEdgeService().findEdgeById(edgeEvent.getTenantId(), edgeEvent.getEdgeId());
                        isRoot = edge.getRootRuleChainId().equals(ruleChainId);
                    }
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    RuleChainUpdateMsg ruleChainUpdateMsg = EdgeMsgConstructorUtils.constructRuleChainUpdatedMsg(msgType, ruleChain, isRoot);

                    DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addRuleChainUpdateMsg(ruleChainUpdateMsg);

                    RuleChainMetaData ruleChainMetaData = edgeCtx.getRuleChainService().loadRuleChainMetaData(edgeEvent.getTenantId(), ruleChainId);
                    RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg = EdgeMsgConstructorUtils
                            .constructRuleChainMetadataUpdatedMsg(msgType, ruleChainMetaData);
                    builder.addRuleChainMetadataUpdateMsg(ruleChainMetadataUpdateMsg);
                    downlinkMsg = builder.build();
                }
            }
            case DELETED, UNASSIGNED_FROM_EDGE -> downlinkMsg = DownlinkMsg.newBuilder()
                    .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                    .addRuleChainUpdateMsg(EdgeMsgConstructorUtils.constructRuleChainDeleteMsg(ruleChainId))
                    .build();
        }
        return downlinkMsg;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.RULE_CHAIN;
    }

}
