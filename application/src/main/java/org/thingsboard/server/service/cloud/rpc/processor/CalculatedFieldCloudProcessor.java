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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.gen.edge.v1.CalculatedFieldRequestMsg;
import org.thingsboard.server.gen.edge.v1.CalculatedFieldUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.cf.BaseCalculatedFieldProcessor;

import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class CalculatedFieldCloudProcessor extends BaseCalculatedFieldProcessor {

    public ListenableFuture<Void> processCalculatedFieldMsgFromCloud(TenantId tenantId, CalculatedFieldUpdateMsg calculatedFieldUpdateMsg) {
        CalculatedFieldId calculatedFieldId = new CalculatedFieldId(new UUID(calculatedFieldUpdateMsg.getIdMSB(), calculatedFieldUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);
            return switch (calculatedFieldUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    saveOrUpdateCalculatedFieldFromCloud(tenantId, calculatedFieldId, calculatedFieldUpdateMsg);
                    yield Futures.immediateFuture(null);
                }
                case ENTITY_DELETED_RPC_MESSAGE -> {
                    CalculatedField calculatedField = edgeCtx.getCalculatedFieldService().findById(tenantId, calculatedFieldId);
                    if (calculatedField != null) {
                        edgeCtx.getCalculatedFieldService().deleteCalculatedField(tenantId, calculatedFieldId);
                        pushCalculatedFieldEventToRuleEngine(tenantId, calculatedField, TbMsgType.ENTITY_DELETED);
                    }
                    yield Futures.immediateFuture(null);
                }
                default -> handleUnsupportedMsgType(calculatedFieldUpdateMsg.getMsgType());
            };
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    private void saveOrUpdateCalculatedFieldFromCloud(TenantId tenantId, CalculatedFieldId calculatedFieldId, CalculatedFieldUpdateMsg calculatedFieldUpdateMsg) {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateCalculatedField(tenantId, calculatedFieldId, calculatedFieldUpdateMsg);
        Boolean wasCreated = resultPair.getFirst();
        if (wasCreated) {
            CalculatedField calculatedField = edgeCtx.getCalculatedFieldService().findById(tenantId, calculatedFieldId);
            pushCalculatedFieldEventToRuleEngine(tenantId, calculatedField, TbMsgType.ENTITY_CREATED);
        }
        Boolean calculatedFieldNameWasUpdated = resultPair.getSecond();
        if (calculatedFieldNameWasUpdated) {
            cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.CALCULATED_FIELD, EdgeEventActionType.UPDATED, calculatedFieldId, null);
        }
    }

    private void pushCalculatedFieldEventToRuleEngine(TenantId tenantId, CalculatedField calculatedField, TbMsgType msgType) {
        try {
            String calculatedFieldAsString = JacksonUtil.toString(calculatedField);
            pushEntityEventToRuleEngine(tenantId, calculatedField.getId(), null, msgType, calculatedFieldAsString, new TbMsgMetaData());
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push calculatedField action to rule engine: {}", tenantId, calculatedField.getId(), msgType.name(), e);
        }
    }

    @Override
    public UplinkMsg convertCloudEventToUplink(CloudEvent cloudEvent) {
        CalculatedFieldId calculatedFieldId = new CalculatedFieldId(cloudEvent.getEntityId());
        switch (cloudEvent.getAction()) {
            case ADDED, UPDATED -> {
                CalculatedField calculatedField = edgeCtx.getCalculatedFieldService().findById(cloudEvent.getTenantId(), calculatedFieldId);
                if (calculatedField != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    CalculatedFieldUpdateMsg calculatedFieldUpdateMsg = EdgeMsgConstructorUtils.constructCalculatedFieldUpdatedMsg(msgType, calculatedField);
                    UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addCalculatedFieldUpdateMsg(calculatedFieldUpdateMsg);

                    return builder.build();
                } else {
                    log.info("Skipping event as calculatedField was not found [{}]", cloudEvent);
                }
            }
            case DELETED -> {
                CalculatedFieldUpdateMsg calculatedFieldUpdateMsg = EdgeMsgConstructorUtils.constructCalculatedFieldDeleteMsg(calculatedFieldId);
                return UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addCalculatedFieldUpdateMsg(calculatedFieldUpdateMsg).build();
            }
        }
        return null;
    }

    public UplinkMsg convertCalculatedFieldRequestEventToUplink(CloudEvent cloudEvent) {
        EntityId entityId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getType(), cloudEvent.getEntityId());
        CalculatedFieldRequestMsg calculatedFieldRequestMsg = CalculatedFieldRequestMsg.newBuilder()
                .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                .setEntityType(entityId.getEntityType().name())
                .build();
        UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                .addCalculatedFieldRequestMsg(calculatedFieldRequestMsg);
        return builder.build();
    }

    @Override
    public CloudEventType getCloudEventType() {
        return CloudEventType.CALCULATED_FIELD;
    }

}
