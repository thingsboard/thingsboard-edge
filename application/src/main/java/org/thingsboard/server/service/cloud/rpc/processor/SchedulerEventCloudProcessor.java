/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.gen.edge.v1.SchedulerEventUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;
import org.thingsboard.server.service.scheduler.SchedulerService;

import java.util.UUID;

@Component
@Slf4j
public class SchedulerEventCloudProcessor extends BaseEdgeProcessor {

    @Autowired
    private SchedulerEventService schedulerEventService;

    @Autowired
    private SchedulerService schedulerService;

    public ListenableFuture<Void> processScheduleEventFromCloud(TenantId tenantId, SchedulerEventUpdateMsg schedulerEventUpdateMsg) {
        try {
            SchedulerEventId schedulerEventId = new SchedulerEventId(new UUID(schedulerEventUpdateMsg.getIdMSB(), schedulerEventUpdateMsg.getIdLSB()));
            switch (schedulerEventUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    SchedulerEvent schedulerEvent = schedulerEventService.findSchedulerEventById(tenantId, schedulerEventId);
                    boolean created = false;
                    if (schedulerEvent == null) {
                        created = true;
                        schedulerEvent = new SchedulerEvent();
                        schedulerEvent.setId(schedulerEventId);
                        schedulerEvent.setCreatedTime(Uuids.unixTimestamp(schedulerEventId.getId()));
                        schedulerEvent.setTenantId(tenantId);
                    }
                    schedulerEvent.setName(schedulerEventUpdateMsg.getName());
                    schedulerEvent.setType(schedulerEventUpdateMsg.getType());
                    schedulerEvent.setSchedule(JacksonUtil.toJsonNode(schedulerEventUpdateMsg.getSchedule()));
                    schedulerEvent.setConfiguration(JacksonUtil.toJsonNode(schedulerEventUpdateMsg.getConfiguration()));
                    safeSetCustomerId(schedulerEventUpdateMsg, schedulerEvent);
                    schedulerEvent.setAdditionalInfo(schedulerEventUpdateMsg.hasAdditionalInfo()
                            ? JacksonUtil.toJsonNode(schedulerEventUpdateMsg.getAdditionalInfo()) : null);
                    EntityId originatorId = null;
                    if (schedulerEventUpdateMsg.hasOriginatorType()
                            && schedulerEventUpdateMsg.hasOriginatorIdMSB()
                            && schedulerEventUpdateMsg.hasOriginatorIdLSB()) {
                        originatorId = constructEntityId(schedulerEventUpdateMsg.getOriginatorType(),
                                schedulerEventUpdateMsg.getOriginatorIdMSB(), schedulerEventUpdateMsg.getOriginatorIdLSB());
                    }
                    schedulerEvent.setOriginatorId(originatorId);
                    schedulerEventService.saveSchedulerEvent(schedulerEvent);

                    if (created) {
                        schedulerService.onSchedulerEventAdded(schedulerEvent);
                    } else {
                        schedulerService.onSchedulerEventUpdated(schedulerEvent);
                    }

                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    SchedulerEventInfo schedulerEventById = schedulerEventService.findSchedulerEventInfoById(tenantId, schedulerEventId);
                    if (schedulerEventById != null) {
                        schedulerEventService.deleteSchedulerEvent(tenantId, schedulerEventId);

                        schedulerService.onSchedulerEventDeleted(schedulerEventById);
                    }
                    break;
                case UNRECOGNIZED:
                    return handleUnsupportedMsgType(schedulerEventUpdateMsg.getMsgType());
            }
        } catch (Exception e) {
            String errMsg = String.format("Can't process SchedulerEventUpdateMsg [%s]", schedulerEventUpdateMsg);
            log.error(errMsg, e);
            return Futures.immediateFailedFuture(new RuntimeException(errMsg, e));
        }
        return Futures.immediateFuture(null);
    }

    private void safeSetCustomerId(SchedulerEventUpdateMsg schedulerEventUpdateMsg, SchedulerEvent schedulerEvent) {
        CustomerId customerId = safeGetCustomerId(schedulerEventUpdateMsg.getCustomerIdMSB(),
                schedulerEventUpdateMsg.getCustomerIdLSB());
        schedulerEvent.setCustomerId(customerId);
    }

}
