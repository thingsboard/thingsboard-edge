/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.entitiy.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.scheduler.SchedulerService;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTbSchedulerService extends AbstractTbEntityService implements TbSchedulerService {

    private final SchedulerService schedulerService;
    private final  SchedulerEventService schedulerEventService;

    @Override
    public SchedulerEvent save(SchedulerEvent schedulerEvent, User user) throws ThingsboardException {
        try {
            SchedulerEvent savedSchedulerEvent = checkNotNull(schedulerEventService.saveSchedulerEvent(schedulerEvent));
            logEntityActionService.logEntityAction(user.getTenantId(), savedSchedulerEvent.getId(), savedSchedulerEvent,
                    savedSchedulerEvent.getCustomerId(),
                    schedulerEvent.getId() == null ? ActionType.ADDED : ActionType.UPDATED, user);
            if (schedulerEvent.getId() == null) {
                schedulerService.onSchedulerEventAdded(savedSchedulerEvent);
            } else {
                schedulerService.onSchedulerEventUpdated(savedSchedulerEvent);
            }
            return savedSchedulerEvent;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(user.getTenantId(), emptyId(EntityType.SCHEDULER_EVENT), schedulerEvent,
                    schedulerEvent.getId() == null ? ActionType.ADDED : ActionType.UPDATED, user, e);
            throw e;
        }
    }

    @Override
    public void delete(SchedulerEvent schedulerEvent, User user) throws ThingsboardException {
        ActionType actionType = ActionType.DELETED;
        SchedulerEventId schedulerEventId = schedulerEvent.getId();
        try {
            schedulerEventService.deleteSchedulerEvent(user.getTenantId(), schedulerEventId);
            logEntityActionService.logEntityAction(user.getTenantId(), schedulerEventId, schedulerEvent,
                    schedulerEvent.getCustomerId(), actionType, user, schedulerEventId.getId());
            schedulerService.onSchedulerEventDeleted(schedulerEvent);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(user.getTenantId(), emptyId(EntityType.SCHEDULER_EVENT),
                    actionType, user, e, schedulerEventId.getId());
            throw e;
        }
    }

    @Override
    public SchedulerEventInfo assignToEdge(SchedulerEventId schedulerEventId, Edge edge, User user) throws ThingsboardException {
        try {
            SchedulerEventInfo savedSchedulerEvent = checkNotNull(schedulerEventService.assignSchedulerEventToEdge(user.getTenantId(), schedulerEventId, edge.getId()));
            logEntityActionService.logEntityAction(user.getTenantId(), schedulerEventId, savedSchedulerEvent,
                    ActionType.ASSIGNED_TO_EDGE, user, schedulerEventId.getId(), savedSchedulerEvent.getName(), edge.getId().getId(), edge.getName());
            return savedSchedulerEvent;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(user.getTenantId(), emptyId(EntityType.SCHEDULER_EVENT),
                    ActionType.ASSIGNED_TO_EDGE, user, e, schedulerEventId.getId(), edge.getId().getId());

            throw e;
        }
    }

    @Override
    public SchedulerEventInfo unassignFromEdge(SchedulerEventId schedulerEventId, Edge edge, User user) throws ThingsboardException {
        try {
            SchedulerEventInfo savedSchedulerEvent = checkNotNull(schedulerEventService.unassignSchedulerEventFromEdge(user.getTenantId(), schedulerEventId, edge.getId()));
            logEntityActionService.logEntityAction(user.getTenantId(), schedulerEventId, savedSchedulerEvent, ActionType.UNASSIGNED_FROM_EDGE,
                    user, schedulerEventId.getId(), savedSchedulerEvent.getName(), edge.getId().getId(), edge.getName());
            return savedSchedulerEvent;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(user.getTenantId(), emptyId(EntityType.SCHEDULER_EVENT),
                    ActionType.UNASSIGNED_FROM_EDGE, user, e, schedulerEventId.getId());
            throw e;
        }
    }
}
