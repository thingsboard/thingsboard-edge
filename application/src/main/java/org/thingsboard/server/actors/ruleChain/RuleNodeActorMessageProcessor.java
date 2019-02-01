/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.shared.ComponentMsgProcessor;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.cluster.ClusterEventMsg;
import org.thingsboard.server.dao.rule.RuleChainService;

/**
 * @author Andrew Shvayka
 */
public class RuleNodeActorMessageProcessor extends ComponentMsgProcessor<RuleNodeId> {

    private final ActorRef parent;
    private final ActorRef self;
    private final RuleChainService service;
    private RuleNode ruleNode;
    private TbNode tbNode;
    private TbContext defaultCtx;

    RuleNodeActorMessageProcessor(TenantId tenantId, RuleChainId ruleChainId, RuleNodeId ruleNodeId, ActorSystemContext systemContext
            , ActorRef parent, ActorRef self) {
        super(systemContext, tenantId, ruleNodeId);
        this.parent = parent;
        this.self = self;
        this.service = systemContext.getRuleChainService();
        this.ruleNode = systemContext.getRuleChainService().findRuleNodeById(tenantId, entityId);
        this.defaultCtx = new DefaultTbContext(systemContext, new RuleNodeCtx(tenantId, parent, self, ruleNode));
    }

    @Override
    public void start(ActorContext context) throws Exception {
        tbNode = initComponent(ruleNode);
        state = ComponentLifecycleState.ACTIVE;
    }

    @Override
    public void onUpdate(ActorContext context) throws Exception {
        RuleNode newRuleNode = systemContext.getRuleChainService().findRuleNodeById(tenantId, entityId);
        boolean restartRequired = !(ruleNode.getType().equals(newRuleNode.getType())
                && ruleNode.getConfiguration().equals(newRuleNode.getConfiguration()));
        this.ruleNode = newRuleNode;
        this.defaultCtx.updateSelf(newRuleNode);
        if (restartRequired) {
            if (tbNode != null) {
                tbNode.destroy();
            }
            start(context);
        }
    }

    @Override
    public void stop(ActorContext context) {
        if (tbNode != null) {
            tbNode.destroy();
        }
        context.stop(self);
    }

    @Override
    public void onClusterEventMsg(ClusterEventMsg msg) {
        if (tbNode != null) {
            tbNode.onClusterEventMsg(defaultCtx, msg);
        }
    }

    public void onRuleToSelfMsg(RuleNodeToSelfMsg msg) throws Exception {
        checkActive();
        if (ruleNode.isDebugMode()) {
            systemContext.persistDebugInput(tenantId, entityId, msg.getMsg(), "Self");
        }
        try {
            tbNode.onMsg(defaultCtx, msg.getMsg());
        } catch (Exception e) {
            defaultCtx.tellFailure(msg.getMsg(), e);
        }
    }

    void onRuleChainToRuleNodeMsg(RuleChainToRuleNodeMsg msg) throws Exception {
        checkActive();
        if (ruleNode.isDebugMode()) {
            systemContext.persistDebugInput(tenantId, entityId, msg.getMsg(), msg.getFromRelationType());
        }
        try {
            tbNode.onMsg(msg.getCtx(), msg.getMsg());
        } catch (Exception e) {
            msg.getCtx().tellFailure(msg.getMsg(), e);
        }
    }

    @Override
    public String getComponentName() {
        return ruleNode.getName();
    }

    private TbNode initComponent(RuleNode ruleNode) throws Exception {
        Class<?> componentClazz = Class.forName(ruleNode.getType());
        TbNode tbNode = (TbNode) (componentClazz.newInstance());
        tbNode.init(defaultCtx, new TbNodeConfiguration(ruleNode.getConfiguration()));
        return tbNode;
    }

}
