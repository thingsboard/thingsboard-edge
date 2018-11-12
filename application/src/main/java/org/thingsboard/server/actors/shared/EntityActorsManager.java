/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.actors.shared;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.common.data.SearchTextBased;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.page.PageDataIterable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ashvayka on 15.03.18.
 */
@Slf4j
public abstract class EntityActorsManager<T extends EntityId, A extends UntypedActor, M extends SearchTextBased<? extends UUIDBased>> {

    protected final ActorSystemContext systemContext;
    protected final BiMap<T, ActorRef> actors;

    public EntityActorsManager(ActorSystemContext systemContext) {
        this.systemContext = systemContext;
        this.actors = HashBiMap.create();
    }

    protected abstract TenantId getTenantId();

    protected abstract String getDispatcherName();

    protected abstract Creator<A> creator(T entityId);

    protected abstract PageDataIterable.FetchFunction<M> getFetchEntitiesFunction();

    public void init(ActorContext context) {
        for (M entity : new PageDataIterable<>(getFetchEntitiesFunction(), ContextAwareActor.ENTITY_PACK_LIMIT)) {
            T entityId = (T) entity.getId();
            log.debug("[{}|{}] Creating entity actor", entityId.getEntityType(), entityId.getId());
            //TODO: remove this cast making UUIDBased subclass of EntityId an interface and vice versa.
            ActorRef actorRef = getOrCreateActor(context, entityId);
            visit(entity, actorRef);
            log.debug("[{}|{}] Entity actor created.", entityId.getEntityType(), entityId.getId());
        }
    }

    public void visit(M entity, ActorRef actorRef) {
    }

    public ActorRef getOrCreateActor(ActorContext context, T entityId) {
        return actors.computeIfAbsent(entityId, eId ->
                context.actorOf(Props.create(creator(eId))
                        .withDispatcher(getDispatcherName()), eId.toString()));
    }

    public void broadcast(Object msg) {
        actors.values().forEach(actorRef -> actorRef.tell(msg, ActorRef.noSender()));
    }

    public void remove(T id) {
        actors.remove(id);
    }

}
