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
package org.thingsboard.server.actors.shared.plugin;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Props;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.plugin.PluginActor;
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageDataIterable.FetchFunction;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.dao.plugin.PluginService;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public abstract class PluginManager {

    protected final ActorSystemContext systemContext;
    protected final PluginService pluginService;
    protected final Map<PluginId, ActorRef> pluginActors;

    public PluginManager(ActorSystemContext systemContext) {
        this.systemContext = systemContext;
        this.pluginService = systemContext.getPluginService();
        this.pluginActors = new HashMap<>();
    }

    public void init(ActorContext context) {
        PageDataIterable<PluginMetaData> pluginIterator = new PageDataIterable<>(getFetchPluginsFunction(),
                ContextAwareActor.ENTITY_PACK_LIMIT);
        for (PluginMetaData plugin : pluginIterator) {
            log.debug("[{}] Creating plugin actor", plugin.getId());
            getOrCreatePluginActor(context, plugin.getId());
            log.debug("Plugin actor created.");
        }
    }

    abstract FetchFunction<PluginMetaData> getFetchPluginsFunction();

    abstract TenantId getTenantId();

    abstract String getDispatcherName();

    public ActorRef getOrCreatePluginActor(ActorContext context, PluginId pluginId) {
        return pluginActors.computeIfAbsent(pluginId, pId ->
                context.actorOf(Props.create(new PluginActor.ActorCreator(systemContext, getTenantId(), pId))
                        .withDispatcher(getDispatcherName()), pId.toString()));
    }

    public void broadcast(Object msg) {
        pluginActors.values().forEach(actorRef -> actorRef.tell(msg, ActorRef.noSender()));
    }

    public void remove(PluginId id) {
        pluginActors.remove(id);
    }
}
