/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
