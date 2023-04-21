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
package org.thingsboard.server.actors.ruleChain;

import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.actors.TbRuleNodeUpdateException;
import org.thingsboard.server.actors.shared.ComponentMsgProcessor;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.RuleNodeException;
import org.thingsboard.server.common.msg.queue.RuleNodeInfo;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.gen.transport.TransportProtos;

/**
 * @author Andrew Shvayka
 */
public class RuleNodeActorMessageProcessor extends ComponentMsgProcessor<RuleNodeId> {

    private final String ruleChainName;
    private final TbApiUsageReportClient apiUsageClient;
    private final DefaultTbContext defaultCtx;
    private RuleNode ruleNode;
    private TbNode tbNode;
    private RuleNodeInfo info;

    RuleNodeActorMessageProcessor(TenantId tenantId, String ruleChainName, RuleNodeId ruleNodeId, ActorSystemContext systemContext
            , TbActorRef parent, TbActorRef self) {
        super(systemContext, tenantId, ruleNodeId);
        this.apiUsageClient = systemContext.getApiUsageClient();
        this.ruleChainName = ruleChainName;
        this.ruleNode = systemContext.getRuleChainService().findRuleNodeById(tenantId, entityId);
        this.defaultCtx = new DefaultTbContext(systemContext, ruleChainName, new RuleNodeCtx(tenantId, parent, self, ruleNode));
        this.info = new RuleNodeInfo(ruleNodeId, ruleChainName, ruleNode != null ? ruleNode.getName() : "Unknown");
    }

    @Override
    public void start(TbActorCtx context) throws Exception {
        if (isMyNode()) {
            tbNode = initComponent(ruleNode);
            if (tbNode != null) {
                state = ComponentLifecycleState.ACTIVE;
            }
        }
    }

    @Override
    public void onUpdate(TbActorCtx context) throws Exception {
        if (isMyNode()) {
            RuleNode newRuleNode = systemContext.getRuleChainService().findRuleNodeById(tenantId, entityId);
            this.info = new RuleNodeInfo(entityId, ruleChainName, newRuleNode != null ? newRuleNode.getName() : "Unknown");
            boolean restartRequired = state != ComponentLifecycleState.ACTIVE ||
                    !(ruleNode.getType().equals(newRuleNode.getType()) && ruleNode.getConfiguration().equals(newRuleNode.getConfiguration()));
            this.ruleNode = newRuleNode;
            this.defaultCtx.updateSelf(newRuleNode);
            if (restartRequired) {
                if (tbNode != null) {
                    tbNode.destroy();
                }
                try {
                    start(context);
                } catch (Exception e) {
                    throw new TbRuleNodeUpdateException("Failed to update rule node", e);
                }
            }
        } else {
            if (tbNode != null) {
                stop(null);
                tbNode = null;
            }
        }
    }

    @Override
    public void stop(TbActorCtx context) {
        if (tbNode != null) {
            tbNode.destroy();
            state = ComponentLifecycleState.SUSPENDED;
        }
    }

    @Override
    public void onPartitionChangeMsg(PartitionChangeMsg msg) throws Exception {
        if (tbNode != null) {
            if (!isMyNode()) {
                stop(null);
                tbNode = null;
            } else {
                tbNode.onPartitionChangeMsg(defaultCtx, msg);
            }
        } else if (isMyNode()) {
            start(null);
        }
    }

    public void onRuleToSelfMsg(RuleNodeToSelfMsg msg) throws Exception {
        checkComponentStateActive(msg.getMsg());
        TbMsg tbMsg = msg.getMsg();
        int ruleNodeCount = tbMsg.getAndIncrementRuleNodeCounter();
        int maxRuleNodeExecutionsPerMessage = getTenantProfileConfiguration().getMaxRuleNodeExecsPerMessage();
        if (maxRuleNodeExecutionsPerMessage == 0 || ruleNodeCount < maxRuleNodeExecutionsPerMessage) {
            apiUsageClient.report(tenantId, tbMsg.getCustomerId(), ApiUsageRecordKey.RE_EXEC_COUNT);
            if (ruleNode.isDebugMode()) {
                systemContext.persistDebugInput(tenantId, entityId, msg.getMsg(), "Self");
            }
            try {
                tbNode.onMsg(defaultCtx, msg.getMsg());
            } catch (Exception e) {
                defaultCtx.tellFailure(msg.getMsg(), e);
            }
        } else {
            tbMsg.getCallback().onFailure(new RuleNodeException("Message is processed by more then " + maxRuleNodeExecutionsPerMessage + " rule nodes!", ruleChainName, ruleNode));
        }
    }

    void onRuleChainToRuleNodeMsg(RuleChainToRuleNodeMsg msg) throws Exception {
        if (!isMyNode()) {
            putToQueue(msg.getMsg());
        } else {
            msg.getMsg().getCallback().onProcessingStart(info);
            checkComponentStateActive(msg.getMsg());
            TbMsg tbMsg = msg.getMsg();
            int ruleNodeCount = tbMsg.getAndIncrementRuleNodeCounter();
            int maxRuleNodeExecutionsPerMessage = getTenantProfileConfiguration().getMaxRuleNodeExecsPerMessage();
            if (maxRuleNodeExecutionsPerMessage == 0 || ruleNodeCount < maxRuleNodeExecutionsPerMessage) {
                apiUsageClient.report(tenantId, tbMsg.getCustomerId(), ApiUsageRecordKey.RE_EXEC_COUNT);
                if (ruleNode.isDebugMode()) {
                    systemContext.persistDebugInput(tenantId, entityId, msg.getMsg(), msg.getFromRelationType());
                }
                try {
                    tbNode.onMsg(msg.getCtx(), msg.getMsg());
                } catch (Exception e) {
                    msg.getCtx().tellFailure(msg.getMsg(), e);
                }
            } else {
                tbMsg.getCallback().onFailure(new RuleNodeException("Message is processed by more then " + maxRuleNodeExecutionsPerMessage + " rule nodes!", ruleChainName, ruleNode));
            }
        }
    }

    @Override
    public String getComponentName() {
        return ruleNode.getName();
    }

    private TbNode initComponent(RuleNode ruleNode) throws Exception {
        TbNode tbNode = null;
        if (ruleNode != null) {
            Class<?> componentClazz = Class.forName(ruleNode.getType());
            tbNode = (TbNode) (componentClazz.getDeclaredConstructor().newInstance());
            tbNode.init(defaultCtx, new TbNodeConfiguration(ruleNode.getConfiguration()));
        }
        return tbNode;
    }

    @Override
    protected RuleNodeException getInactiveException() {
        return new RuleNodeException("Rule Node is not active! Failed to initialize.", ruleChainName, ruleNode);
    }

    private boolean isMyNode() {
        return !ruleNode.isSingletonMode()
                || systemContext.getDiscoveryService().isMonolith()
                || defaultCtx.isLocalEntity(ruleNode.getId());
    }

    private void putToQueue(TbMsg source) {
        TbMsg tbMsg = TbMsg.newMsg(source, source.getQueueName(), source.getRuleChainId(), entityId);
        TopicPartitionInfo tpi = systemContext.resolve(ServiceType.TB_RULE_ENGINE, tbMsg.getQueueName(), tenantId, entityId);
        TransportProtos.ToRuleEngineMsg toQueueMsg = TransportProtos.ToRuleEngineMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setTbMsg(TbMsg.toByteString(tbMsg))
                .build();
        systemContext.getClusterService().pushMsgToRuleEngine(tpi, tbMsg.getId(), toQueueMsg, null);
        defaultCtx.ack(source);
    }
}
