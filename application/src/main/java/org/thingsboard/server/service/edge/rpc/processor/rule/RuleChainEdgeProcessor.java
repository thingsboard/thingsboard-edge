/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.edge.rpc.processor.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import static org.thingsboard.server.service.edge.DefaultEdgeNotificationService.EDGE_IS_ROOT_BODY_KEY;

@Component
@Slf4j
@TbCoreComponent
public class RuleChainEdgeProcessor extends BaseEdgeProcessor {

    public DownlinkMsg convertRuleChainEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        RuleChainId ruleChainId = new RuleChainId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
                RuleChain ruleChain = ruleChainService.findRuleChainById(edgeEvent.getTenantId(), ruleChainId);
                if (ruleChain != null) {
                    boolean isRoot = false;
                    if (edgeEvent.getBody() != null && edgeEvent.getBody().get(EDGE_IS_ROOT_BODY_KEY) != null) {
                        try {
                            isRoot = Boolean.parseBoolean(edgeEvent.getBody().get(EDGE_IS_ROOT_BODY_KEY).asText());
                        } catch (Exception ignored) {}
                    }
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    RuleChainUpdateMsg ruleChainUpdateMsg =
                            ruleChainMsgConstructor.constructRuleChainUpdatedMsg(msgType, ruleChain, isRoot, edgeVersion);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addRuleChainUpdateMsg(ruleChainUpdateMsg)
                            .build();
                }
                break;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addRuleChainUpdateMsg(ruleChainMsgConstructor.constructRuleChainDeleteMsg(ruleChainId))
                        .build();
                break;
        }
        return downlinkMsg;
    }

    public DownlinkMsg convertRuleChainMetadataEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        RuleChainId ruleChainId = new RuleChainId(edgeEvent.getEntityId());
        RuleChain ruleChain = ruleChainService.findRuleChainById(edgeEvent.getTenantId(), ruleChainId);
        DownlinkMsg downlinkMsg = null;
        if (ruleChain != null) {
            RuleChainMetaData ruleChainMetaData = ruleChainService.loadRuleChainMetaData(edgeEvent.getTenantId(), ruleChainId);
            UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
            RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg =
                    ruleChainMsgConstructor.constructRuleChainMetadataUpdatedMsg(edgeEvent.getTenantId(), msgType, ruleChainMetaData, edgeVersion);
            if (ruleChainMetadataUpdateMsg != null) {
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addRuleChainMetadataUpdateMsg(ruleChainMetadataUpdateMsg)
                        .build();
            }
        }
        return downlinkMsg;
    }
}
