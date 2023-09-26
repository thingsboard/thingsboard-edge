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
package org.thingsboard.server.actors.shared;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.stats.StatsPersistTick;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileConfiguration;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.RuleNodeException;

@Slf4j
public abstract class ComponentMsgProcessor<T extends EntityId> extends AbstractContextAwareMsgProcessor {

    protected final TenantId tenantId;
    protected final T entityId;
    protected ComponentLifecycleState state;

    protected ComponentMsgProcessor(ActorSystemContext systemContext, TenantId tenantId, T id) {
        super(systemContext);
        this.tenantId = tenantId;
        this.entityId = id;
    }

    protected TenantProfileConfiguration getTenantProfileConfiguration() {
        return systemContext.getTenantProfileCache().get(tenantId).getProfileData().getConfiguration();
    }

    public abstract String getComponentName();

    public abstract void start(TbActorCtx context) throws Exception;

    public abstract void stop(TbActorCtx context) throws Exception;

    public abstract void onPartitionChangeMsg(PartitionChangeMsg msg) throws Exception;

    public void onCreated(TbActorCtx context) throws Exception {
        start(context);
    }

    public void onUpdate(TbActorCtx context) throws Exception {
        restart(context);
    }

    public void onActivate(TbActorCtx context) throws Exception {
        restart(context);
    }

    public void onSuspend(TbActorCtx context) throws Exception {
        stop(context);
    }

    public void onStop(TbActorCtx context) throws Exception {
        stop(context);
    }

    private void restart(TbActorCtx context) throws Exception {
        stop(context);
        start(context);
    }

    public void scheduleStatsPersistTick(TbActorCtx context, long statsPersistFrequency) {
        schedulePeriodicMsgWithDelay(context, new StatsPersistTick(), statsPersistFrequency, statsPersistFrequency);
    }

    protected boolean checkMsgValid(TbMsg tbMsg) {
        var valid = tbMsg.isValid();
        if (!valid) {
            if (log.isTraceEnabled()) {
                log.trace("Skip processing of message: {} because it is no longer valid!", tbMsg);
            }
        }
        return valid;
    }

    protected void checkComponentStateActive(TbMsg tbMsg) throws RuleNodeException {
        if (state != ComponentLifecycleState.ACTIVE) {
            log.debug("Component is not active. Current state [{}] for processor [{}][{}] tenant [{}]", state, entityId.getEntityType(), entityId, tenantId);
            RuleNodeException ruleNodeException = getInactiveException();
            if (tbMsg != null) {
                tbMsg.getCallback().onFailure(ruleNodeException);
            }
            throw ruleNodeException;
        }
    }

    abstract protected RuleNodeException getInactiveException();

}
