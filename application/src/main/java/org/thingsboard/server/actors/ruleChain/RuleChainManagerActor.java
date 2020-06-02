/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
import akka.actor.Props;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.Getter;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.rule.RuleChainService;

import java.util.function.Function;

/**
 * Created by ashvayka on 15.03.18.
 */
public abstract class RuleChainManagerActor extends ContextAwareActor {

    protected final TenantId tenantId;
    private final RuleChainService ruleChainService;
    private final BiMap<RuleChainId, ActorRef> actors;
    @Getter
    protected RuleChain rootChain;
    @Getter
    protected ActorRef rootChainActor;

    public RuleChainManagerActor(ActorSystemContext systemContext, TenantId tenantId) {
        super(systemContext);
        this.tenantId = tenantId;
        this.actors = HashBiMap.create();
        this.ruleChainService = systemContext.getRuleChainService();
    }

    protected void initRuleChains() {
        for (RuleChain ruleChain : new PageDataIterable<>(link -> ruleChainService.findTenantRuleChains(tenantId, link), ContextAwareActor.ENTITY_PACK_LIMIT)) {
            RuleChainId ruleChainId = ruleChain.getId();
            log.debug("[{}|{}] Creating rule chain actor", ruleChainId.getEntityType(), ruleChain.getId());
            //TODO: remove this cast making UUIDBased subclass of EntityId an interface and vice versa.
            ActorRef actorRef = getOrCreateActor(this.context(), ruleChainId, id -> ruleChain);
            visit(ruleChain, actorRef);
            log.debug("[{}|{}] Rule Chain actor created.", ruleChainId.getEntityType(), ruleChainId.getId());
        }
    }

    protected void visit(RuleChain entity, ActorRef actorRef) {
        if (entity != null && entity.isRoot()) {
            rootChain = entity;
            rootChainActor = actorRef;
        }
    }

    public ActorRef getOrCreateActor(akka.actor.ActorContext context, RuleChainId ruleChainId) {
        return getOrCreateActor(context, ruleChainId, eId -> ruleChainService.findRuleChainById(TenantId.SYS_TENANT_ID, eId));
    }

    public ActorRef getOrCreateActor(akka.actor.ActorContext context, RuleChainId ruleChainId, Function<RuleChainId, RuleChain> provider) {
        return actors.computeIfAbsent(ruleChainId, eId -> {
            RuleChain ruleChain = provider.apply(eId);
            return context.actorOf(Props.create(new RuleChainActor.ActorCreator(systemContext, tenantId, ruleChain))
                    .withDispatcher(DefaultActorService.TENANT_RULE_DISPATCHER_NAME), eId.toString());
        });
    }

    protected ActorRef getEntityActorRef(EntityId entityId) {
        ActorRef target = null;
        if (entityId.getEntityType() == EntityType.RULE_CHAIN) {
            target = getOrCreateActor(this.context(), (RuleChainId) entityId);
        }
        return target;
    }

    protected void broadcast(Object msg) {
        actors.values().forEach(actorRef -> actorRef.tell(msg, ActorRef.noSender()));
    }

    public ActorRef get(RuleChainId id) {
        return actors.get(id);
    }

}
