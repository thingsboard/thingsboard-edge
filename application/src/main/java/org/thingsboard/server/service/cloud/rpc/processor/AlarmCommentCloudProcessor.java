// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AlarmCommentUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.alarm.BaseAlarmProcessor;

@Slf4j
@Component
@TbCoreComponent
public class AlarmCommentCloudProcessor extends BaseAlarmProcessor {

    public ListenableFuture<Void> processAlarmCommentMsgFromCloud(TenantId tenantId, AlarmCommentUpdateMsg alarmCommentUpdateMsg) {
        try {
            cloudSynchronizationManager.getSync().set(true);
            return processAlarmCommentMsg(tenantId, alarmCommentUpdateMsg);
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    @Override
    public UplinkMsg convertCloudEventToUplink(CloudEvent cloudEvent) {
        UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
        AlarmComment alarmComment;
        return switch (cloudEvent.getAction()) {
            case ADDED_COMMENT, UPDATED_COMMENT, DELETED_COMMENT -> {
                alarmComment = JacksonUtil.convertValue(cloudEvent.getEntityBody(), AlarmComment.class);
                yield UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addAlarmCommentUpdateMsg(EdgeMsgConstructorUtils.constructAlarmCommentUpdatedMsg(msgType, alarmComment))
                        .build();
            }
            default -> null;
        };
    }

    @Override
    public CloudEventType getCloudEventType() {
        return CloudEventType.ALARM_COMMENT;
    }

}
