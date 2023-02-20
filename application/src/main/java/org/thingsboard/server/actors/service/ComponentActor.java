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
package org.thingsboard.server.actors.service;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorException;
import org.thingsboard.server.actors.TbRuleNodeUpdateException;
import org.thingsboard.server.actors.shared.ComponentMsgProcessor;
import org.thingsboard.server.actors.stats.StatsPersistMsg;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public abstract class ComponentActor<T extends EntityId, P extends ComponentMsgProcessor<T>> extends ContextAwareActor {

    private long lastPersistedErrorTs = 0L;
    protected final TenantId tenantId;
    protected final T id;
    protected P processor;
    private long messagesProcessed;
    private long errorsOccurred;

    public ComponentActor(ActorSystemContext systemContext, TenantId tenantId, T id) {
        super(systemContext);
        this.tenantId = tenantId;
        this.id = id;
    }

    abstract protected P createProcessor(TbActorCtx ctx);

    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        super.init(ctx);
        this.processor = createProcessor(ctx);
        initProcessor(ctx);
    }

    protected void initProcessor(TbActorCtx ctx) throws TbActorException {
        try {
            log.debug("[{}][{}][{}] Starting processor.", tenantId, id, id.getEntityType());
            processor.start(ctx);
            logLifecycleEvent(ComponentLifecycleEvent.STARTED);
            if (systemContext.isStatisticsEnabled()) {
                scheduleStatsPersistTick();
            }
        } catch (Exception e) {
            log.debug("[{}][{}] Failed to start {} processor.", tenantId, id, id.getEntityType(), e);
            logAndPersist("OnStart", e, true);
            logLifecycleEvent(ComponentLifecycleEvent.STARTED, e);
            throw new TbActorException("Failed to init actor", e);
        }
    }

    private void scheduleStatsPersistTick() {
        try {
            processor.scheduleStatsPersistTick(ctx, systemContext.getStatisticsPersistFrequency());
        } catch (Exception e) {
            log.error("[{}][{}] Failed to schedule statistics store message. No statistics is going to be stored: {}", tenantId, id, e.getMessage());
            logAndPersist("onScheduleStatsPersistMsg", e);
        }
    }

    @Override
    public void destroy() {
        try {
            log.debug("[{}][{}][{}] Stopping processor.", tenantId, id, id.getEntityType());
            if (processor != null) {
                processor.stop(ctx);
            }
            logLifecycleEvent(ComponentLifecycleEvent.STOPPED);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to stop {} processor: {}", tenantId, id, id.getEntityType(), e.getMessage());
            logAndPersist("OnStop", e, true);
            logLifecycleEvent(ComponentLifecycleEvent.STOPPED, e);
        }
    }

    protected void onComponentLifecycleMsg(ComponentLifecycleMsg msg) {
        log.debug("[{}][{}][{}] onComponentLifecycleMsg: [{}]", tenantId, id, id.getEntityType(), msg.getEvent());
        try {
            switch (msg.getEvent()) {
                case CREATED:
                    processor.onCreated(ctx);
                    break;
                case UPDATED:
                    processor.onUpdate(ctx);
                    break;
                case SUSPENDED:
                    processor.onSuspend(ctx);
                    break;
                case DELETED:
                    processor.onStop(ctx);
                    ctx.stop(ctx.getSelf());
                    break;
                default:
                    break;
            }
            logLifecycleEvent(msg.getEvent());
        } catch (Exception e) {
            logAndPersist("onLifecycleMsg", e, true);
            logLifecycleEvent(msg.getEvent(), e);
            if (e instanceof TbRuleNodeUpdateException) {
                throw (TbRuleNodeUpdateException) e;
            }
        }
    }

    protected void onClusterEventMsg(PartitionChangeMsg msg) {
        try {
            processor.onPartitionChangeMsg(msg);
        } catch (Exception e) {
            logAndPersist("onClusterEventMsg", e);
        }
    }

    protected void onStatsPersistTick(EntityId entityId) {
        try {
            systemContext.getStatsActor().tell(new StatsPersistMsg(messagesProcessed, errorsOccurred, tenantId, entityId));
            resetStatsCounters();
        } catch (Exception e) {
            logAndPersist("onStatsPersistTick", e);
        }
    }

    private void resetStatsCounters() {
        messagesProcessed = 0;
        errorsOccurred = 0;
    }

    protected void increaseMessagesProcessedCount() {
        messagesProcessed++;
    }

    protected void logAndPersist(String method, Exception e) {
        logAndPersist(method, e, false);
    }

    private void logAndPersist(String method, Exception e, boolean critical) {
        errorsOccurred++;
        String componentName = processor != null ? processor.getComponentName() : "Unknown";
        if (critical) {
            log.debug("[{}][{}][{}] Failed to process method: {}", id, tenantId, componentName, method);
            log.debug("Critical Error: ", e);
        } else {
            log.trace("[{}][{}][{}] Failed to process method: {}", id, tenantId, componentName, method);
            log.trace("Debug Error: ", e);
        }
        long ts = System.currentTimeMillis();
        if (ts - lastPersistedErrorTs > getErrorPersistFrequency()) {
            systemContext.persistError(tenantId, id, method, e);
            lastPersistedErrorTs = ts;
        }
    }

    private void logLifecycleEvent(ComponentLifecycleEvent event) {
        logLifecycleEvent(event, null);
    }

    protected void logLifecycleEvent(ComponentLifecycleEvent event, Exception e) {
        systemContext.persistLifecycleEvent(tenantId, id, event, e);
    }

    protected abstract long getErrorPersistFrequency();

}
