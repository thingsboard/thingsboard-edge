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
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.gen.edge.v1.AiModelUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.ai.BaseAiModelProcessor;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class AiModelCloudProcessor extends BaseAiModelProcessor {

    public ListenableFuture<Void> processAiModelMsgFromCloud(TenantId tenantId, AiModelUpdateMsg aiModelUpdateMsg) {
        AiModelId aiModelId = new AiModelId(new UUID(aiModelUpdateMsg.getIdMSB(), aiModelUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);
            return switch (aiModelUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    saveOrUpdateAiModelFromCloud(tenantId, aiModelId, aiModelUpdateMsg);
                    yield Futures.immediateFuture(null);
                }
                case ENTITY_DELETED_RPC_MESSAGE -> {
                    deleteAiModel(tenantId, null, aiModelId);
                    yield Futures.immediateFuture(null);
                }
                default -> handleUnsupportedMsgType(aiModelUpdateMsg.getMsgType());
            };
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    private void saveOrUpdateAiModelFromCloud(TenantId tenantId, AiModelId aiModelId, AiModelUpdateMsg aiModelUpdateMsg) {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateAiModel(tenantId, aiModelId, aiModelUpdateMsg);
        Boolean create = resultPair.getFirst();
        if (create) {
            Optional<AiModel> aiModel = edgeCtx.getAiModelService().findAiModelById(tenantId, aiModelId);
            aiModel.ifPresent(model -> pushEntityEventToRuleEngine(tenantId, null, model, TbMsgType.ENTITY_CREATED));
        }
        Boolean aiModelNameUpdated = resultPair.getSecond();
        if (aiModelNameUpdated) {
            cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.AI_MODEL, EdgeEventActionType.UPDATED, aiModelId, null);
        }
    }

    @Override
    public UplinkMsg convertCloudEventToUplink(CloudEvent cloudEvent) {
        AiModelId aiModelId = new AiModelId(cloudEvent.getEntityId());
        switch (cloudEvent.getAction()) {
            case ADDED, UPDATED -> {
                AiModel aiModel = edgeCtx.getAiModelService().findAiModelById(cloudEvent.getTenantId(), aiModelId).orElse(null);
                if (aiModel != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    AiModelUpdateMsg aiModelUpdateMsg = EdgeMsgConstructorUtils.constructAiModelUpdatedMsg(msgType, aiModel);
                    return UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addAiModelUpdateMsg(aiModelUpdateMsg)
                            .build();
                } else {
                    log.info("Skipping event as AiModel was not found [{}]", cloudEvent);
                }
            }
            case DELETED -> {
                AiModelUpdateMsg aiModelUpdateMsg = EdgeMsgConstructorUtils.constructAiModelDeleteMsg(aiModelId);
                return UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addAiModelUpdateMsg(aiModelUpdateMsg)
                        .build();
            }
        }
        return null;
    }

    @Override
    public CloudEventType getCloudEventType() {
        return CloudEventType.AI_MODEL;
    }

}
