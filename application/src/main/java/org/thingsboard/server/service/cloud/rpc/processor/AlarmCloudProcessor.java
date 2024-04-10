/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AlarmCommentUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.service.edge.rpc.constructor.alarm.AlarmMsgConstructor;
import org.thingsboard.server.service.edge.rpc.processor.alarm.BaseAlarmProcessor;

@Component
@Slf4j
public class AlarmCloudProcessor extends BaseAlarmProcessor {

    public ListenableFuture<Void> processAlarmMsgFromCloud(TenantId tenantId, AlarmUpdateMsg alarmUpdateMsg) {
        try {
            cloudSynchronizationManager.getSync().set(true);
            return processAlarmMsg(tenantId, alarmUpdateMsg);
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    public ListenableFuture<Void> processAlarmCommentMsgFromCloud(TenantId tenantId, AlarmCommentUpdateMsg alarmCommentUpdateMsg) {
        try {
            cloudSynchronizationManager.getSync().set(true);
            return processAlarmCommentMsg(tenantId, alarmCommentUpdateMsg);
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    public UplinkMsg convertAlarmEventToUplink(CloudEvent cloudEvent, EdgeVersion edgeVersion) {
        AlarmUpdateMsg alarmUpdateMsg =
                convertAlarmEventToAlarmMsg(cloudEvent.getTenantId(), cloudEvent.getEntityId(), cloudEvent.getAction(), cloudEvent.getEntityBody(), edgeVersion);
        if (alarmUpdateMsg != null) {
            return UplinkMsg.newBuilder()
                    .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                    .addAlarmUpdateMsg(alarmUpdateMsg)
                    .build();
        }
        return null;
    }

    public UplinkMsg convertAlarmCommentEventToUplink(CloudEvent cloudEvent, EdgeVersion edgeVersion) {
        UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
        AlarmComment alarmComment;
        switch (cloudEvent.getAction()) {
            case ADDED_COMMENT:
            case UPDATED_COMMENT:
            case DELETED_COMMENT:
                alarmComment = JacksonUtil.convertValue(cloudEvent.getEntityBody(), AlarmComment.class);
                return UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addAlarmCommentUpdateMsg(((AlarmMsgConstructor) alarmMsgConstructorFactory
                                .getMsgConstructorByEdgeVersion(edgeVersion)).constructAlarmCommentUpdatedMsg(msgType, alarmComment))
                        .build();
            default:
                return null;
        }
    }

    @Override
    protected EntityId getAlarmOriginatorFromMsg(TenantId tenantId, AlarmUpdateMsg alarmUpdateMsg) {
        Alarm alarm = JacksonUtil.fromString(alarmUpdateMsg.getEntity(), Alarm.class, true);
        return alarm != null ? alarm.getOriginator() : null;
    }

    @Override
    protected Alarm constructAlarmFromUpdateMsg(TenantId tenantId, AlarmId alarmId, EntityId originatorId, AlarmUpdateMsg alarmUpdateMsg) {
        return JacksonUtil.fromString(alarmUpdateMsg.getEntity(), Alarm.class, true);
    }

}
