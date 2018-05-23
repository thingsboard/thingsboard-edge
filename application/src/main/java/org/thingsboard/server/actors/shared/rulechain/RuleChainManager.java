/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.actors.shared.rulechain;

import akka.actor.ActorRef;
import akka.japi.Creator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.ruleChain.RuleChainActor;
import org.thingsboard.server.actors.shared.EntityActorsManager;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.rule.RuleChainService;

/**
 * Created by ashvayka on 15.03.18.
 */
@Slf4j
public abstract class RuleChainManager extends EntityActorsManager<RuleChainId, RuleChainActor, RuleChain> {

    protected final RuleChainService service;
    @Getter
    protected RuleChain rootChain;
    @Getter
    protected ActorRef rootChainActor;

    public RuleChainManager(ActorSystemContext systemContext) {
        super(systemContext);
        this.service = systemContext.getRuleChainService();
    }

    @Override
    public Creator<RuleChainActor> creator(RuleChainId entityId) {
        return new RuleChainActor.ActorCreator(systemContext, getTenantId(), entityId);
    }

    @Override
    public void visit(RuleChain entity, ActorRef actorRef) {
        if (entity.isRoot()) {
            rootChain = entity;
            rootChainActor = actorRef;
        }
    }

}
