/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
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
package org.thingsboard.server.actors.shared.rule;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Props;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.rule.RuleActor;
import org.thingsboard.server.actors.rule.RuleActorChain;
import org.thingsboard.server.actors.rule.RuleActorMetaData;
import org.thingsboard.server.actors.rule.SimpleRuleActorChain;
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageDataIterable.FetchFunction;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.data.rule.RuleMetaData;
import org.thingsboard.server.dao.rule.RuleService;

import java.util.*;

@Slf4j
public abstract class RuleManager {

    protected final ActorSystemContext systemContext;
    protected final RuleService ruleService;
    protected final Map<RuleId, ActorRef> ruleActors;
    protected final TenantId tenantId;

    private Map<RuleMetaData, RuleActorMetaData> ruleMap;
    private RuleActorChain ruleChain;

    public RuleManager(ActorSystemContext systemContext, TenantId tenantId) {
        this.systemContext = systemContext;
        this.ruleService = systemContext.getRuleService();
        this.ruleActors = new HashMap<>();
        this.tenantId = tenantId;
    }

    public void init(ActorContext context) {
        doInit(context);
    }

    private void doInit(ActorContext context) {
        PageDataIterable<RuleMetaData> ruleIterator = new PageDataIterable<>(getFetchRulesFunction(),
                ContextAwareActor.ENTITY_PACK_LIMIT);
        ruleMap = new HashMap<>();

        for (RuleMetaData rule : ruleIterator) {
            log.debug("[{}] Creating rule actor {}", rule.getId(), rule);
            ActorRef ref = getOrCreateRuleActor(context, rule.getId());
            ruleMap.put(rule, RuleActorMetaData.systemRule(rule.getId(), rule.getWeight(), ref));
            log.debug("[{}] Rule actor created.", rule.getId());
        }

        refreshRuleChain();
    }

    public Optional<ActorRef> update(ActorContext context, RuleId ruleId, ComponentLifecycleEvent event) {
        if (ruleMap == null) {
            doInit(context);
        }
        RuleMetaData rule;
        if (event != ComponentLifecycleEvent.DELETED) {
            rule = systemContext.getRuleService().findRuleById(ruleId);
        } else {
            rule = ruleMap.keySet().stream()
                    .filter(r -> r.getId().equals(ruleId))
                    .peek(r -> r.setState(ComponentLifecycleState.SUSPENDED))
                    .findFirst()
                    .orElse(null);
            if (rule != null) {
                ruleMap.remove(rule);
                ruleActors.remove(ruleId);
            }
        }
        if (rule != null) {
            RuleActorMetaData actorMd = ruleMap.get(rule);
            if (actorMd == null) {
                ActorRef ref = getOrCreateRuleActor(context, rule.getId());
                actorMd = RuleActorMetaData.systemRule(rule.getId(), rule.getWeight(), ref);
                ruleMap.put(rule, actorMd);
            }
            refreshRuleChain();
            return Optional.of(actorMd.getActorRef());
        } else {
            log.warn("[{}] Can't process unknown rule!", ruleId);
            return Optional.empty();
        }
    }

    abstract FetchFunction<RuleMetaData> getFetchRulesFunction();

    abstract String getDispatcherName();

    public ActorRef getOrCreateRuleActor(ActorContext context, RuleId ruleId) {
        return ruleActors.computeIfAbsent(ruleId, rId ->
                context.actorOf(Props.create(new RuleActor.ActorCreator(systemContext, tenantId, rId))
                        .withDispatcher(getDispatcherName()), rId.toString()));
    }

    public RuleActorChain getRuleChain(ActorContext context) {
        if (ruleChain == null) {
            doInit(context);
        }
        return ruleChain;
    }

    private void refreshRuleChain() {
        Set<RuleActorMetaData> activeRuleSet = new HashSet<>();
        for (Map.Entry<RuleMetaData, RuleActorMetaData> rule : ruleMap.entrySet()) {
            if (rule.getKey().getState() == ComponentLifecycleState.ACTIVE) {
                activeRuleSet.add(rule.getValue());
            }
        }
        ruleChain = new SimpleRuleActorChain(activeRuleSet);
    }
}
