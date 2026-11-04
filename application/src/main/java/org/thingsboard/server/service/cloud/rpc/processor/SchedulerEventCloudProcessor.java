// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.gen.edge.v1.SchedulerEventUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.scheduler.BaseSchedulerEventProcessor;

import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class SchedulerEventCloudProcessor extends BaseSchedulerEventProcessor {

    public ListenableFuture<Void> processScheduleEventFromCloud(TenantId tenantId, SchedulerEventUpdateMsg schedulerEventUpdateMsg) {
        SchedulerEventId schedulerEventId = new SchedulerEventId(new UUID(schedulerEventUpdateMsg.getIdMSB(), schedulerEventUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);

            switch (schedulerEventUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    Boolean created = super.saveOrUpdateSchedulerEvent(tenantId, schedulerEventId, schedulerEventUpdateMsg);
                    if (created) {
                        pushSchedulerEventCreatedEventToRuleEngine(tenantId, schedulerEventId);
                        return Futures.immediateFuture(null);
                    }

                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    SchedulerEvent schedulerEventToDelete = edgeCtx.getSchedulerEventService().findSchedulerEventById(tenantId, schedulerEventId);
                    if (schedulerEventToDelete != null) {
                        edgeCtx.getSchedulerEventService().deleteSchedulerEvent(tenantId, schedulerEventId);
                        pushSchedulerEventDeletedEventToRuleEngine(tenantId, schedulerEventToDelete);
                    }
                    break;
                case UNRECOGNIZED:
                    return handleUnsupportedMsgType(schedulerEventUpdateMsg.getMsgType());
            }
        } catch (Exception e) {
            String errMsg = String.format("Can't process SchedulerEventUpdateMsg [%s]", schedulerEventUpdateMsg);
            log.error(errMsg, e);
            return Futures.immediateFailedFuture(new RuntimeException(errMsg, e));
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
        return Futures.immediateFuture(null);
    }

    private void pushSchedulerEventCreatedEventToRuleEngine(TenantId tenantId, SchedulerEventId schedulerEventId) {
        SchedulerEvent schedulerEvent = edgeCtx.getSchedulerEventService().findSchedulerEventById(tenantId, schedulerEventId);
        pushSchedulerEventToRuleEngine(tenantId, schedulerEvent, TbMsgType.ENTITY_CREATED);
    }

    private void pushSchedulerEventDeletedEventToRuleEngine(TenantId tenantId, SchedulerEvent schedulerEvent) {
        pushSchedulerEventToRuleEngine(tenantId, schedulerEvent, TbMsgType.ENTITY_DELETED);
    }

    private void pushSchedulerEventToRuleEngine(TenantId tenantId, SchedulerEvent schedulerEvent, TbMsgType msgType) {
        try {
            String schedulerEventAsString = JacksonUtil.toString(schedulerEvent);
            pushEntityEventToRuleEngine(tenantId, schedulerEvent.getId(), null, msgType, schedulerEventAsString, new TbMsgMetaData());
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push scheduler event action to rule engine: {}", tenantId, schedulerEvent.getId(), msgType.name(), e);
        }
    }

    @Override
    protected boolean isEnabledDuringCreation() {
        return true;
    }

    @Override
    protected void setCustomerId(TenantId tenantId, CustomerId customerId, SchedulerEvent schedulerEvent) {
        if (isCustomerNotExists(tenantId, schedulerEvent.getCustomerId())) {
            schedulerEvent.setCustomerId(null);
        }
    }

    @Override
    public UplinkMsg convertCloudEventToUplink(CloudEvent cloudEvent) {
        SchedulerEventId schedulerEventId = new SchedulerEventId(cloudEvent.getEntityId());
        switch (cloudEvent.getAction()) {
            case ADDED, UPDATED -> {
                SchedulerEvent schedulerEvent = edgeCtx.getSchedulerEventService().findSchedulerEventById(cloudEvent.getTenantId(), schedulerEventId);
                if (schedulerEvent != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    SchedulerEventUpdateMsg schedulerEventUpdateMsg = EdgeMsgConstructorUtils.constructSchedulerEventUpdatedMsg(msgType, schedulerEvent);
                    return UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addSchedulerEventUpdateMsg(schedulerEventUpdateMsg).build();
                } else {
                    log.info("Skipping event as scheduler event was not found [{}]", cloudEvent);
                }
            }
            case DELETED -> {
                SchedulerEventUpdateMsg schedulerEventUpdateMsg = EdgeMsgConstructorUtils.constructSchedulerEventDeleteMsg(schedulerEventId);
                return UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addSchedulerEventUpdateMsg(schedulerEventUpdateMsg).build();
            }
        }
        return null;
    }

    @Override
    public CloudEventType getCloudEventType() {
        return CloudEventType.SCHEDULER_EVENT;
    }
}
